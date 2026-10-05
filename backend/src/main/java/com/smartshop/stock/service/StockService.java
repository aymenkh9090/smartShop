package com.smartshop.stock.service;

import com.smartshop.common.dto.PageResponse;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.common.mapping.TextNormalizer;
import com.smartshop.product.entity.Product;
import com.smartshop.product.repository.ProductRepository;
import com.smartshop.security.CurrentUserProvider;
import com.smartshop.stock.dto.InventoryRequest;
import com.smartshop.stock.dto.InventoryResponse;
import com.smartshop.stock.dto.ProductUnitFilter;
import com.smartshop.stock.dto.ProductUnitRequest;
import com.smartshop.stock.dto.ProductUnitResponse;
import com.smartshop.stock.dto.StockMovementFilter;
import com.smartshop.stock.dto.StockMovementRequest;
import com.smartshop.stock.dto.StockMovementResponse;
import com.smartshop.stock.entity.ProductUnit;
import com.smartshop.stock.entity.ProductUnitStatus;
import com.smartshop.stock.entity.StockMovement;
import com.smartshop.stock.entity.StockMovementType;
import com.smartshop.stock.mapper.StockMapper;
import com.smartshop.stock.repository.ProductUnitRepository;
import com.smartshop.stock.repository.StockMovementRepository;
import com.smartshop.stock.repository.StockSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Seul point de modification de la quantite des produits. Chaque variation est tracee par un
 * {@link StockMovement} et s'execute sous verrou pessimiste du produit.
 */
@Service
@RequiredArgsConstructor
public class StockService {

    static final String DEFAULT_INVENTORY_REASON = "Inventaire";
    static final String SALE_REASON = "Vente";
    static final String PURCHASE_REASON = "Achat";
    static final String AFTER_SALES_REASON = "Pièce SAV";
    private static final int REASON_MAX_LENGTH = 255;

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final ProductUnitRepository unitRepository;
    private final CurrentUserProvider currentUserProvider;
    private final StockMapper stockMapper;

    @Transactional
    public StockMovementResponse recordMovement(StockMovementRequest request) {
        Product product = lockProduct(request.productId());
        StockMovement movement = apply(product, request.type(), request.quantity(),
                TextNormalizer.trimToNull(request.reason()), TextNormalizer.trimToNull(request.reference()),
                request.unitsOrEmpty(), request.unitIdsOrEmpty(), ProductUnitStatus.REMOVED);
        return stockMapper.toResponse(movement);
    }

    // ------------------------------------------------------------------ appels internes (ventes, achats, SAV)
    // Propagation MANDATORY : ces methodes s'executent dans la transaction de l'operation metier appelante,
    // qui doit traiter ses produits par id croissant (ordre de verrouillage stable).

    /** Sortie de stock d'une vente ; les unites designees passent a SOLD. */
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement recordSale(Long productId, int quantity, List<Long> unitIds, String saleReference) {
        Product product = lockProduct(productId);
        if (!product.isActive()) {
            throw new BusinessException("Produit désactivé : vente impossible (%s)".formatted(product.getName()));
        }
        return apply(product, StockMovementType.STOCK_OUT, quantity, SALE_REASON, saleReference,
                List.of(), unitIds, ProductUnitStatus.SOLD);
    }

    /** Remise en stock lors de l'annulation d'une vente ; les unites vendues repassent en stock. */
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement recordSaleCancellation(Long productId, int quantity, List<Long> unitIds,
                                                String saleReference, String cancelReason) {
        return apply(lockProduct(productId), StockMovementType.RETURN, quantity,
                truncateReason("Annulation vente : " + cancelReason),
                saleReference, List.of(), unitIds, ProductUnitStatus.SOLD);
    }

