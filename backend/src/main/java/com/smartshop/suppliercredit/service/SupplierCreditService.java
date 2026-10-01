package com.smartshop.suppliercredit.service;

import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.common.mapping.TextNormalizer;
import com.smartshop.common.payment.PaymentRequest;
import com.smartshop.config.BusinessCalendar;
import com.smartshop.common.payment.RescheduleRequest;
import com.smartshop.purchase.entity.Purchase;
import com.smartshop.security.CurrentUserProvider;
import com.smartshop.suppliercredit.dto.SupplierBalanceResponse;
import com.smartshop.suppliercredit.dto.SupplierCreditFilter;
import com.smartshop.suppliercredit.dto.SupplierCreditResponse;
import com.smartshop.suppliercredit.mapper.SupplierCreditMapper;
import com.smartshop.suppliercredit.repository.SupplierCreditRepository;
import com.smartshop.suppliercredit.repository.SupplierCreditSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Credits fournisseurs : achats non soldes a leur creation, et leurs reglements ulterieurs.
 */
@Service
@RequiredArgsConstructor
public class SupplierCreditService {

    private final SupplierCreditRepository creditRepository;
    private final SupplierCreditMapper creditMapper;
    private final CurrentUserProvider currentUserProvider;
    private final BusinessCalendar calendar;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<SupplierCreditResponse> search(SupplierCreditFilter filter, Pageable pageable) {
        var today = calendar.today();
        return PageResponse.from(creditRepository.findAll(SupplierCreditSpecifications.matching(filter, today), pageable)
                .map(purchase -> creditMapper.toResponse(purchase, today)));
    }

    @Transactional(readOnly = true)
    public SupplierCreditResponse findById(Long purchaseId) {
        return creditMapper.toResponse(requireCredit(creditRepository.findById(purchaseId).orElse(null), purchaseId),
                calendar.today());
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierBalanceResponse> balances(Long supplierId, Pageable pageable) {
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.from(creditRepository.findBalances(supplierId, calendar.today(), unsorted));
    }

    /** Enregistre un paiement verse au fournisseur, sous verrou de l'achat. */
    @Transactional
    public SupplierCreditResponse makePayment(Long purchaseId, PaymentRequest request) {
        Purchase purchase = requireCredit(creditRepository.findByIdForUpdate(purchaseId).orElse(null), purchaseId);
        purchase.makePayment(request.amount(), request.method(), TextNormalizer.trimToNull(request.reference()),
                currentUserProvider.currentUser(), clock.instant());
        return creditMapper.toResponse(purchase, calendar.today());
    }

    @Transactional
    public SupplierCreditResponse reschedule(Long purchaseId, RescheduleRequest request) {
        Purchase purchase = requireCredit(creditRepository.findByIdForUpdate(purchaseId).orElse(null), purchaseId);
        purchase.reschedule(request.dueDate());
        return creditMapper.toResponse(purchase, calendar.today());
    }

    private static Purchase requireCredit(Purchase purchase, Long purchaseId) {
        if (purchase == null || !purchase.isOnCredit()) {
            throw new ResourceNotFoundException("Credit fournisseur", purchaseId);
        }
        return purchase;
    }
}
