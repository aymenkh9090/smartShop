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
- Montants affiches avec le pipe `currency` / `number`, jamais d'arrondi manuel (locale `fr`, devise `TND` : `currency: 'TND' : 'symbol' : '1.3-3'`).
- UI : Angular Material 20 (pas de `@angular/animations`, ne pas importer `NoopAnimationsModule`). Classes globales dans `styles.scss` : `page-header`, `filters`, `table-container`, `form-card`, `form-grid`, `form-actions`, `badge`.

## Patterns etablis (phase 3)
- Feature = `<f>.models.ts`, `<f>.service.ts`, `<f>.routes.ts` (lazy, `data: { roles }` sur new/edit), `<f>-list/`, `<f>-form/`.
- Liste : signal `query` (filtre + page + tri) -> `toObservable` + `switchMap` -> `toSignal` ; filtres debounces 300 ms qui remettent la page a 0 ; recharger = `query.update(q => ({ ...q }))`.
- Formulaire : route `:id/edit` lue par `input<string>()` (withComponentInputBinding) ; `fieldErrorMessage` dans `<mat-error>` ; `applyServerErrors` + `NotificationService.error` en cas d'echec.
- Params HTTP : `toHttpParams` (ignore null/''). Confirmation : `confirmAction(dialog, {...})`.
- Dans `*matCellDef`, la variable est `any` : passer par une methode typee pour indexer un `Record`.

## Commandes
- `npm start`, `npm run build`, `npm run test:ci`.