    /** Sortie d'une piece utilisee pour une reparation SAV ; les unites designees passent a SOLD. */
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement recordRepairPart(Long productId, int quantity, List<Long> unitIds, String ticketNumber) {
        Product product = lockProduct(productId);
        if (!product.isActive()) {
            throw new BusinessException("Produit désactivé : utilisation impossible (%s)".formatted(product.getName()));
        }
        return apply(product, StockMovementType.STOCK_OUT, quantity, AFTER_SALES_REASON, ticketNumber,
                List.of(), unitIds, ProductUnitStatus.SOLD);
    }

    /** Remise en stock d'une piece SAV retiree du ticket ou d'un ticket annule. */
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement recordRepairPartReturn(Long productId, int quantity, List<Long> unitIds,
                                                String ticketNumber, String reason) {
        return apply(lockProduct(productId), StockMovementType.RETURN, quantity,
                truncateReason("Retour pièce SAV : " + reason), ticketNumber, List.of(), unitIds,
                ProductUnitStatus.SOLD);
    }

    /** Entree de stock d'un achat ; cree les unites des produits suivis. */
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement recordPurchase(Long productId, int quantity, List<ProductUnitRequest> units,
                                        String purchaseReference) {
        return apply(lockProduct(productId), StockMovementType.STOCK_IN, quantity, PURCHASE_REASON,
                purchaseReference, units, List.of(), ProductUnitStatus.REMOVED);
    }

