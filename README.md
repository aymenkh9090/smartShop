# SmartShop

Application web de gestion pour une boutique de smartphones, récepteurs, abonnements IPTV, chargeurs, câbles, écouteurs, montres connectées et accessoires.

Elle centralise : stock, ventes, achats, clients, fournisseurs, crédits clients et fournisseurs, service après-vente (SAV) et tableau de bord.

> État actuel : **Phase 6 – crédits** terminée (crédits clients et fournisseurs, paiements ultérieurs, échéances, soldes). Voir `CLAUDE.md` pour la feuille de route.

## Architecture

```text
smartshop/
├── backend/                 API REST Spring Boot
│   └── src/main/java/com/smartshop/
│       ├── common/          ApiError, exceptions, GlobalExceptionHandler
│       ├── config/          configuration transverse (CORS...)
│       ├── security/        SecurityConfig, JWT, gestion 401/403
│       └── <feature>/       controller/ service/ repository/ entity/ dto/ mapper/
├── frontend/                Application Angular
│   └── src/app/
│       ├── core/            services, guards, interceptors, modèles
│       ├── shared/          composants, pipes, directives réutilisables
│       └── features/        une fonctionnalité par dossier (lazy loading)
├── .claude/skills/          règles de travail pour Claude Code
├── docker-compose.yml       PostgreSQL de développement
├── .env.example             modèle des variables d'environnement
└── CLAUDE.md                contexte et règles du projet
```

```text
Angular 20  --HTTP/JSON-->  Spring Boot REST API (JWT)  -->  JPA/Hibernate  -->  PostgreSQL (Flyway)
```

## Technologies

| Couche | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Web MVC, Spring Data JPA / Hibernate, Spring Security, JWT (OAuth2 Resource Server / Nimbus), Bean Validation, Lombok, Flyway, Maven |
| Tests backend | JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL), JaCoCo |
| Frontend | Angular 20 (standalone), Angular Material 20, TypeScript, Router, HttpClient, Reactive Forms, RxJS, Signals |
| Base de données | PostgreSQL 17 |
| Infra | Docker, Docker Compose |

## Prérequis

- Java 21
- Docker + Docker Compose (requis aussi pour les tests backend, via Testcontainers)
- Node.js 20.19+ et npm
- Angular CLI 20 (facultatif : `npx ng` fonctionne aussi)
- Maven n'est pas requis : utiliser le wrapper `./mvnw`

## Variables d'environnement

Copier le modèle, puis renseigner des valeurs réelles. Le fichier `.env` n'est **jamais** versionné.

```bash
cp .env.example .env
openssl rand -hex 16      # -> DB_PASSWORD
openssl rand -base64 48   # -> JWT_SECRET
```

| Variable | Défaut | Description |
|---|---|---|
| `DB_HOST` | `localhost` | Hôte PostgreSQL |
| `DB_PORT` | `5432` | Port PostgreSQL |
| `DB_NAME` | `smartshop` | Nom de la base |
| `DB_USERNAME` | `smartshop` | Utilisateur |
| `DB_PASSWORD` | **obligatoire** | Mot de passe |
| `JWT_SECRET` | **obligatoire** | Secret de signature JWT (≥ 32 caractères) |
| `JWT_ACCESS_TOKEN_TTL` | `15m` | Durée de vie du token d'accès |
| `JWT_REFRESH_TOKEN_TTL` | `7d` | Durée de vie du refresh token |
| `SERVER_PORT` | `8080` | Port de l'API |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` (dev) | Origines autorisées, séparées par des virgules |
| `ADMIN_USERNAME` | `admin` | Compte administrateur initial |
| `ADMIN_EMAIL` | `admin@smartshop.local` | Email de l'administrateur initial |
| `ADMIN_PASSWORD` | — | Mot de passe de l'administrateur initial (≥ 8 caractères) |

Le même `.env` est lu par Docker Compose et par le profil Spring `dev` (profil par défaut).
Sans `DB_PASSWORD` ou `JWT_SECRET`, l'application refuse de démarrer.

> ⚠️ Une variable déjà exportée dans votre shell est **prioritaire** sur `.env`, aussi bien pour Docker Compose que pour Spring. Si `DB_PASSWORD` est exporté globalement (pour un autre projet par exemple), c'est cette valeur qui sera utilisée. Vérifier avec `env | grep -E '^(DB_|JWT_)'`.

