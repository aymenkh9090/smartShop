---
name: frontend
description: Regles pour coder dans frontend/ (Angular 20 standalone) - pages, composants, services HTTP, guards, interceptors, formulaires.
---

# Frontend SmartShop

## Structure (`src/app/`)
- `core/` : singletons (services HTTP, guards, interceptors, modeles transverses comme `page`, `payment`). Utilisable partout.
- `shared/` ne depend jamais de `features/`. Une feature peut utiliser les services/pickers d'une autre feature (ex. `ProductPicker`).
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
- Apercus de montants : `shared/utils/money.ts` (calculs en millimes) ; le backend reste la reference.
- Saisie de paiements : `PaymentsEditor` + `createPaymentRow` ; motif obligatoire : `askReason(dialog, ...)` ; paiement ulterieur : `askPayment(...)` ; echeance : `askNewDueDate(...)`.
- Dates "jour" (echeances) : `toIsoDate` / `fromIsoDate` (jamais `new Date('yyyy-MM-dd')`, interprete en UTC).
- Tests : creer le composant DANS `fakeAsync` quand un `debounceTime` est teste ; appeler `fixture.detectChanges()` apres une action qui met a jour un signal lu par `toObservable`.
- Dans `*matCellDef`, la variable est `any` : passer par une methode typee pour indexer un `Record`.

## Patterns etablis (phase 7)
- Page detail avec actions : signaux `computed` par action (role + statut), et une methode `run(action, message)` qui gere `busy`, la mise a jour du signal et les notifications.
- Boutons de workflow generes depuis `nextStatuses` renvoye par le backend (pas de duplication des transitions cote Angular).
- Spec d'un composant qui embarque un picker : fournir aussi `ProductService` / `CustomerService` mockes.

## Commandes
- `npm start`, `npm run build`, `npm run test:ci`.