    /**
     * Applique un comptage physique : un ajustement par produit dont la quantite differe.
     * Tout ou rien : une ligne invalide annule l'inventaire complet.
     */
    @Transactional
    public InventoryResponse applyInventory(InventoryRequest request) {
        Set<Long> productIds = new HashSet<>();
        for (InventoryRequest.Line line : request.lines()) {
            if (!productIds.add(line.productId())) {
                throw badRequest("Produit présent plusieurs fois dans l'inventaire : " + line.productId());
            }
        }
        String reason = Objects.requireNonNullElse(TextNormalizer.trimToNull(request.reason()), DEFAULT_INVENTORY_REASON);
        String reference = TextNormalizer.trimToNull(request.reference());

        // Verrouillage dans un ordre stable pour eviter les interblocages entre inventaires concurrents
        List<InventoryRequest.Line> lines = request.lines().stream()
                .sorted(Comparator.comparing(InventoryRequest.Line::productId))
                .toList();
        List<StockMovement> movements = new ArrayList<>();
        for (InventoryRequest.Line line : lines) {
            Product product = lockProduct(line.productId());
            if (product.isSerialTracked()) {
                long identified = unitRepository.countByProductIdAndStatus(product.getId(), ProductUnitStatus.IN_STOCK);
                if (line.countedQuantity() < identified) {
                    throw badRequest(("'%s' : %d unité(s) avec IMEI enregistrée(s) en stock, comptage %d. "
                            + "Sortez d'abord les unités manquantes par leur IMEI.")
                            .formatted(product.getName(), identified, line.countedQuantity()));
                }
            }
            int difference = line.countedQuantity() - product.getQuantity();
            if (difference != 0) {
                movements.add(apply(product, StockMovementType.ADJUSTMENT, difference, reason, reference,
                        List.of(), List.of(), ProductUnitStatus.REMOVED));
            }
        }
        return new InventoryResponse(lines.size(), movements.size(),
                movements.stream().map(stockMapper::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> searchMovements(StockMovementFilter filter, Pageable pageable) {
        return PageResponse.from(movementRepository.findAll(StockSpecifications.movements(filter), pageable)
                .map(stockMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public StockMovementResponse findMovement(Long id) {
        return movementRepository.findWithDetailsById(id)
                .map(stockMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Mouvement de stock", id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductUnitResponse> searchUnits(ProductUnitFilter filter, Pageable pageable) {
        return PageResponse.from(unitRepository.findAll(StockSpecifications.units(filter), pageable)
                .map(stockMapper::toUnitResponse));
    }

    // ------------------------------------------------------------------ regles metier

    /**
     * @param outgoingStatus statut donne aux unites qui sortent du stock (REMOVED, ou SOLD pour une vente)
     */
    private StockMovement apply(Product product, StockMovementType type, int quantity, String reason,
                                String reference, List<ProductUnitRequest> newUnits, List<Long> unitIds,
                                ProductUnitStatus outgoingStatus) {
        int delta = signedDelta(type, quantity);
        if (type != StockMovementType.STOCK_IN && reason == null) {
            throw badRequest("Le motif est obligatoire pour ce type de mouvement");
        }
        if (type == StockMovementType.STOCK_IN && !product.isActive()) {
            throw new BusinessException("Produit désactivé : entrée en stock impossible");
        }
        List<ProductUnit> units = product.isSerialTracked()
                ? resolveUnits(product, type, delta, newUnits, unitIds, outgoingStatus)
                : requireNoUnits(product, newUnits, unitIds);
        product.changeQuantity(delta);
        return movementRepository.save(new StockMovement(product, type, delta, reason, reference,
                currentUserProvider.currentUser(), units));
    }

    private static int signedDelta(StockMovementType type, int quantity) {
        return switch (type) {
            case STOCK_IN, RETURN -> requirePositive(quantity);
            case STOCK_OUT -> -requirePositive(quantity);
            case ADJUSTMENT -> {
                if (quantity == 0) {
                    throw badRequest("Un ajustement doit modifier la quantité");
                }
                yield quantity;
            }
        };
    }

    private static int requirePositive(int quantity) {
        if (quantity <= 0) {
            throw badRequest("La quantité doit être strictement positive");
        }
        return quantity;
    }

    private static List<ProductUnit> requireNoUnits(Product product, List<ProductUnitRequest> newUnits,
                                                    List<Long> unitIds) {
        if (!newUnits.isEmpty() || !unitIds.isEmpty()) {
            throw badRequest("'%s' n'est pas suivi à l'unité : ne pas fournir d'unités".formatted(product.getName()));
        }
        return List.of();
    }

    /**
     * Produits suivis a l'unite : l'IMEI / numero de serie est facultatif. Le stock peut donc contenir
     * des unites identifiees et des articles anonymes ; invariant : unites IN_STOCK <= quantite.
     * Une entree cree les unites fournies ; une sortie ou un retour designe eventuellement des unites existantes.
     */
    private List<ProductUnit> resolveUnits(Product product, StockMovementType type, int delta,
                                           List<ProductUnitRequest> newUnits, List<Long> unitIds,
                                           ProductUnitStatus outgoingStatus) {
        boolean createsUnits = delta > 0 && type != StockMovementType.RETURN;
        if (createsUnits) {
            if (!unitIds.isEmpty()) {
                throw badRequest("Une entrée crée de nouvelles unités : fournir 'units', pas 'unitIds'");
            }
            return createUnits(product, newUnits, delta);
        }
        if (!newUnits.isEmpty()) {
            throw badRequest("Désigner les unités existantes concernées avec 'unitIds'");
        }
        // Sortie : unites en stock -> outgoingStatus. Retour : unites sorties ou vendues -> remises en stock.
        boolean returning = delta > 0;
        int count = Math.abs(delta);
        List<ProductUnit> units = changeUnitsStatus(product, unitIds, count,
                returning ? ProductUnitStatus.IN_STOCK : outgoingStatus);
        if (!returning && product.getQuantity() >= count) {
            ensureEnoughAnonymousItems(product, count, units.size());
        }
        return units;
    }

    /**
     * Les articles sortis sans IMEI sont pris parmi les articles anonymes. S'il n'en reste pas assez,
     * l'IMEI doit etre designe : sinon des unites resteraient "en stock" alors qu'elles sont sorties.
     */
    private void ensureEnoughAnonymousItems(Product product, int count, int designated) {
        int anonymousOut = count - designated;
        if (anonymousOut == 0) {
            return;
        }
        // Les unites designees ont deja quitte IN_STOCK (flush automatique avant la requete)
        long identifiedInStock = unitRepository.countByProductIdAndStatus(product.getId(), ProductUnitStatus.IN_STOCK);
        long anonymousAvailable = product.getQuantity() - designated - identifiedInStock;
        if (anonymousOut > anonymousAvailable) {
            throw badRequest(("'%s' : %d article(s) sans IMEI en stock, %d à sortir sans IMEI. "
                    + "Sélectionnez l'IMEI / numéro de série des unités concernées.")
                    .formatted(product.getName(), Math.max(0, anonymousAvailable), anonymousOut));
        }
    }

    /** Unites fournies a l'entree (facultatives) : au plus une par article, lignes vides ignorees. */
    private List<ProductUnit> createUnits(Product product, List<ProductUnitRequest> requests, int maximum) {
        Set<String> imeis = new LinkedHashSet<>();
        Set<String> serials = new LinkedHashSet<>();
        List<ProductUnit> units = new ArrayList<>();
        for (ProductUnitRequest request : requests) {
            String imei = TextNormalizer.trimToNull(request.imei());
            String serial = TextNormalizer.upper(request.serialNumber());
            if (imei == null && serial == null) {
                continue;
            }
            if (imei != null && !imeis.add(imei)) {
                throw badRequest("IMEI en double dans la saisie : " + imei);
            }
            if (serial != null && !serials.add(serial)) {
                throw badRequest("Numéro de série en double dans la saisie : " + serial);
            }
            units.add(new ProductUnit(product, imei, serial));
        }
        if (units.size() > maximum) {
            throw badRequest("%d unité(s) fournie(s) pour %d article(s)".formatted(units.size(), maximum));
        }
        if (!imeis.isEmpty()) {
            List<String> existing = unitRepository.findExistingImeis(imeis);
            if (!existing.isEmpty()) {
                throw new BusinessException("IMEI déjà enregistré(s) : " + String.join(", ", existing));
            }
        }
        if (!serials.isEmpty()) {
            List<String> existing = unitRepository.findExistingSerialNumbers(product.getId(), serials);
            if (!existing.isEmpty()) {
                throw new BusinessException("Numéro(s) de série déjà enregistré(s) : " + String.join(", ", existing));
            }
        }
        return unitRepository.saveAll(units);
    }

    private List<ProductUnit> changeUnitsStatus(Product product, List<Long> unitIds, int expected,
                                                ProductUnitStatus newStatus) {
        Set<Long> ids = new LinkedHashSet<>(unitIds);
        if (ids.size() != unitIds.size()) {
            throw badRequest("Unité sélectionnée plusieurs fois");
        }
        if (ids.size() > expected) {
            throw badRequest("%d unité(s) sélectionnée(s) pour %d article(s)".formatted(ids.size(), expected));
        }
        List<ProductUnit> units = unitRepository.findAllById(ids);
        if (units.size() != ids.size()) {
            throw new ResourceNotFoundException("Unité(s) introuvable(s) parmi : " + ids);
        }
        boolean mustBeInStock = newStatus != ProductUnitStatus.IN_STOCK;
        for (ProductUnit unit : units) {
            if (!unit.getProduct().getId().equals(product.getId())) {
                throw badRequest("L'unité %s n'appartient pas à '%s'".formatted(unit.label(), product.getName()));
            }
            if (unit.isInStock() != mustBeInStock) {
                throw new BusinessException(mustBeInStock
                        ? "L'unité %s n'est pas en stock".formatted(unit.label())
                        : "L'unité %s est déjà en stock".formatted(unit.label()));
            }
            unit.changeStatus(newStatus);
        }
        return units;
    }

    private Product lockProduct(Long productId) {
        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", productId));
    }

    private static String truncateReason(String reason) {
        return reason.length() > REASON_MAX_LENGTH ? reason.substring(0, REASON_MAX_LENGTH) : reason;
    }

    private static BusinessException badRequest(String message) {
        return new BusinessException(message, HttpStatus.BAD_REQUEST);
    }
}
