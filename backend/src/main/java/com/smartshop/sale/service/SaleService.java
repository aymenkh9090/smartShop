package com.smartshop.sale.service;

import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.common.mapping.TextNormalizer;
import com.smartshop.common.payment.PaymentRequest;
import com.smartshop.customer.entity.Customer;
import com.smartshop.customer.service.CustomerService;
import com.smartshop.sale.dto.SaleCancelRequest;
import com.smartshop.sale.dto.SaleFilter;
import com.smartshop.sale.dto.SaleRequest;
import com.smartshop.sale.dto.SaleResponse;
import com.smartshop.sale.dto.SaleSummaryResponse;
import com.smartshop.sale.entity.Sale;
import com.smartshop.sale.entity.SaleItem;
import com.smartshop.sale.mapper.SaleMapper;
import com.smartshop.sale.repository.SaleRepository;
import com.smartshop.sale.repository.SaleSpecifications;
import com.smartshop.security.CurrentUserProvider;
import com.smartshop.stock.entity.ProductUnit;
import com.smartshop.stock.entity.StockMovement;
import com.smartshop.stock.service.StockService;
import com.smartshop.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final CustomerService customerService;
    private final StockService stockService;
    private final CurrentUserProvider currentUserProvider;
    private final SaleMapper saleMapper;
    private final Clock clock;

    /**
     * Enregistre une vente : sortie de stock de chaque ligne, paiements et totaux dans une seule transaction.
     * Toute incoherence (stock insuffisant, trop-percu, credit sans client...) annule l'ensemble.
     */
    @Transactional
    public SaleResponse create(SaleRequest request) {
        ensureDistinctProducts(request.items());
        Customer customer = request.customerId() == null ? null : customerService.getActiveEntity(request.customerId());
        User user = currentUserProvider.currentUser();
        Sale sale = new Sale(nextReference(), customer, user, TextNormalizer.trimToNull(request.notes()));

        // Ordre stable de verrouillage des produits (evite les interblocages entre ventes concurrentes)
        request.items().stream()
                .sorted(Comparator.comparing(SaleRequest.Item::productId))
                .forEach(item -> {
                    StockMovement movement = stockService.recordSale(item.productId(), item.quantity(),
                            item.unitIdsOrEmpty(), sale.getReference());
                    sale.addItem(movement.getProduct(), item.quantity(), movement.getUnits());
                });

        Instant now = clock.instant();
        for (PaymentRequest payment : request.paymentsOrEmpty()) {
            sale.addPayment(payment.amount(), payment.method(), TextNormalizer.trimToNull(payment.reference()), user, now);
        }
        sale.complete(request.discount(), request.dueDate());
        return saleMapper.toResponse(saleRepository.save(sale));
    }

    /** Annule une vente : le stock (et les IMEI) est restitue, la vente reste dans l'historique. */
    @Transactional
    public SaleResponse cancel(Long id, SaleCancelRequest request) {
        Sale sale = saleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vente", id));
        if (sale.isCancelled()) {
            throw new BusinessException("La vente %s est deja annulee".formatted(sale.getReference()));
        }
        String reason = request.reason().trim();
        sale.getItems().stream()
                .sorted(Comparator.comparing((SaleItem item) -> item.getProduct().getId()))
                .forEach(item -> stockService.recordSaleCancellation(item.getProduct().getId(), item.getQuantity(),
                        item.getUnits().stream().map(ProductUnit::getId).toList(), sale.getReference(), reason));
        sale.cancel(currentUserProvider.currentUser(), clock.instant(), reason);
        return saleMapper.toResponse(sale);
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleSummaryResponse> search(SaleFilter filter, Pageable pageable) {
        return PageResponse.from(saleRepository.findAll(SaleSpecifications.matching(filter), pageable)
                .map(saleMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public SaleResponse findById(Long id) {
        return saleRepository.findById(id)
                .map(saleMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Vente", id));
    }

    private String nextReference() {
        return "V-%d-%06d".formatted(Year.now(clock).getValue(), saleRepository.nextReferenceNumber());
    }

    private static void ensureDistinctProducts(List<SaleRequest.Item> items) {
        Set<Long> productIds = new HashSet<>();
        for (SaleRequest.Item item : items) {
            if (!productIds.add(item.productId())) {
                throw new BusinessException("Produit present sur plusieurs lignes : regroupez les quantites",
                        HttpStatus.BAD_REQUEST);
            }
        }
    }
}
