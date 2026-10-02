package com.smartshop.aftersales.service;

import com.smartshop.aftersales.dto.AfterSalesCancelRequest;
import com.smartshop.aftersales.dto.AfterSalesCreateRequest;
import com.smartshop.aftersales.dto.AfterSalesDeliverRequest;
import com.smartshop.aftersales.dto.AfterSalesFilter;
import com.smartshop.aftersales.dto.AfterSalesPartRequest;
import com.smartshop.aftersales.dto.AfterSalesResponse;
import com.smartshop.aftersales.dto.AfterSalesStatusRequest;
import com.smartshop.aftersales.dto.AfterSalesSummaryResponse;
import com.smartshop.aftersales.dto.AfterSalesWorkRequest;
import com.smartshop.aftersales.dto.TechnicianAssignRequest;
import com.smartshop.aftersales.dto.TechnicianResponse;
import com.smartshop.aftersales.entity.AfterSalesPart;
import com.smartshop.aftersales.entity.AfterSalesStatus;
import com.smartshop.aftersales.entity.AfterSalesTicket;
import com.smartshop.aftersales.mapper.AfterSalesMapper;
import com.smartshop.aftersales.repository.AfterSalesSpecifications;
import com.smartshop.aftersales.repository.AfterSalesTicketRepository;
import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.common.mapping.TextNormalizer;
import com.smartshop.common.payment.PaymentRequest;
import com.smartshop.config.BusinessCalendar;
import com.smartshop.customer.entity.Customer;
import com.smartshop.customer.service.CustomerService;
import com.smartshop.product.entity.Product;
import com.smartshop.product.repository.ProductRepository;
import com.smartshop.sale.entity.Sale;
import com.smartshop.sale.entity.SaleItem;
import com.smartshop.sale.repository.SaleRepository;
import com.smartshop.security.CurrentUserProvider;
import com.smartshop.stock.entity.ProductUnit;
import com.smartshop.stock.entity.StockMovement;
import com.smartshop.stock.repository.ProductUnitRepository;
import com.smartshop.stock.service.StockService;
import com.smartshop.user.entity.Role;
import com.smartshop.user.entity.User;
import com.smartshop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.Comparator;
import java.util.List;

/**
 * Service apres-vente : reception, travail technique (diagnostic, pieces, statuts), paiement et livraison.
 * Chaque operation d'ecriture verrouille le ticket ; les pieces passent par {@link StockService}
 * dans la meme transaction.
 */
@Service
@RequiredArgsConstructor
public class AfterSalesService {

    static final String PART_REMOVED_REASON = "piece retiree du ticket";

    private final AfterSalesTicketRepository ticketRepository;
    private final CustomerService customerService;
    private final ProductRepository productRepository;
    private final ProductUnitRepository unitRepository;
    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final StockService stockService;
    private final CurrentUserProvider currentUserProvider;
    private final AfterSalesMapper mapper;
    private final BusinessCalendar calendar;
    private final Clock clock;

    // ------------------------------------------------------------------ reception

    @Transactional
    public AfterSalesResponse create(AfterSalesCreateRequest request) {
        Customer customer = customerService.getActiveEntity(request.customerId());
        AfterSalesTicket.Device device = resolveDevice(request);
        Instant now = clock.instant();
        AfterSalesTicket ticket = new AfterSalesTicket(nextTicketNumber(), customer, device,
                request.reportedIssue().trim(), request.estimatedCost(), request.expectedDate(),
                TextNormalizer.trimToNull(request.notes()), calendar.dateOf(now), currentUserProvider.currentUser(), now);
        return mapper.toResponse(ticketRepository.save(ticket));
    }

