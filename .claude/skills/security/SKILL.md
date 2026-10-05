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
- Seuls `POST /api/auth/login`, `/api/auth/refresh`, `/api/auth/logout` sont publics. `/api/auth/register` et `/api/users/**` sont reserves aux ADMIN (aussi dans `authorizeHttpRequests`). Tout nouvel endpoint public doit etre justifie.
- Comptes (`UserService`) : desactivation et reinitialisation du mot de passe revoquent les refresh tokens ; un ADMIN ne se desactive pas et ne change pas son propre role ; toujours au moins un ADMIN actif (`lockActiveAdmins()` avant le controle).
- Hierarchie (`SecurityConfig.roleHierarchy`) : ADMIN > MANAGER > SELLER / TECHNICIAN. Ecrire `hasRole('SELLER')` (pas la liste de tous les roles). Matrice cible dans le README.
- Piege : `@Valid` s'execute AVANT `@PreAuthorize`. Pour un endpoint sensible, ajouter aussi la regle dans `authorizeHttpRequests` afin qu'un non-autorise recoive 403 et non un detail de validation.
- Refresh tokens : opaques, stockes hashes (SHA-256), a usage unique (rotation) ; rejouer un token revoque revoque toutes les sessions de l'utilisateur.
- Premier ADMIN : cree au demarrage via `ADMIN_PASSWORD` si la base est vide. Jamais de compte ou mot de passe dans une migration.
- Frontend : `AuthService` / `authInterceptor` / `authGuard` (`data: { roles: [...] }`) dans `core/`.
- Erreurs 401/403 au format `ApiError` (`JsonAuthenticationEntryPoint`), sans detail interne.
- Secrets : uniquement via variables d'environnement / `.env` (non versionne). Mettre a jour `.env.example` sans valeur reelle.
- CORS : origines explicites via `CORS_ALLOWED_ORIGINS`, jamais `*`.
- Tester chaque regle d'acces (anonyme -> 401, mauvais role -> 403, bon role -> 2xx).
