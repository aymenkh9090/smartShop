package com.smartshop.customercredit.service;

import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.common.mapping.TextNormalizer;
import com.smartshop.common.payment.PaymentRequest;
import com.smartshop.config.BusinessCalendar;
import com.smartshop.customercredit.dto.CustomerBalanceResponse;
import com.smartshop.customercredit.dto.CustomerCreditFilter;
import com.smartshop.customercredit.dto.CustomerCreditResponse;
import com.smartshop.common.payment.RescheduleRequest;
import com.smartshop.customercredit.mapper.CustomerCreditMapper;
import com.smartshop.customercredit.repository.CustomerCreditRepository;
import com.smartshop.customercredit.repository.CustomerCreditSpecifications;
import com.smartshop.sale.entity.Sale;
import com.smartshop.sale.repository.SaleRepository;
import com.smartshop.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Credits clients : ventes non soldees a leur creation, et leurs reglements ulterieurs.
 */
@Service
@RequiredArgsConstructor
public class CustomerCreditService {

    private final CustomerCreditRepository creditRepository;
    private final SaleRepository saleRepository;
    private final CustomerCreditMapper creditMapper;
    private final CurrentUserProvider currentUserProvider;
    private final BusinessCalendar calendar;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<CustomerCreditResponse> search(CustomerCreditFilter filter, Pageable pageable) {
        var today = calendar.today();
        return PageResponse.from(creditRepository.findAll(CustomerCreditSpecifications.matching(filter, today), pageable)
                .map(sale -> creditMapper.toResponse(sale, today)));
    }

    @Transactional(readOnly = true)
    public CustomerCreditResponse findById(Long saleId) {
        return creditMapper.toResponse(requireCredit(creditRepository.findById(saleId).orElse(null), saleId),
                calendar.today());
    }

    /** Solde de chaque client debiteur (ou d'un seul client avec customerId). */
    @Transactional(readOnly = true)
    public PageResponse<CustomerBalanceResponse> balances(Long customerId, Pageable pageable) {
        // Le tri est fixe par la requete (encours decroissant)
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.from(creditRepository.findBalances(customerId, calendar.today(), unsorted));
    }

    /** Encaisse un paiement sur un credit client, sous verrou de la vente. */
    @Transactional
    public CustomerCreditResponse receivePayment(Long saleId, PaymentRequest request) {
        Sale sale = requireCredit(saleRepository.findByIdForUpdate(saleId).orElse(null), saleId);
        sale.receivePayment(request.amount(), request.method(), TextNormalizer.trimToNull(request.reference()),
                currentUserProvider.currentUser(), clock.instant());
        return creditMapper.toResponse(sale, calendar.today());
    }

    @Transactional
    public CustomerCreditResponse reschedule(Long saleId, RescheduleRequest request) {
        Sale sale = requireCredit(saleRepository.findByIdForUpdate(saleId).orElse(null), saleId);
        sale.reschedule(request.dueDate());
        return creditMapper.toResponse(sale, calendar.today());
    }

    private static Sale requireCredit(Sale sale, Long saleId) {
        if (sale == null || !sale.isOnCredit()) {
            throw new ResourceNotFoundException("Credit client", saleId);
        }
        return sale;
    }
}
