---
name: database
description: Regles pour le schema PostgreSQL et les migrations Flyway (creation/modification de tables, index, contraintes, entites JPA).
---

# Base de donnees SmartShop

- Schema gere **uniquement** par Flyway : `backend/src/main/resources/db/migration/V<n>__<description>.sql`.
- Hibernate en `ddl-auto: validate` : chaque entite doit correspondre exactement a une migration.
- Ne jamais modifier une migration deja appliquee/commitee : en ajouter une nouvelle.
- Nommage : tables et colonnes en `snake_case`, tables au singulier (`product`, `sale_item`).
- Cles primaires `bigint generated always as identity` (`@GeneratedValue(strategy = IDENTITY)`).
- Montants : `numeric(12,3)` ; quantites : `integer` avec `check (quantity >= 0)` si pertinent.
- Dates : `timestamptz` (`Instant` ou `OffsetDateTime` cote Java).
- Toujours : `not null` quand obligatoire, `unique` sur references metier (sku, barcode, reference, ticket_number), FK explicites, index sur les colonnes de recherche/jointure.
- Enums stockes en `varchar` + `@Enumerated(EnumType.STRING)` + contrainte `check`.
- Relations JPA en `FetchType.LAZY` ; eviter le N+1 (fetch join / `@EntityGraph`).
- Verifier une migration : `./mvnw test` (Testcontainers applique toutes les migrations sur PostgreSQL reel).
