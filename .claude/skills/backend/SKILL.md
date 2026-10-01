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
- Config : `application.yml` + profils ; tout secret via `${VARIABLE}` sans valeur par defaut.
- Spring Boot 4 : starters modulaires (`spring-boot-starter-webmvc`, ...), Jackson 3 (`tools.jackson.*`).

## Commandes
- `./mvnw test` (Docker requis pour Testcontainers), `./mvnw verify` (+ rapport JaCoCo `target/site/jacoco`).
