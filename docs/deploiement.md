# Déploiement gratuit (démo portfolio)

Architecture :

```text
Navigateur ──> Netlify (Angular, statique)
     │
     └──────> Render (API Spring Boot, Docker)  ──>  Neon (PostgreSQL, SSL)
```

Le frontend appelle l'API **directement** sur son domaine Render (CORS), sans proxy Netlify :
le proxy Netlify abandonne au bout d'environ 30 s, alors que l'API gratuite met jusqu'à une minute
à sortir de veille.

| Service | Offre gratuite | À savoir |
|---|---|---|
| Netlify | site statique | — |
| Render | 750 h/mois, 512 Mo | veille après 15 min sans trafic, ~1 min pour se réveiller |
| Neon | 1 Go, 100 h de calcul/mois | veille après 5 min, réveil en quelques secondes |

Fichiers utilisés : `backend/Dockerfile`, `backend/src/main/resources/application-prod.yml`,
`render.yaml`, `netlify.toml`, script `build:netlify` de `frontend/package.json`.

Le dépôt doit être sur GitHub (ou GitLab) : Render et Netlify déploient depuis le dépôt.

---

## 1. Base de données — Neon

1. Créer un compte sur <https://neon.com>, puis un projet `smartshop` (région : Europe, ex. Frankfurt).
2. **Connect** → décocher **Connection pooling** (hôte « direct », sans `-pooler` : Flyway en a besoin).
3. Noter : hôte (`ep-xxx.eu-central-1.aws.neon.tech`), base (`neondb`), utilisateur, mot de passe.

Les migrations Flyway créent les tables au premier démarrage de l'API.

## 2. API — Render

1. Créer un compte sur <https://render.com> et relier GitHub.
2. **New → Blueprint**, choisir le dépôt : Render lit `render.yaml` et crée le service `smartshop-api` (Docker, gratuit).
3. Renseigner les variables demandées :

| Variable | Valeur |
|---|---|
| `DB_HOST` | hôte Neon (direct) |
| `DB_NAME` | `neondb` |
| `DB_USERNAME` / `DB_PASSWORD` | identifiants Neon |
| `CORS_ALLOWED_ORIGINS` | URL Netlify (étape 3), ex. `https://smartshop-demo.netlify.app` — mettre une valeur provisoire puis la corriger |
| `ADMIN_EMAIL` | votre email |
| `ADMIN_PASSWORD` | mot de passe fort, **différent** de celui du `.env` local |

`JWT_SECRET` est généré par Render. Le premier build Docker prend quelques minutes.

4. Vérifier : `https://<service>.onrender.com/api/auth/me` doit répondre **401** (API en ligne, accès protégé).

## 3. Frontend — Netlify

1. Créer un compte sur <https://netlify.com> → **Add new site → Import an existing project** → le dépôt.
   Netlify lit `netlify.toml` (base `frontend`, commande `npm run build:netlify`, dossier `dist/frontend/browser`).
2. **Site configuration → Environment variables** :

| Variable | Valeur |
|---|---|
| `SMARTSHOP_API_URL` | `https://<service>.onrender.com/api` |
| `DEMO_USERNAME` / `DEMO_PASSWORD` | compte de démonstration (étape 4) — lettres et chiffres uniquement |

3. **Site configuration → Change site name** (ex. `smartshop-demo`), puis **Deploys → Trigger deploy**.
4. Reporter l'URL Netlify dans `CORS_ALLOWED_ORIGINS` sur Render (le service redémarre).

Toute modification d'une variable Netlify demande un nouveau déploiement : les valeurs sont intégrées au build.

## 4. Compte de démonstration

Ne jamais publier le mot de passe ADMIN.

1. Se connecter sur le site Netlify avec le compte ADMIN.
2. **Administration → Utilisateurs → Nouvel utilisateur** : identifiant `demo`, rôle **Gérant**
   (accès à tout sauf l'administration des comptes).
3. Reporter l'identifiant et le mot de passe dans `DEMO_USERNAME` / `DEMO_PASSWORD` sur Netlify, redéployer.

La page de connexion affiche alors un encadré « Compte de démonstration » avec un bouton **Remplir**.
Pensez à saisir quelques catégories, produits, clients et ventes pour que le tableau de bord soit parlant.

## 5. Mise en veille

Au premier accès après 15 min d'inactivité, l'API met environ une minute à démarrer.
La page de connexion la réveille dès son ouverture et affiche un message si la connexion tarde.

Pour la garder éveillée (facultatif) : un moniteur gratuit (ex. UptimeRobot) qui appelle
`https://<service>.onrender.com/api/auth/me` toutes les 10 min. Un service actif en continu consomme
environ 744 h/mois, sous les 750 h gratuites. Ce point d'accès répond 401 sans toucher la base :
Neon peut rester en veille.

## Tester l'image en local

```bash
docker build -t smartshop-api backend/
docker run --rm -m 512m --network host \
  -e PORT=8090 -e DB_HOST=localhost -e DB_PORT=5432 -e DB_NAME=smartshop \
  -e DB_USERNAME=smartshop -e DB_PASSWORD=... -e DB_SSLMODE=disable \
  -e JWT_SECRET=... -e CORS_ALLOWED_ORIGINS=http://localhost:4300 \
  smartshop-api
```

`DB_SSLMODE=disable` uniquement pour une base locale sans SSL ; en production la valeur par défaut est `require`.
