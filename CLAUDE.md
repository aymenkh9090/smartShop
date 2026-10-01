# SmartShop — Contexte et règles du projet

## 1. Présentation du projet

SmartShop est une application web de gestion destinée à une boutique spécialisée dans la vente de :

- Smartphones
- Récepteurs
- Abonnements IPTV
- Chargeurs
- Câbles
- Écouteurs
- Accessoires téléphoniques
- Montres connectées
- Autres produits électroniques

L'application doit permettre de centraliser la gestion du stock, les ventes, les clients, les fournisseurs, les crédits clients, les crédits fournisseurs et le service après-vente.

L'objectif est de construire une application professionnelle, maintenable, sécurisée et évolutive.

---

# 2. Stack technique

## Backend

- Java 21
- Spring Boot 4.1.x (Java 21) — décision Phase 1 : la branche 3.x n'a plus de support OSS
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- JWT (Spring Security OAuth2 Resource Server / Nimbus JOSE)
- PostgreSQL
- Bean Validation
- Lombok
- MapStruct
- Maven
- JUnit 5
- Mockito
- JaCoCo
- Docker

## Frontend

- Angular 20
- TypeScript
- Angular Router
- Angular HttpClient
- Reactive Forms
- RxJS
- Angular Material ou une bibliothèque UI moderne
- Guards
- Interceptors
- Services
- Signals lorsque pertinent

## Base de données

PostgreSQL.

Les migrations de base de données doivent être versionnées.

Décision : **Flyway** (`backend/src/main/resources/db/migration`), Hibernate en `ddl-auto: validate`.

Ne jamais modifier manuellement la base de données en production sans migration correspondante.

---

# 3. Architecture générale

Le projet est composé de deux applications principales :

```text
smartshop/
├── backend/
└── frontend/
```

Le backend expose une API REST.

Le frontend Angular consomme cette API.

Architecture logique :

```text
Angular 20
    |
    | HTTP / JSON
    v
Spring Boot REST API
    |
    +-- Security / JWT
    |
    +-- Controllers
    |
    +-- Services
    |
    +-- Repositories
    |
    +-- JPA / Hibernate
    |
    v
PostgreSQL
```

---

# 4. Architecture Backend

Le backend doit suivre une architecture propre et maintenable.

Organisation recommandée :

```text
com.smartshop
├── common
├── config
├── security
├── auth
├── user
├── product
├── category
├── stock
├── customer
├── supplier
├── sale
├── purchase
├── customercredit
├── suppliercredit
├── aftersales
└── dashboard
```

Chaque fonctionnalité doit idéalement être organisée avec :

```text
feature/
├── controller
├── service
├── repository
├── entity
├── dto
└── mapper
```

Éviter de mettre toute la logique métier dans les Controllers.

Les Controllers doivent rester minces.

La logique métier doit être dans les Services.

Les repositories doivent uniquement gérer l'accès aux données.

---

# 5. Sécurité

L'authentification doit utiliser :

- Spring Security
- JWT
- BCrypt pour le hash des mots de passe

Endpoints principaux :

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
GET  /api/auth/me
```

Les endpoints protégés doivent nécessiter un JWT valide.

Prévoir des rôles :

```text
ADMIN
MANAGER
SELLER
TECHNICIAN
```

Les permissions doivent être pensées avant d'implémenter les endpoints.

Ne jamais stocker un mot de passe en clair.

Ne jamais mettre de secret JWT directement dans le code source.

Les secrets doivent être configurables avec des variables d'environnement.

---

# 6. Gestion des produits

L'application doit permettre de gérer différents types de produits.

Exemples :

```text
SMARTPHONE
RECEIVER
IPTV_SUBSCRIPTION
CHARGER
CABLE
EARPHONE
SMARTWATCH
ACCESSORY
OTHER
```

Un produit doit pouvoir avoir au minimum :

- id
- référence
- code produit / SKU
- nom
- description
- catégorie
- prix d'achat
- prix de vente
- quantité
- seuil minimum
- marque
- modèle
- état
- date de création
- date de modification

Pour les smartphones et produits nécessitant un suivi individuel, prévoir la possibilité de gérer :

- IMEI
- numéro de série
- garantie

Ne pas forcer toutes les propriétés spécifiques dans la table principale si cela crée un modèle difficile à maintenir.

---

# 7. Gestion du stock

Le stock doit permettre :

- entrée de stock
- sortie de stock
- ajustement
- transfert si plusieurs magasins sont ajoutés ultérieurement
- inventaire
- historique des mouvements
- alertes de stock faible

Chaque mouvement doit être traçable.

Exemple :

```text
STOCK_IN
STOCK_OUT
ADJUSTMENT
RETURN
```

Un mouvement doit conserver notamment :

- produit
- quantité
- type
- date
- utilisateur
- motif
- référence éventuelle

Ne jamais modifier directement une quantité sans conserver une trace du mouvement lorsque l'opération correspond à une entrée/sortie métier.

---

# 8. Clients

Un client doit pouvoir avoir :

- nom
- prénom
- téléphone
- email
- adresse
- CIN si nécessaire
- notes
- solde

Le système doit permettre de consulter :

- historique des ventes
- paiements
- crédits
- remboursements
- service après-vente

---

# 9. Fournisseurs

Un fournisseur doit pouvoir avoir :

- raison sociale / nom
- téléphone
- email
- adresse
- matricule fiscal si nécessaire
- notes
- solde

Le système doit permettre de consulter :

- achats
- paiements
- crédits fournisseurs
- historique des opérations

---

# 10. Crédits clients

Le système doit gérer les ventes à crédit.

Exemple :

```text
Client
   |
   +-- Vente : 800 DT
   |
   +-- Paiement : 300 DT
   |
   +-- Reste : 500 DT
