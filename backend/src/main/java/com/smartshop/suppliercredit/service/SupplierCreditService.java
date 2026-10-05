package com.smartshop.suppliercredit.service;

import com.smartshop.common.credit.CreditCancelRequest;
import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.common.mapping.TextNormalizer;
import com.smartshop.common.payment.CreditPaymentRequest;
import com.smartshop.common.payment.RescheduleRequest;
import com.smartshop.config.BusinessCalendar;
import com.smartshop.purchase.entity.Purchase;
import com.smartshop.purchase.repository.PurchaseRepository;
import com.smartshop.security.CurrentUserProvider;
import com.smartshop.supplier.entity.Supplier;
import com.smartshop.supplier.service.SupplierService;
import com.smartshop.suppliercredit.dto.SupplierBalanceResponse;
import com.smartshop.suppliercredit.dto.SupplierCreditFilter;
import com.smartshop.suppliercredit.dto.SupplierCreditRequest;
import com.smartshop.suppliercredit.dto.SupplierCreditResponse;
import com.smartshop.suppliercredit.dto.SupplierCreditSummaryResponse;
import com.smartshop.suppliercredit.entity.SupplierCredit;
import com.smartshop.suppliercredit.mapper.SupplierCreditMapper;
import com.smartshop.suppliercredit.repository.SupplierCreditEntryRepository;
import com.smartshop.suppliercredit.repository.SupplierCreditRepository;
import com.smartshop.suppliercredit.repository.SupplierCreditSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.Objects;

/**
 * Credits fournisseurs : achats non soldes a leur creation et credits saisis a la main, avec leurs reglements.
 */
@Service
@RequiredArgsConstructor
public class SupplierCreditService {

    private final SupplierCreditEntryRepository entryRepository;
    private final SupplierCreditRepository creditRepository;
    private final PurchaseRepository purchaseRepository;
    private final SupplierService supplierService;
    private final SupplierCreditMapper creditMapper;
    private final CurrentUserProvider currentUserProvider;
    private final BusinessCalendar calendar;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<SupplierCreditSummaryResponse> search(SupplierCreditFilter filter, Pageable pageable) {
        var today = calendar.today();
        return PageResponse.from(entryRepository.findAll(SupplierCreditSpecifications.matching(filter, today), pageable)
                .map(entry -> creditMapper.toSummary(entry, today)));
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierBalanceResponse> balances(Long supplierId, Pageable pageable) {
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.from(entryRepository.findBalances(supplierId, calendar.today(), unsorted));
    }

    // ------------------------------------------------------------------ credits issus d'un achat

    @Transactional(readOnly = true)
    public SupplierCreditResponse findById(Long purchaseId) {
        return creditMapper.toResponse(requirePurchaseCredit(purchaseRepository.findById(purchaseId).orElse(null),
                purchaseId), calendar.today());
    }

    /** Enregistre un paiement verse au fournisseur, sous verrou de l'achat. */
    @Transactional
    public SupplierCreditResponse makePayment(Long purchaseId, CreditPaymentRequest request) {
        Purchase purchase = requirePurchaseCredit(purchaseRepository.findByIdForUpdate(purchaseId).orElse(null),
                purchaseId);
        Instant paidAt = calendar.momentOf(request.paidOn());
        if (paidAt.isBefore(purchase.getCreatedAt())) {
            throw new BusinessException("La date du paiement précède la date de l'achat", HttpStatus.BAD_REQUEST);
        }
        purchase.makePayment(request.amount(), request.methodOrDefault(), TextNormalizer.trimToNull(request.reference()),
                currentUserProvider.currentUser(), paidAt);
        return creditMapper.toResponse(purchase, calendar.today());
    }

    @Transactional
    public SupplierCreditResponse reschedule(Long purchaseId, RescheduleRequest request) {
        Purchase purchase = requirePurchaseCredit(purchaseRepository.findByIdForUpdate(purchaseId).orElse(null),
                purchaseId);
        purchase.reschedule(request.dueDate());
        return creditMapper.toResponse(purchase, calendar.today());
    }

    // ------------------------------------------------------------------ credits saisis a la main

    @Transactional
    public SupplierCreditResponse create(SupplierCreditRequest request) {
        Supplier supplier = supplierService.getActiveEntity(request.supplierId());
        LocalDate creditDate = Objects.requireNonNullElse(request.creditDate(), calendar.today());
        if (request.dueDate() != null && request.dueDate().isBefore(creditDate)) {
            throw new BusinessException("L'échéance précède la date du crédit", HttpStatus.BAD_REQUEST);
        }
        SupplierCredit credit = new SupplierCredit(nextReference(), supplier, request.amount(),
                calendar.momentOf(creditDate), request.dueDate(), TextNormalizer.trimToNull(request.description()),
                currentUserProvider.currentUser());
        return creditMapper.toResponse(creditRepository.save(credit), calendar.today());
    }

    @Transactional(readOnly = true)
    public SupplierCreditResponse findManual(Long id) {
        return creditMapper.toResponse(creditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crédit fournisseur", id)), calendar.today());
    }

    /** Enregistre une tranche versee au fournisseur ; le reste du est recalcule. */
    @Transactional
    public SupplierCreditResponse payManual(Long id, CreditPaymentRequest request) {
        SupplierCredit credit = lockManual(id);
        credit.pay(request.amount(), request.methodOrDefault(), TextNormalizer.trimToNull(request.reference()),
                calendar.momentOf(request.paidOn()), clock.instant(), currentUserProvider.currentUser());
        creditRepository.flush();
        return creditMapper.toResponse(credit, calendar.today());
    }

    @Transactional
    public SupplierCreditResponse rescheduleManual(Long id, RescheduleRequest request) {
        SupplierCredit credit = lockManual(id);
        credit.reschedule(request.dueDate());
        return creditMapper.toResponse(credit, calendar.today());
    }

    @Transactional
    public SupplierCreditResponse cancelManual(Long id, CreditCancelRequest request) {
        SupplierCredit credit = lockManual(id);
        credit.cancel(currentUserProvider.currentUser(), clock.instant(), request.reason().trim());
        return creditMapper.toResponse(credit, calendar.today());
    }

    private SupplierCredit lockManual(Long id) {
        return creditRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crédit fournisseur", id));
    }

    private String nextReference() {
        return "CF-%d-%06d".formatted(Year.now(clock).getValue(), creditRepository.nextReferenceNumber());
    }

    private static Purchase requirePurchaseCredit(Purchase purchase, Long purchaseId) {
        if (purchase == null || !purchase.isOnCredit()) {
            throw new ResourceNotFoundException("Crédit fournisseur", purchaseId);
        }
        return purchase;
    }
}
