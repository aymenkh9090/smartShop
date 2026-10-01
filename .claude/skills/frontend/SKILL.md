---
name: frontend
description: Regles pour coder dans frontend/ (Angular 20 standalone) - pages, composants, services HTTP, guards, interceptors, formulaires.
---

# Frontend SmartShop

## Structure (`src/app/`)
- `core/` : singletons (services HTTP, guards, interceptors, modeles). Jamais importe par `shared/`.
- `shared/` : composants/pipes/directives reutilisables, sans dependance a une feature.
- `features/<feature>/` : pages d'une fonctionnalite + `<feature>.routes.ts` charge en lazy loading.

## Regles
- Composants standalone, `ChangeDetectionStrategy.OnPush`, `inject()` plutot que l'injection par constructeur.
- Etat local : signals (`signal`, `computed`) ; flux HTTP : RxJS / `toSignal`.
- Formulaires : Reactive Forms types (`FormBuilder.nonNullable`), validations alignees sur le backend.
- URL de l'API : uniquement via `environment.apiUrl`. Jamais d'URL en dur.
- Erreurs HTTP : typer avec `ApiError` (`core/models/api-error.ts`).
- Template : control flow natif (`@if`, `@for` avec `track`).
- Les guards ne sont qu'un confort UX : l'autorisation reelle est cote backend.
- Montants affiches avec le pipe `currency` / `number`, jamais d'arrondi manuel.

## Commandes
- `npm start`, `npm run build`, `npm run test:ci`.