```

Chaque crédit doit être traçable.

Prévoir :

- montant total
- montant payé
- montant restant
- date
- échéance éventuelle
- statut

Statuts possibles :

```text
PENDING
PARTIALLY_PAID
PAID
OVERDUE
CANCELLED
```

Les paiements doivent être enregistrés séparément.

Ne pas simplement modifier le montant restant sans conserver l'historique des paiements.

---

# 11. Crédits fournisseurs

Même principe que les crédits clients, mais pour les achats auprès des fournisseurs.

Exemple :

```text
Achat fournisseur : 3000 DT
Paiement : 1000 DT
Reste : 2000 DT
```

Prévoir :

- achat
- montant total
- paiements
- reste
- échéance
- statut
- historique

---

# 12. Service après-vente

Le module SAV doit permettre d'enregistrer un appareil ou produit confié par un client.

Exemple :

```text
Client
  |
  v
Ticket SAV
  |
  +-- Produit
  +-- IMEI / numéro de série
  +-- Problème déclaré
  +-- Diagnostic
  +-- Technicien
  +-- Réparation
  +-- Pièces utilisées
  +-- Coût
  +-- Statut
```

Statuts possibles :

```text
RECEIVED
DIAGNOSING
WAITING_FOR_PART
IN_REPAIR
READY
DELIVERED
CANCELLED
```

Le système doit conserver l'historique des changements de statut.

Prévoir :

- numéro de ticket
- client
- produit
- IMEI / numéro de série
- problème déclaré
- diagnostic
- technicien
- date de réception
- date prévue
- date de livraison
- coût de réparation
- garantie
- statut
- notes

---

# 13. Ventes

Le module vente doit permettre :

- création d'une vente
- ajout de plusieurs produits
- calcul automatique du total
- remise
- paiement
- vente comptant
- vente à crédit
- mise à jour du stock
- historique

Une vente peut contenir plusieurs lignes.

Exemple :

```text
Sale
 ├── SaleItem Smartphone
 ├── SaleItem Charger
 └── SaleItem Earphone
