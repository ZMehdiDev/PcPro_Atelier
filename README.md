# PC Pro Atelier

Application interne de suivi des réparations de l'atelier PC Pro : chaque appareil déposé ouvre un
**dossier** qui suit son parcours (diagnostic, attente client ou pièce, réparation, tests, restitution),
avec ses pièces, notes, photos et l'historique de qui a fait quoi.

- `backend/` : API REST Spring Boot 4 (Java 21) + PostgreSQL 16 (migrations Flyway)
- `infra/` : Docker Compose pour PostgreSQL et le backend
- Front Angular : à venir

## Démarrer en local (développement)

Prérequis : Java 21, Docker.

```bash
cp .env.example .env               # puis renseigner APP_JWT_SECRET (openssl rand -base64 48)
cd infra && docker compose --env-file ../.env -f docker-compose.dev.yml up -d postgres
cd ../backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Le profil `dev` charge des données de démo (15 clients, 50 dossiers) et active Swagger :
http://localhost:8080/swagger-ui

Comptes de démo (mot de passe `atelier123`) : `zak` (admin), `pere`, `frere` (techniciens).

Tests (Docker requis, ils tournent sur un vrai PostgreSQL via Testcontainers) :

```bash
cd backend && ./mvnw test
```

## Déployer

L'appli est faite pour être accessible depuis l'atelier, la maison ou un téléphone, en HTTPS.
Il faut une machine allumée en permanence (serveur à l'atelier ou petit VPS), un nom de domaine
(ex : `atelier.pcpro-services.be`) qui pointe vers elle, et les ports 80/443 ouverts.

```bash
cp .env.example .env    # APP_DOMAIN, POSTGRES_PASSWORD, APP_JWT_SECRET et APP_BOOTSTRAP_ADMIN_PASSWORD
cd infra && docker compose --env-file ../.env -f docker-compose.prod.yml up -d --build
```

- **Caddy** obtient le certificat HTTPS tout seul et sert le front (dossier `infra/front`) et l'API
  (`/api`) sur le même domaine. Le backend et PostgreSQL ne sont pas exposés.
- **Sauvegarde** de la base chaque nuit dans `infra/backups` (14 jours gardés). À copier ailleurs
  régulièrement, comme le volume `photos_data`.
- `docker-compose.dev.yml` lance la même chose en local, sans HTTPS.

Au premier démarrage, le compte `APP_BOOTSTRAP_ADMIN_LOGIN` reçoit le mot de passe
`APP_BOOTSTRAP_ADMIN_PASSWORD` et le rôle admin. L'admin crée ensuite les autres comptes
(`POST /api/employes`). Les photos sont stockées dans le volume `photos_data`.

| Variable | Rôle |
|---|---|
| `APP_DOMAIN` | Domaine public (compose de prod). |
| `APP_JWT_SECRET` | Clé de signature des tokens (32 caractères minimum). **Obligatoire**, l'appli refuse de démarrer sans. |
| `APP_BOOTSTRAP_ADMIN_LOGIN` / `_PASSWORD` | Premier compte admin (n'écrase jamais un mot de passe existant). |
| `APP_CORS_ALLOWED_ORIGINS` | URL(s) du front si elles diffèrent de l'API (inutile en prod derrière Caddy). |
| `APP_COOKIE_SECURE` | `false` seulement en local sans HTTPS (`true` par défaut). |
| `APP_SWAGGER_ENABLED` | `true` pour exposer Swagger (désactivé par défaut). |
| `APP_STORAGE_PHOTOS_DIRECTORY` | Dossier de stockage des photos. |

## API

Toutes les routes sauf `POST /api/auth/login|refresh|logout` et `GET /api/health` demandent un header
`Authorization: Bearer <token>`. Les erreurs ont toujours le même format JSON
(`status`, `message`, `fieldErrors`, `traceId`).

| | |
|---|---|
| **Connexion** | `POST /api/auth/login` → token (15 min) + cookie de session · `POST /api/auth/refresh` · `POST /api/auth/logout` · `GET /api/auth/moi` · `POST /api/auth/mot-de-passe` |
| **Dossiers** | `POST /api/dossiers` · `GET /api/dossiers?q=&statut=&actifs=&priorite=&bloque=&technicienId=&mesDossiers=&enRetard=` · `GET /api/dossiers/stats` · `GET/PUT /api/dossiers/{id}` · `PATCH /api/dossiers/{id}/statut` · `…/technicien` · `…/priorite` · `…/blocage` |
| **Pièces** | `POST /api/dossiers/{id}/pieces` · `PUT …/pieces/{pieceId}` · `PATCH …/pieces/{pieceId}/etat` |
| **Notes** | `POST /api/dossiers/{id}/notes` |
| **Photos** | `POST /api/dossiers/{id}/photos` (multipart `file`) · `GET …/photos/{photoId}/contenu` · `DELETE …/photos/{photoId}` |
| **Clients** | `GET /api/clients?q=` · `GET /api/clients/{id}` (avec ses dossiers) · `PUT /api/clients/{id}` |
| **Employés** | `GET /api/employes` · admin : `POST /api/employes`, `PUT /api/employes/{id}`, `POST /api/employes/{id}/mot-de-passe` |

### Règles métier

- **Statuts du dossier** : `RECU → DIAGNOSTIC → ATTENTE_CLIENT / ATTENTE_PIECE → REPARATION → TESTS → PRET → CLOTURE`,
  avec les retours en arrière utiles (test raté, client qui revient…). Le détail d'un dossier renvoie
  `statutsPossibles`. Un dossier bloqué ne peut pas être clôturé ; un dossier clôturé n'accepte plus
  que des notes et seul un admin peut le rouvrir.
- **Pièces** : `A_COMMANDER → COMMANDEE → RECUE → MONTEE` (ou `ANNULEE`). Une pièce commandée met le
  dossier en `ATTENTE_PIECE` ; quand toutes les pièces attendues sont reçues, il repasse en `REPARATION`.
- **Clients** : retrouvés par numéro de téléphone (normalisé : `0470 12 34 56` = `0470123456`).
- **Historique** : chaque action est enregistrée avec l'employé connecté.
- **Rester connecté** : la connexion pose un cookie `HttpOnly` valable 30 jours (prolongé à chaque
  utilisation) ; le front appelle `/api/auth/refresh` quand le token de 15 min expire. Chaque refresh
  change le cookie, et un ancien cookie rejoué plus tard (copié) déconnecte tous les appareils de l'employé.
- **Sécurité** : mots de passe bcrypt, blocage 15 min après 5 échecs sur un login ou 20 depuis une même
  adresse IP, tous les appareils déconnectés quand le mot de passe change ou que l'employé est désactivé, photos vérifiées sur leur contenu
  (JPEG/PNG/WebP, 10 Mo max).
