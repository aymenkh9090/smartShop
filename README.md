# SmartShop

Application web de gestion pour une boutique de smartphones, récepteurs, abonnements IPTV, chargeurs, câbles, écouteurs, montres connectées et accessoires.

Elle centralise : stock, ventes, achats, clients, fournisseurs, crédits clients et fournisseurs, service après-vente (SAV) et tableau de bord.

> État actuel : **Phase 2 – sécurité** terminée (utilisateurs, rôles, JWT, refresh tokens, login Angular). Voir `CLAUDE.md` pour la feuille de route.

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
| Frontend | Angular 20 (standalone), TypeScript, Router, HttpClient, Reactive Forms, RxJS, Signals |
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
