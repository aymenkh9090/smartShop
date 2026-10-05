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
- UI : Angular Material 20 (pas de `@angular/animations`, ne pas importer `NoopAnimationsModule`). Classes globales dans `styles.scss` : `page-header` (+ `page-title` / `page-subtitle` / `back-link`), `filters`, `table-container`, `form-card`, `form-section` (titre + aide a gauche, champs a droite), `form-grid`, `form-actions`, `badge`, `detail-table`.
- **Textes affiches en francais correctement accentue** (libelles, messages, tooltips, `aria-label`). Le code et les commentaires restent en ASCII.
- Barre laterale sombre dans les deux themes : jetons `--sidebar-*` (`styles.scss`).
- Libelles / descriptions des roles : `ROLE_LABELS`, `ROLE_DESCRIPTIONS` (`core/models/auth.models.ts`), ne pas les dupliquer.
- Design system (`styles.scss`) : theme M3 indigo (`_theme-colors.scss`, genere par `ng g @angular/material:theme-color`), police Inter, icones Material Symbols (`MAT_ICON_DEFAULT_OPTIONS`, classe `filled` pour l'icone pleine).
- Mode clair / sombre : `ThemeService` (clair / sombre / systeme, memorise) pilote `color-scheme` sur `<html>`. **Jamais de couleur en dur dans un composant** : utiliser les jetons `--app-*` (surface, border, text-muted...) et `--status-*` (success / warning / danger / info / neutral), definis avec `light-dark()`. Un canvas (Chart.js) lit `ThemeService.isDark()`.
- Pastilles : `badge` + `success` (actif, paye, pret), `warn` (a regler), `danger` (en retard, stock faible), `muted` (clos, inactif) ; sans modificateur = info. Le libelle porte toujours le sens.
- Coquille : menu groupe dans `app.ts` (`NAV_GROUPS`, filtre par role, groupes vides masques), menu escamotable sous 960px. Ajouter une page = l'ajouter dans un groupe.

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

## Patterns etablis (phase 8)
- Graphiques : Chart.js via ng2-charts 9 (Angular 20 ; ng2-charts >= 10 exige Angular 21+). Enregistrer uniquement les elements utilises (`provideCharts({ registerables: [...] })`) dans le composant graphique, pas globalement.
- Lire le skill `dataviz` avant tout graphique : serie unique = slot 1 `#2a78d6`, colonnes <= 24px, extremite arrondie 4px, grille discrete, infobulle au survol, tableau des valeurs (`<details>`) pour l'accessibilite. Valider les couleurs avec son script.
- Grilles responsives : `minmax(min(220px, 100%), 1fr)` pour ne jamais deborder en largeur etroite.
- Sections pilotees par le backend (null = non autorise) : le template affiche simplement les sections presentes, sans dupliquer la matrice des roles.

## Commandes
- `npm start`, `npm run build`, `npm run test:ci`.
