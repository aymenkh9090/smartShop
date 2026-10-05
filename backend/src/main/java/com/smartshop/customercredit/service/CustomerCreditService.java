package com.smartshop.customercredit.service;

import com.smartshop.common.credit.CreditCancelRequest;
import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.common.mapping.TextNormalizer;
import com.smartshop.common.payment.CreditPaymentRequest;
import com.smartshop.common.payment.RescheduleRequest;
import com.smartshop.config.BusinessCalendar;
import com.smartshop.customer.entity.Customer;
import com.smartshop.customer.service.CustomerService;
import com.smartshop.customercredit.dto.CustomerBalanceResponse;
import com.smartshop.customercredit.dto.CustomerCreditFilter;
import com.smartshop.customercredit.dto.CustomerCreditRequest;
import com.smartshop.customercredit.dto.CustomerCreditResponse;
import com.smartshop.customercredit.dto.CustomerCreditSummaryResponse;
import com.smartshop.customercredit.entity.CustomerCredit;
import com.smartshop.customercredit.mapper.CustomerCreditMapper;
import com.smartshop.customercredit.repository.CustomerCreditEntryRepository;
import com.smartshop.customercredit.repository.CustomerCreditRepository;
import com.smartshop.customercredit.repository.CustomerCreditSpecifications;
import com.smartshop.sale.entity.Sale;
import com.smartshop.sale.repository.SaleRepository;
import com.smartshop.security.CurrentUserProvider;
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
 * Credits clients : ventes non soldees a leur creation et credits saisis a la main, avec leurs reglements.
 */
@Service
@RequiredArgsConstructor
public class CustomerCreditService {

    private final CustomerCreditEntryRepository entryRepository;
    private final CustomerCreditRepository creditRepository;
    private final SaleRepository saleRepository;
    private final CustomerService customerService;
    private final CustomerCreditMapper creditMapper;
    private final CurrentUserProvider currentUserProvider;
    private final BusinessCalendar calendar;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<CustomerCreditSummaryResponse> search(CustomerCreditFilter filter, Pageable pageable) {
        var today = calendar.today();
        return PageResponse.from(entryRepository.findAll(CustomerCreditSpecifications.matching(filter, today), pageable)
                .map(entry -> creditMapper.toSummary(entry, today)));
    }

    /** Solde de chaque client debiteur (ou d'un seul client avec customerId). */
    @Transactional(readOnly = true)
    public PageResponse<CustomerBalanceResponse> balances(Long customerId, Pageable pageable) {
        // Le tri est fixe par la requete (encours decroissant)
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.from(entryRepository.findBalances(customerId, calendar.today(), unsorted));
    }

    // ------------------------------------------------------------------ credits issus d'une vente

    @Transactional(readOnly = true)
    public CustomerCreditResponse findById(Long saleId) {
        return creditMapper.toResponse(requireSaleCredit(saleRepository.findById(saleId).orElse(null), saleId),
                calendar.today());
    }

    /** Encaisse un paiement sur un credit client, sous verrou de la vente. */
    @Transactional
    public CustomerCreditResponse receivePayment(Long saleId, CreditPaymentRequest request) {
        Sale sale = requireSaleCredit(saleRepository.findByIdForUpdate(saleId).orElse(null), saleId);
        Instant paidAt = calendar.momentOf(request.paidOn());
        if (paidAt.isBefore(sale.getCreatedAt())) {
            throw new BusinessException("La date du paiement précède la date de la vente", HttpStatus.BAD_REQUEST);
        }
        sale.receivePayment(request.amount(), request.methodOrDefault(), TextNormalizer.trimToNull(request.reference()),
                currentUserProvider.currentUser(), paidAt);
        return creditMapper.toResponse(sale, calendar.today());
    }

    @Transactional
    public CustomerCreditResponse reschedule(Long saleId, RescheduleRequest request) {
        Sale sale = requireSaleCredit(saleRepository.findByIdForUpdate(saleId).orElse(null), saleId);
        sale.reschedule(request.dueDate());
        return creditMapper.toResponse(sale, calendar.today());
    }

    // ------------------------------------------------------------------ credits saisis a la main

    @Transactional
    public CustomerCreditResponse create(CustomerCreditRequest request) {
        Customer customer = customerService.getActiveEntity(request.customerId());
        LocalDate creditDate = Objects.requireNonNullElse(request.creditDate(), calendar.today());
        if (request.dueDate() != null && request.dueDate().isBefore(creditDate)) {
            throw new BusinessException("L'échéance précède la date du crédit", HttpStatus.BAD_REQUEST);
        }
        CustomerCredit credit = new CustomerCredit(nextReference(), customer, request.amount(),
                calendar.momentOf(creditDate), request.dueDate(), TextNormalizer.trimToNull(request.description()),
                currentUserProvider.currentUser());
        return creditMapper.toResponse(creditRepository.save(credit), calendar.today());
    }

    @Transactional(readOnly = true)
    public CustomerCreditResponse findManual(Long id) {
        return creditMapper.toResponse(creditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crédit client", id)), calendar.today());
    }

    /** Enregistre une tranche versee par le client ; le reste du est recalcule. */
    @Transactional
    public CustomerCreditResponse payManual(Long id, CreditPaymentRequest request) {
        CustomerCredit credit = lockManual(id);
        credit.pay(request.amount(), request.methodOrDefault(), TextNormalizer.trimToNull(request.reference()),
                calendar.momentOf(request.paidOn()), clock.instant(), currentUserProvider.currentUser());
        creditRepository.flush();
        return creditMapper.toResponse(credit, calendar.today());
    }

    @Transactional
    public CustomerCreditResponse rescheduleManual(Long id, RescheduleRequest request) {
        CustomerCredit credit = lockManual(id);
        credit.reschedule(request.dueDate());
        return creditMapper.toResponse(credit, calendar.today());
    }

    @Transactional
    public CustomerCreditResponse cancelManual(Long id, CreditCancelRequest request) {
        CustomerCredit credit = lockManual(id);
        credit.cancel(currentUserProvider.currentUser(), clock.instant(), request.reason().trim());
        return creditMapper.toResponse(credit, calendar.today());
    }

    private CustomerCredit lockManual(Long id) {
        return creditRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crédit client", id));
    }

    private String nextReference() {
        return "CC-%d-%06d".formatted(Year.now(clock).getValue(), creditRepository.nextReferenceNumber());
    }

    private static Sale requireSaleCredit(Sale sale, Long saleId) {
        if (sale == null || !sale.isOnCredit()) {
            throw new ResourceNotFoundException("Crédit client", saleId);
        }
        return sale;
    }
}