    /**
     * Retrouve l'unite vendue par la boutique (IMEI, sinon numero de serie non ambigu), son produit
     * et la garantie de sa derniere vente non annulee.
     */
    private AfterSalesTicket.Device resolveDevice(AfterSalesCreateRequest request) {
        String imei = TextNormalizer.trimToNull(request.imei());
        String serial = TextNormalizer.upper(request.serialNumber());
        ProductUnit unit = findUnit(imei, serial);

        Product product = null;
        if (request.productId() != null) {
            product = productRepository.findById(request.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit", request.productId()));
        }
        if (unit != null) {
            if (product != null && !product.getId().equals(unit.getProduct().getId())) {
                throw new BusinessException("L'identifiant %s correspond au produit '%s'"
                        .formatted(unit.label(), unit.getProduct().getName()), HttpStatus.BAD_REQUEST);
            }
            product = unit.getProduct();
            imei = imei != null ? imei : unit.getImei();
            serial = serial != null ? serial : unit.getSerialNumber();
        }

        Sale sale = null;
        LocalDate warrantyUntil = null;
        if (unit != null) {
            List<SaleItem> items = saleRepository.findCompletedItemsByUnit(unit.getId());
            if (!items.isEmpty()) {
                SaleItem item = items.getFirst();
                sale = item.getSale();
                if (item.getWarrantyMonths() != null && item.getWarrantyMonths() > 0) {
                    warrantyUntil = calendar.dateOf(sale.getCreatedAt()).plusMonths(item.getWarrantyMonths());
                }
            }
        }
        return new AfterSalesTicket.Device(request.deviceDescription().trim(), product, unit, sale, imei, serial,
                warrantyUntil);
    }

    private ProductUnit findUnit(String imei, String serial) {
        if (imei != null) {
            return unitRepository.findByImei(imei).orElse(null);
        }
        if (serial != null) {
            List<ProductUnit> units = unitRepository.findBySerialNumber(serial);
            return units.size() == 1 ? units.getFirst() : null;
        }
        return null;
    }

    // ------------------------------------------------------------------ travail technique

    @Transactional
    public AfterSalesResponse updateWork(Long id, AfterSalesWorkRequest request) {
        AfterSalesTicket ticket = lockTicket(id);
        ticket.updateWork(TextNormalizer.trimToNull(request.diagnosis()),
                TextNormalizer.trimToNull(request.repairDescription()), request.laborCost(), request.estimatedCost(),
                request.expectedDate(), request.underWarranty(), TextNormalizer.trimToNull(request.notes()),
                calendar.dateOf(ticket.getReceivedAt()));
        return mapper.toResponse(ticket);
    }

    @Transactional
    public AfterSalesResponse assignTechnician(Long id, TechnicianAssignRequest request) {
        User technician = userRepository.findById(request.technicianId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", request.technicianId()));
        if (technician.getRole() != Role.TECHNICIAN || !technician.isEnabled()) {
            throw new BusinessException("%s n'est pas un technicien actif".formatted(technician.getUsername()),
                    HttpStatus.BAD_REQUEST);
        }
        AfterSalesTicket ticket = lockTicket(id);
        ticket.assignTechnician(technician);
        return mapper.toResponse(ticket);
    }

    /** Un technicien qui commence le diagnostic d'un ticket non assigne le prend en charge. */
    @Transactional
    public AfterSalesResponse changeStatus(Long id, AfterSalesStatusRequest request) {
        AfterSalesTicket ticket = lockTicket(id);
        User user = currentUserProvider.currentUser();
        ticket.changeStatus(request.status(), TextNormalizer.trimToNull(request.comment()), user, clock.instant());
        if (request.status() == AfterSalesStatus.DIAGNOSING && ticket.getTechnician() == null
                && user.getRole() == Role.TECHNICIAN) {
            ticket.assignTechnician(user);
        }
        return flushAndMap(ticket);
    }

    @Transactional
    public AfterSalesResponse addPart(Long id, AfterSalesPartRequest request) {
        AfterSalesTicket ticket = lockTicket(id);
        // Statut verifie avant de toucher au stock
        ticket.requireInWorkshop();
        StockMovement movement = stockService.recordRepairPart(request.productId(), request.quantity(),
                request.unitIdsOrEmpty(), ticket.getTicketNumber());
        ticket.addPart(movement.getProduct(), request.quantity(), movement.getUnits(),
                currentUserProvider.currentUser(), clock.instant());
        return flushAndMap(ticket);
    }

    @Transactional
    public AfterSalesResponse removePart(Long id, Long partId) {
        AfterSalesTicket ticket = lockTicket(id);
        AfterSalesPart part = ticket.removePart(partId);
        returnToStock(ticket, part, PART_REMOVED_REASON);
        return flushAndMap(ticket);
    }

    // ------------------------------------------------------------------ paiement, livraison, annulation

    @Transactional
    public AfterSalesResponse receivePayment(Long id, PaymentRequest request) {
        AfterSalesTicket ticket = lockTicket(id);
        ticket.receivePayment(request.amount(), request.method(), TextNormalizer.trimToNull(request.reference()),
                currentUserProvider.currentUser(), clock.instant());
        return flushAndMap(ticket);
    }

    /** Encaisse le reglement eventuel et remet l'appareil : tout ou rien. */
    @Transactional
    public AfterSalesResponse deliver(Long id, AfterSalesDeliverRequest request) {
        AfterSalesTicket ticket = lockTicket(id);
        User user = currentUserProvider.currentUser();
        Instant now = clock.instant();
        for (PaymentRequest payment : request.paymentsOrEmpty()) {
            ticket.receivePayment(payment.amount(), payment.method(), TextNormalizer.trimToNull(payment.reference()),
                    user, now);
        }
        ticket.deliver(TextNormalizer.trimToNull(request.comment()), user, now);
        return flushAndMap(ticket);
    }

    /** Annule le ticket et remet en stock toutes ses pieces (ordre stable de verrouillage des produits). */
    @Transactional
    public AfterSalesResponse cancel(Long id, AfterSalesCancelRequest request) {
        AfterSalesTicket ticket = lockTicket(id);
        String reason = request.reason().trim();
        ticket.cancel(reason, currentUserProvider.currentUser(), clock.instant());
        ticket.getParts().stream()
                .sorted(Comparator.comparing((AfterSalesPart part) -> part.getProduct().getId()))
                .forEach(part -> returnToStock(ticket, part, "annulation du ticket - " + reason));
        return flushAndMap(ticket);
    }

    // ------------------------------------------------------------------ lecture

    @Transactional(readOnly = true)
    public PageResponse<AfterSalesSummaryResponse> search(AfterSalesFilter filter, Pageable pageable) {
        return PageResponse.from(ticketRepository.findAll(AfterSalesSpecifications.matching(filter), pageable)
                .map(mapper::toSummary));
    }

    @Transactional(readOnly = true)
    public AfterSalesResponse findById(Long id) {
        return ticketRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket SAV", id));
    }

    @Transactional(readOnly = true)
    public List<TechnicianResponse> technicians() {
        return userRepository.findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(Role.TECHNICIAN).stream()
                .map(mapper::toTechnician)
                .toList();
    }

    // ------------------------------------------------------------------ outils

    private void returnToStock(AfterSalesTicket ticket, AfterSalesPart part, String reason) {
        stockService.recordRepairPartReturn(part.getProduct().getId(), part.getQuantity(),
                part.getUnits().stream().map(ProductUnit::getId).toList(), ticket.getTicketNumber(), reason);
    }

    private AfterSalesTicket lockTicket(Long id) {
        return ticketRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket SAV", id));
    }

    /** Les elements ajoutes (pieces, paiements, historique) n'ont un id qu'apres insertion. */
    private AfterSalesResponse flushAndMap(AfterSalesTicket ticket) {
        ticketRepository.flush();
        return mapper.toResponse(ticket);
    }

    private String nextTicketNumber() {
        return "SAV-%d-%06d".formatted(Year.now(clock).getValue(), ticketRepository.nextTicketNumber());
    }
}
