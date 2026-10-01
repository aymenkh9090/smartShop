---
name: security
description: Regles de securite SmartShop - authentification JWT, roles, autorisations, secrets, CORS. A lire avant de toucher a security/, auth/ ou d'ajouter un endpoint.
---

# Securite SmartShop

- API stateless : pas de session, CSRF desactive, JWT dans `Authorization: Bearer`.
- JWT via Spring Security OAuth2 Resource Server (Nimbus) : `JwtEncoder`/`JwtDecoder` HS256, secret `JWT_SECRET` (>= 32 caracteres, `JwtProperties`).
- Mots de passe : `PasswordEncoder` (BCrypt) uniquement. Jamais loggues, jamais renvoyes dans un DTO.
- Roles : `ADMIN`, `MANAGER`, `SELLER`, `TECHNICIAN`. Definir la matrice de permissions avant d'ecrire l'endpoint.
- Autorisation : `@PreAuthorize` sur les controllers/services ; tout est `authenticated()` par defaut dans `SecurityConfig`.
- Seuls `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh` sont publics. Tout nouvel endpoint public doit etre justifie.
- Erreurs 401/403 au format `ApiError` (`JsonAuthenticationEntryPoint`), sans detail interne.
- Secrets : uniquement via variables d'environnement / `.env` (non versionne). Mettre a jour `.env.example` sans valeur reelle.
- CORS : origines explicites via `CORS_ALLOWED_ORIGINS`, jamais `*`.
- Tester chaque regle d'acces (anonyme -> 401, mauvais role -> 403, bon role -> 2xx).