## Lancement

### 1. PostgreSQL

```bash
docker compose up -d
docker compose ps          # STATUS doit afficher (healthy)
```

Pour arrêter : `docker compose down` (ajouter `-v` pour supprimer les données ; nécessaire si le mot de passe change après la première initialisation).

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run     # http://localhost:8080
```

Les migrations Flyway s'appliquent automatiquement au démarrage.

### 3. Frontend

```bash
cd frontend
npm install
npm start                  # http://localhost:4200
```

## Authentification et rôles

### Premier démarrage

La création de comptes (`/api/auth/register`) est réservée aux ADMIN. Au démarrage, si la base ne contient **aucun utilisateur**, un compte ADMIN est créé à partir de `ADMIN_USERNAME` / `ADMIN_EMAIL` / `ADMIN_PASSWORD`. Sans `ADMIN_PASSWORD`, rien n'est créé (un avertissement est journalisé). Changer ensuite ce mot de passe.

### Endpoints

| Méthode | URL | Accès | Description |
|---|---|---|---|
| POST | `/api/auth/login` | public | `{username, password}` (username ou email) → access token, refresh token, utilisateur |
| POST | `/api/auth/refresh` | public | `{refreshToken}` → nouvelle paire de tokens (rotation) |
| POST | `/api/auth/logout` | public | `{refreshToken}` → 204, révoque le refresh token |
| GET | `/api/auth/me` | authentifié | utilisateur courant |
| POST | `/api/auth/register` | ADMIN | crée un utilisateur (`username, email, password, firstName, lastName, role`) |

```bash
curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<ADMIN_PASSWORD>"}'
curl -s localhost:8080/api/auth/me -H "Authorization: Bearer <accessToken>"
```

### Fonctionnement des tokens

- **Access token** : JWT HS256 valable 15 min (`JWT_ACCESS_TOKEN_TTL`), il porte le rôle.
- **Refresh token** : valeur aléatoire opaque valable 7 jours, stockée uniquement sous forme d'empreinte SHA-256. Il est **à usage unique** : chaque refresh le remplace. Rejouer un token déjà utilisé révoque toutes les sessions de l'utilisateur (détection de vol).
- Côté Angular, `AuthInterceptor` ajoute le token aux appels `/api`. Sur un 401, il tente un refresh puis rejoue la requête une fois ; si le refresh échoue, l'utilisateur est renvoyé vers `/login`. Les tokens sont stockés dans `localStorage`.

### Rôles et permissions

Hiérarchie : **ADMIN > MANAGER > SELLER / TECHNICIAN** (un rôle hérite des droits des rôles inférieurs).
Matrice cible (appliquée avec `@PreAuthorize` au fil des phases) :

| Module | ADMIN | MANAGER | SELLER | TECHNICIAN |
|---|---|---|---|---|
| Utilisateurs | ✅ gestion | — | — | — |
| Catégories, produits | ✅ | ✅ gestion | lecture | lecture |
| Stock (mouvements, inventaire) | ✅ | ✅ | lecture | sortie de pièces SAV |
| Clients | ✅ | ✅ | ✅ | lecture |
| Fournisseurs, achats | ✅ | ✅ | — | — |
| Ventes | ✅ | ✅ (+ annulation) | ✅ création | — |
| Crédits clients / paiements | ✅ | ✅ | ✅ encaissement | — |
| Crédits fournisseurs | ✅ | ✅ | — | — |
| SAV | ✅ | ✅ | création de tickets | ✅ diagnostic, réparation, statuts |
| Tableau de bord | ✅ complet | ✅ complet | ventes du jour | SAV |

## Référentiel (phase 3)

| Ressource | Lecture | Écriture | Activer / désactiver |
|---|---|---|---|
| `/api/categories` | authentifié | MANAGER (`DELETE` refusé si des produits l'utilisent) | — |
| `/api/products` | authentifié | MANAGER | `PATCH /{id}/status` MANAGER |
| `/api/customers` | authentifié | SELLER | `PATCH /{id}/status` MANAGER |
| `/api/suppliers` | MANAGER | MANAGER | `PATCH /{id}/status` MANAGER |

- **Listes paginées** : `?page=0&size=20&sort=name,asc` (taille maximale 100), réponse `{content, page, size, totalElements, totalPages}`.
- **Filtres** : `q` (recherche « contient », insensible à la casse) et `active=true|false`. Pour les produits, également `categoryId` et `type`.
- **Pas de suppression** des produits, clients et fournisseurs : ils sont désactivés, car les ventes, achats et crédits les référenceront.
- **Quantité en stock** : en lecture seule. Elle vaut 0 à la création et ne change que par les mouvements de stock (phase 4). Un champ `quantity` envoyé dans une requête est ignoré.
- **Normalisation** :
  - SKU, code-barres, CIN et matricule fiscal en majuscules ;
  - emails en minuscules ;
  - téléphones sans espaces (`+216 22 333 444` → `+21622333444`) ;
  - champs texte vides → `null`.
- **Unicité** : SKU, code-barres, téléphone et CIN client, nom de catégorie, raison sociale et matricule fiscal fournisseur (sans tenir compte de la casse pour les noms). Un doublon renvoie une erreur 409.
- **Produits suivis à l'unité** : `serialTracked` et `warrantyMonths` sont portés par le produit. Les IMEI et numéros de série de chaque unité seront gérés avec le stock.

## Stock (phase 4)

| Méthode | URL | Accès | Description |
|---|---|---|---|
| GET | `/api/stock/movements` | authentifié | historique filtrable (`productId`, `type`, `from`, `to` en ISO-8601, `q`), trié du plus récent au plus ancien |
| GET | `/api/stock/movements/{id}` | authentifié | détail, avec les unités concernées |
| POST | `/api/stock/movements` | MANAGER | entrée, sortie, ajustement ou retour |
| POST | `/api/stock/inventory` | MANAGER | comptage physique : un ajustement par écart, en tout-ou-rien |
| GET | `/api/stock/units` | authentifié | recherche d'unités par IMEI ou numéro de série (`q`, `productId`, `status`) |
| GET | `/api/products?lowStock=true` | authentifié | alertes : produits dont la quantité est inférieure ou égale au seuil |

**Règles :**
- **Source unique** : `product.quantity` n'est modifiée que par `StockService`, sous verrou pessimiste du produit. Chaque variation crée un `stock_movement` immuable (variation signée, quantité après, motif, référence, auteur, date). Un test de concurrence vérifie qu'il n'y a jamais de survente.
- **Types et quantités** :
  - `STOCK_IN`, `STOCK_OUT` et `RETURN` : quantité positive ;
  - `ADJUSTMENT` : variation signée, non nulle.
- **Motif** : obligatoire sauf pour une entrée. Une entrée est refusée sur un produit désactivé. Un stock insuffisant renvoie 409.
- **Produits suivis à l'unité** :
  - une entrée crée une unité par article (`units`, avec un IMEI valide — 15 chiffres et clé de Luhn — et/ou un numéro de série) ;
  - une sortie ou un retour désigne les unités existantes (`unitIds`) ;
  - la quantité reste toujours égale au nombre d'unités `IN_STOCK` ;
  - pour ces produits, `serialTracked` n'est plus modifiable tant qu'il reste du stock.
- **Inventaire** : il ne concerne que les produits non suivis ; les produits suivis s'ajustent unité par unité.

## Ventes et achats (phase 5)

| Méthode | URL | Accès | Description |
|---|---|---|---|
| GET | `/api/sales` | SELLER | liste filtrable (`q` : référence, nom ou téléphone du client ; `customerId`, `status`, `paymentStatus`, `from`, `to`) |
| GET | `/api/sales/{id}` | SELLER | détail : lignes, IMEI, garantie, paiements |
| POST | `/api/sales` | SELLER | nouvelle vente |
| POST | `/api/sales/{id}/cancel` | MANAGER | annulation (`reason` obligatoire) |
| GET | `/api/purchases` | MANAGER | liste filtrable (`q`, `supplierId`, `paymentStatus`, `from`, `to`) |
| GET | `/api/purchases/{id}` | MANAGER | détail |
| POST | `/api/purchases` | MANAGER | réception d'un achat fournisseur |

**Règles :**
- **Une seule transaction** : lignes, sorties ou entrées de stock (via `StockService`), paiements et totaux. Toute incohérence annule l'ensemble, y compris les mouvements déjà appliqués aux lignes précédentes ; un test d'intégration le vérifie.
- **Montants** :
  - calculés côté serveur (`BigDecimal`, 3 décimales) ;
  - prix unitaire = **copie du prix de vente** au moment de la vente ;
  - remise globale plafonnée au sous-total ;
  - la somme des paiements ne peut pas dépasser le total.
- **Paiements** : enregistrés séparément (montant, moyen, référence, auteur, date) et immuables. Statut `PAID`, `PARTIALLY_PAID` ou `UNPAID`.
- **Vente à crédit** : une vente avec un reste dû **exige un client** et peut porter une échéance (`dueDate`). Le module crédits (phase 6) s'appuiera sur ces soldes.
- **Produits suivis à l'unité** : la vente désigne les IMEI vendus (`unitIds`), qui passent à `SOLD`. La garantie (mois) est copiée sur la ligne pour le SAV.
- **Annulation** : le stock est restitué (mouvements `RETURN`), les IMEI repassent en stock, et la vente reste dans l'historique avec le statut `CANCELLED`. Les remboursements éventuels ne sont pas encore gérés par l'application.
- **Achat** : alimente le stock (création des IMEI) et met à jour le **dernier prix d'achat** du produit. Un reste dû constitue un crédit fournisseur.
- **Références** : `V-AAAA-NNNNNN` et `A-AAAA-NNNNNN`, générées par séquence PostgreSQL. Une opération refusée consomme quand même un numéro, la numérotation peut donc présenter des trous.

## Crédits (phase 6)

| Méthode | URL | Accès | Description |
|---|---|---|---|
| GET | `/api/customer-credits` | SELLER | crédits clients (`q`, `customerId`, `status`, `open=true` pour les non soldés) |
| GET | `/api/customer-credits/balances` | SELLER | solde par client : encours, part en retard, prochaine échéance (`customerId` facultatif) |
| GET | `/api/customer-credits/{saleId}` | SELLER | détail et historique des paiements |
| POST | `/api/customer-credits/{saleId}/payments` | SELLER | encaissement (`amount`, `method`, `reference`) |
| PATCH | `/api/customer-credits/{saleId}/due-date` | MANAGER | report ou suppression de l'échéance |
| GET, POST, PATCH | `/api/supplier-credits/...` | MANAGER | mêmes opérations pour les achats (`purchaseId`, `supplierId`) |

**Règles :**
- **Définition** : un crédit est une vente ou un achat **non soldé à sa création** (`on_credit`). Il reste visible une fois réglé, avec le statut `PAID`, ce qui conserve l'historique.
- **Reste dû** : il n'est jamais modifié directement. C'est toujours `total - somme des paiements`, et chaque paiement est enregistré séparément et immuable (montant, moyen, référence, auteur, date).
- **Statut calculé** : `CANCELLED` (vente annulée) > `PAID` > `OVERDUE` (échéance dépassée, la date du jour suivant le fuseau `Africa/Tunis`, paramétrable avec `smartshop.business-zone`) > `PARTIALLY_PAID` > `PENDING`. Le filtre SQL et le calcul Java sont testés pour chaque statut.
- **Contrôles des paiements** :
  - un paiement est refusé s'il dépasse le reste dû (400), ou si la vente est annulée ou déjà soldée (409) ;
  - les paiements sont sérialisés par un verrou pessimiste ; un test de concurrence vérifie qu'aucun trop-perçu n'est possible.
- **Soldes** (« solde » des clients et fournisseurs) : agrégation des crédits en cours, triée par encours décroissant. Les ventes annulées sont exclues.

## Tests

```bash
cd backend && ./mvnw verify      # tests + rapport JaCoCo : backend/target/site/jacoco/index.html
cd frontend && npm run test:ci   # tests Karma en Chrome headless
cd frontend && npm run build     # build de production
```

Les tests backend démarrent un PostgreSQL éphémère via Testcontainers : Docker doit être lancé.

## Format des erreurs API

```json
{
  "timestamp": "2026-10-01T21:11:58.247Z",
  "status": 400,
  "message": "Donnees invalides",
  "path": "/api/products",
  "errors": [{ "field": "name", "message": "must not be blank" }]
}
```
