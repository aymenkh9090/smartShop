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
- **Stock : `Product.changeQuantity` n'est appele que par `StockService`** (verrou `findByIdForUpdate`, mouvement trace). Ventes, achats, SAV doivent passer par `StockService` dans leur propre transaction, jamais modifier la quantite eux-memes. Produits `serialTracked` : quantite = nombre d'unites `IN_STOCK`.
- Methodes internes appelees par une autre operation metier : `@Transactional(propagation = MANDATORY)` (ex. `StockService.recordSale`) pour garantir qu'elles s'executent dans la transaction de l'appelant.
- Agregats (Sale, Purchase) : les montants sont calcules et verifies par l'entite (`complete()`), jamais saisis par le client ; montants via `Money` (3 decimales, HALF_UP).
- Tester l'atomicite avec un echec sur une ligne traitee APRES une ligne reussie (sinon le rollback n'est pas reellement teste).
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

## Commandes
- `./mvnw test` (Docker requis pour Testcontainers), `./mvnw verify` (+ rapport JaCoCo `target/site/jacoco`).
