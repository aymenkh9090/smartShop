package com.smartshop.purchase.service;

import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.common.mapping.TextNormalizer;
import com.smartshop.common.payment.Money;
import com.smartshop.common.payment.PaymentRequest;
import com.smartshop.product.entity.Product;
import com.smartshop.purchase.dto.PurchaseFilter;
import com.smartshop.purchase.dto.PurchaseRequest;
import com.smartshop.purchase.dto.PurchaseResponse;
import com.smartshop.purchase.dto.PurchaseSummaryResponse;
import com.smartshop.purchase.entity.Purchase;
import com.smartshop.purchase.mapper.PurchaseMapper;
import com.smartshop.purchase.repository.PurchaseRepository;
import com.smartshop.purchase.repository.PurchaseSpecifications;
import com.smartshop.security.CurrentUserProvider;
import com.smartshop.stock.entity.StockMovement;
import com.smartshop.stock.service.StockService;
import com.smartshop.supplier.entity.Supplier;
import com.smartshop.supplier.service.SupplierService;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierService supplierService;
    private final StockService stockService;
    private final CurrentUserProvider currentUserProvider;
    private final PurchaseMapper purchaseMapper;
    private final Clock clock;

    /**
     * Enregistre un achat recu : entree en stock de chaque ligne (creation des IMEI), mise a jour du
     * dernier prix d'achat des produits, paiements et totaux, dans une seule transaction.
     */
    @Transactional
    public PurchaseResponse create(PurchaseRequest request) {
        Set<Long> productIds = new HashSet<>();
        for (PurchaseRequest.Item item : request.items()) {
            if (!productIds.add(item.productId())) {
                throw new BusinessException("Produit present sur plusieurs lignes : regroupez les quantites",
                        HttpStatus.BAD_REQUEST);
            }
        }
        Supplier supplier = supplierService.getActiveEntity(request.supplierId());
        User user = currentUserProvider.currentUser();
        Purchase purchase = new Purchase(nextReference(), supplier,
                TextNormalizer.trimToNull(request.supplierInvoiceRef()), user, TextNormalizer.trimToNull(request.notes()));

        request.items().stream()
                .sorted(Comparator.comparing(PurchaseRequest.Item::productId))
                .forEach(item -> {
                    StockMovement movement = stockService.recordPurchase(item.productId(), item.quantity(),
                            item.unitsOrEmpty(), purchase.getReference());
                    Product product = movement.getProduct();
                    // Dernier prix d'achat connu, utilise pour le calcul des marges
                    product.setPurchasePrice(Money.of(item.unitCost()));
                    purchase.addItem(product, item.quantity(), item.unitCost(), movement.getUnits());
                });

        Instant now = clock.instant();
        for (PaymentRequest payment : request.paymentsOrEmpty()) {
            purchase.addPayment(payment.amount(), payment.method(), TextNormalizer.trimToNull(payment.reference()),
                    user, now);
        }
        purchase.complete(request.dueDate());
        return purchaseMapper.toResponse(purchaseRepository.save(purchase));
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseSummaryResponse> search(PurchaseFilter filter, Pageable pageable) {
        return PageResponse.from(purchaseRepository.findAll(PurchaseSpecifications.matching(filter), pageable)
                .map(purchaseMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public PurchaseResponse findById(Long id) {
        return purchaseRepository.findById(id)
                .map(purchaseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Achat", id));
    }

    private String nextReference() {
        return "A-%d-%06d".formatted(Year.now(clock).getValue(), purchaseRepository.nextReferenceNumber());
    }
}