```

Le stock doit être mis à jour de manière transactionnelle.

---

# 14. Achats

Prévoir un module achat permettant :

- fournisseur
- produits
- quantités
- prix d'achat
- total
- paiement
- crédit fournisseur
- mise à jour du stock

---

# 15. Transactions

Les opérations critiques doivent utiliser les transactions Spring :

```java
@Transactional
```

Exemples :

- validation d'une vente
- entrée de stock
- sortie de stock
- paiement d'un crédit
- création d'un achat

Une opération métier ne doit pas laisser la base dans un état incohérent.

---

# 16. DTO

Ne pas exposer directement les Entity JPA dans les API publiques.

Utiliser des DTO :

```text
Request DTO
Response DTO
```

Exemple :

```text
ProductCreateRequest
ProductUpdateRequest
ProductResponse
```

Utiliser MapStruct pour les mappings lorsqu'il est pertinent.

---

# 17. Validation

Utiliser Jakarta Bean Validation.

Exemples :

```java
@NotBlank
@NotNull
@Positive
@PositiveOrZero
@Email
@Size
```

Les erreurs doivent retourner une réponse JSON cohérente.

Prévoir un gestionnaire global :

```text
@RestControllerAdvice
```

Format recommandé :

```json
{
  "timestamp": "...",
  "status": 400,
  "message": "...",
  "path": "...",
  "errors": []
}
```

---

# 18. Gestion des erreurs

Créer des exceptions métier spécifiques.

Exemples :

```text
ResourceNotFoundException
BusinessException
InsufficientStockException
InvalidPaymentException
CreditNotFoundException
```

Ne jamais retourner des stack traces internes au frontend.

---

# 19. Base de données

Prévoir des contraintes :

- NOT NULL lorsque nécessaire
- UNIQUE pour les références uniques
- Foreign Keys
- Index sur les colonnes fréquemment recherchées

Exemples :

```text
product.sku
product.barcode
customer.phone
sale.reference
after_sales.ticket_number
```

Les montants monétaires doivent utiliser :

```java
BigDecimal
```

Ne jamais utiliser `double` pour les montants financiers.

---

# 20. API REST

Convention :

```text
/api/auth
/api/products
/api/categories
/api/stock
/api/customers
/api/suppliers
/api/sales
/api/purchases
/api/customer-credits
/api/supplier-credits
/api/after-sales
/api/dashboard
```

Utiliser les méthodes HTTP correctement :

```text
GET
POST
PUT
PATCH
DELETE
```

Utiliser des codes HTTP appropriés.

---

# 21. Frontend Angular

Angular 20 doit utiliser une architecture claire.

Organisation recommandée :

```text
src/app/
├── core/
│   ├── guards/
│   ├── interceptors/
│   ├── services/
│   └── models/
│
├── shared/
│   ├── components/
│   ├── pipes/
│   └── directives/
│
├── features/
│   ├── auth/
│   ├── dashboard/
│   ├── products/
│   ├── stock/
│   ├── customers/
│   ├── suppliers/
│   ├── sales/
│   ├── purchases/
│   ├── customer-credits/
│   ├── supplier-credits/
│   └── after-sales/
│
└── app.routes.ts
```

Utiliser :

- standalone components
- lazy loading
- reactive forms
- route guards
- HTTP interceptors
- services
- signals lorsque pertinent

---

# 22. Angular Security

Le JWT doit être automatiquement ajouté aux requêtes protégées avec un HttpInterceptor.

Prévoir :

```text
AuthService
AuthGuard
AuthInterceptor
```

Le frontend doit gérer :

- login
- logout
- session
- expiration JWT
- accès selon rôle

Ne jamais faire confiance uniquement aux protections Angular.

L'autorisation réelle doit toujours être contrôlée côté backend.

---

# 23. Dashboard

Prévoir progressivement un dashboard avec :

- chiffre d'affaires
- ventes du jour
- ventes du mois
- achats
- stock faible
- crédits clients
- crédits fournisseurs
- SAV en cours
- SAV prêts
- produits les plus vendus

Ne pas implémenter toutes les statistiques immédiatement.

Commencer par les indicateurs essentiels.

---

# 24. Tests

Le backend doit avoir des tests unitaires et d'intégration.

Priorité :

1. Services métier
2. Security
3. Repositories critiques
4. Controllers

Utiliser :

- JUnit 5
- Mockito
- Spring Boot Test

Tester notamment :

- login
- JWT
- création produit
- entrée/sortie stock
- vente
- calcul crédit
- paiement crédit
- SAV
- permissions

---

# 25. Git

Utiliser des commits propres.

Format recommandé :

```text
feat: add product management
feat: implement customer credit
fix: prevent negative stock
test: add sales service tests
refactor: improve stock service
docs: update README
```

Ne jamais commit :

```text
.env
secrets
passwords
JWT secrets
database credentials
```

---

# 26. Méthode de développement

Ne pas développer toute l'application en une seule étape.

Procéder par phases.

## Phase 1 — Initialisation

- création du repository
- backend Spring Boot
- frontend Angular
- PostgreSQL
- Docker
- configuration
- README
- Git

## Phase 2 — Security

- User
- Role
- Register
- Login
- JWT
- BCrypt
- Authorization

## Phase 3 — Référentiel

- catégories
- produits
- fournisseurs
- clients

## Phase 4 — Stock

- mouvements
- inventaire
- alertes
- historique

## Phase 5 — Achats et ventes

- achats
- ventes
- paiements
- stock

## Phase 6 — Crédits

- crédits clients
- paiements clients
- crédits fournisseurs
- paiements fournisseurs

## Phase 7 — SAV

- tickets
- diagnostic
- réparation
- pièces
- statuts
- historique

## Phase 8 — Dashboard

- statistiques
- indicateurs
- graphiques

## Phase 9 — Tests et qualité

- tests
- JaCoCo
- SonarCloud
- Docker
- documentation

---

# 27. Règle importante pour Claude Code

Avant de coder une fonctionnalité importante :

1. comprendre le besoin
2. inspecter le code existant
3. proposer une solution
4. vérifier les dépendances nécessaires
5. implémenter
6. écrire les tests
7. exécuter les tests
8. corriger les erreurs
9. mettre à jour la documentation

Ne pas réécrire inutilement du code fonctionnel.

Ne pas créer de fichiers ou classes en double.

Ne pas installer une dépendance sans vérifier qu'elle est réellement nécessaire.

Toujours privilégier une architecture simple et maintenable.

---

# 28. Principe de développement

Le projet doit être développé comme une vraie application professionnelle.

Priorités :

```text
Correctness
Security
Maintainability
Testability
Performance
User Experience
```

La simplicité doit être privilégiée lorsqu'une solution complexe n'apporte pas de valeur réelle.

---

# 29. Skills du projet

Avant une tâche, lire le skill correspondant dans `.claude/skills/` :
`backend`, `frontend`, `database`, `security`, `testing`.
