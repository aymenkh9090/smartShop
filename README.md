# SmartShop

Application web de gestion pour une boutique de smartphones, récepteurs, abonnements IPTV, chargeurs, câbles, écouteurs, montres connectées et accessoires.

Elle centralise : stock, ventes, achats, clients, fournisseurs, crédits clients et fournisseurs, service après-vente (SAV) et tableau de bord.

> État actuel : **Phase 1 – initialisation** terminée (socle backend, frontend et base de données). Voir `CLAUDE.md` pour la feuille de route.

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
