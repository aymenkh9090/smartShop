---
name: backend
description: Regles pour coder dans backend/ (Spring Boot 4, Java 21) - nouvelle feature, controller, service, DTO, mapper, exception. A lire avant toute modification Java.
---

# Backend SmartShop

## Structure
- Package racine `com.smartshop`, une feature = un package : `controller/ service/ repository/ entity/ dto/ mapper/`.
- Transverse : `common/` (exceptions, `ApiError`), `config/`, `security/`.
- Verifier qu'une classe equivalente n'existe pas deja avant d'en creer une.

## Regles
- Controllers minces : validation (`@Valid`), appel du service, code HTTP. Aucune logique metier.
- Services : logique metier, `@Transactional` sur toute operation qui ecrit (lecture : `@Transactional(readOnly = true)`).
- Jamais d'entite JPA dans l'API : `XxxCreateRequest`, `XxxUpdateRequest`, `XxxResponse` (records Java), mapping MapStruct.
- Montants : `BigDecimal` (colonne `numeric(12,3)` - le dinar a 3 decimales). Jamais `double`.
- Erreurs : lever `ResourceNotFoundException` / `BusinessException` (ou une sous-classe) ; `GlobalExceptionHandler` produit le JSON. Jamais de try/catch qui avale une erreur.
- Mouvements de stock, paiements, statuts SAV : toujours historises, jamais une simple mise a jour de champ.
- **Stock : `Product.changeQuantity` n'est appele que par `StockService`** (verrou `findByIdForUpdate`, mouvement trace). Ventes, achats, SAV doivent passer par `StockService` dans leur propre transaction, jamais modifier la quantite eux-memes. Produits `serialTracked` : IMEI facultatif, invariant unites `IN_STOCK` <= quantite (articles anonymes = la difference).
- Methodes internes appelees par une autre operation metier : `@Transactional(propagation = MANDATORY)` (ex. `StockService.recordSale`) pour garantir qu'elles s'executent dans la transaction de l'appelant.
- Agregats (Sale, Purchase) : les montants sont calcules et verifies par l'entite (`complete()`), jamais saisis par le client ; montants via `Money` (3 decimales, HALF_UP).
- Tester l'atomicite avec un echec sur une ligne traitee APRES une ligne reussie (sinon le rollback n'est pas reellement teste).
- Credits saisis a la main : `ManualCredit` / `ManualCreditPayment` (`common/credit`, `@MappedSuperclass`) ; listes, soldes et dashboard passent par la vue `XxxCreditEntry` (`@Subselect` union ventes/achats + credits saisis, `@Id` = reference). Filtre statut commun : `CreditStatusSpecifications`.
- Credits : jamais de colonne "reste du" modifiable ; toujours recalcule depuis les paiements dans l'agregat (`receivePayment` / `makePayment`, sous verrou). Statut `CreditStatus.of(...)` en Java ET en Specification SQL : garder les deux coherents (test parametre par statut).
- Date du jour metier (echeances) : `BusinessCalendar.today()` (fuseau Africa/Tunis), jamais `LocalDate.now()`.
- Verifier un test de concurrence en retirant temporairement le verrou : il doit echouer.
- Plusieurs verrous dans une transaction : verrouiller dans un ordre stable (ex. par id croissant) pour eviter les interblocages.
- Auteur d'une operation : `CurrentUserProvider.currentUser()` (claim `uid` du JWT).
- Config : `application.yml` + profils ; tout secret via `${VARIABLE}` sans valeur par defaut.
- Spring Boot 4 : starters modulaires (`spring-boot-starter-webmvc`, ...), Jackson 3 (`tools.jackson.*`).
- **Jackson 3 : jamais de primitif (`boolean`, `int`) dans un Request DTO.** Un champ absent fait echouer toute la desérialisation (400 "corps illisible"). Utiliser `Boolean`/`Integer` + `@NotNull`.
- `@Pattern` s'applique AVANT le trim du mapper : tolerer les espaces autour (`^\\s*...\\s*$`).
- Spring Data 4 : `PropertyReferenceException` est dans `org.springframework.data.core`.

## Patterns etablis (phase 3)
- Mappers : `@Mapper(config = MapStructConfig.class)` (componentModel spring, champ cible oublie = erreur de compilation). `TextNormalizer` trim automatiquement toutes les chaines ; `qualifiedByName = "upper" | "lower" | "phone"` pour le reste.
- Unicite : `existsByX` (creation) / `existsByXAndIdNot` (modification) -> `BusinessException` (409). La contrainte SQL reste le filet de securite.
- **Piege auto-flush** : en modification, faire les controles (requetes `existsBy...`) AVANT de modifier l'entite managee (mapper `update`, setters). Sinon Hibernate flushe la modification avant la requete et la contrainte SQL repond a la place du controle metier. Normaliser les valeurs a controler avec `TextNormalizer`.
- Listes : `XxxFilter` record (query params) + `XxxSpecifications.matching(filter)` + `SearchPatterns` (echappe `%`/`_`) + `PageResponse.from(page)` ; `@EntityGraph` sur `findAll(spec, pageable)` si relation affichee.
- Pas de suppression des entites referencees par l'historique : `PATCH /{id}/status` avec `StatusUpdateRequest`.
- `POST` -> `201` + header `Location`. Tests d'integration : etendre `AbstractIntegrationTest` (`tokenFor(Role)`, `getAs/postAs/...`).

## Patterns etablis (phase 7)
- Workflow a etats : transitions declarees dans l'enum (`AfterSalesStatus.nextWorkflowStatuses()`), verifiees par l'agregat ; chaque transition ajoute une ligne d'historique immuable. Exposer `nextStatuses` dans la reponse pour le frontend.
- Ordre de verrouillage : agregat (ticket) d'abord, puis produits par id croissant. Personne ne verrouille un produit puis un ticket.
- Verifier le statut de l'agregat AVANT d'appeler `StockService` (pas de mouvement inutile en cas de refus).
- Elements ajoutes a un agregat existant (pieces, paiements) : `repository.flush()` avant le mapping, sinon leur id est null dans la reponse.
- Date locale d'un instant : `BusinessCalendar.dateOf(instant)`.

## Patterns etablis (phase 8)
- Statistiques : repository dedie (`DashboardRepository`, `EntityManager`) avec des requetes d'agregation ; periodes `[from, to[` calculees par `BusinessCalendar.startOf(date)` ; regroupement par jour local en SQL natif (`created_at at time zone :zone`).
- Contenu selon le role dans le service : `RoleHierarchy.getReachableGrantedAuthorities` (ne pas reimplementer la hierarchie) ; section non autorisee = null, jamais calculee.
- Tests de dates relatives au jour courant : antidater avec `calendar.startOf(jour).plusSeconds(3600)` (jamais d'arithmetique d'intervalle SQL en UTC).

## Commandes
- `./mvnw test` (Docker requis pour Testcontainers), `./mvnw verify` (+ rapport JaCoCo `target/site/jacoco`).
