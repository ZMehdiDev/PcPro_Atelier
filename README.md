# PC Pro Atelier

Application interne de suivi des réparations de l'atelier PC Pro : chaque appareil déposé ouvre un
**dossier** qui suit son parcours (diagnostic, attente client ou pièce, réparation, tests, restitution),
avec ses pièces, notes, photos et l'historique de qui a fait quoi.

- `backend/` : API REST Spring Boot 4 (Java 21) + PostgreSQL 16 (migrations Flyway)
- `infra/` : Docker Compose pour PostgreSQL et le backend
- `front/` : application web Angular (ordinateur et téléphone) et page de suivi publique des clients

## Démarrer en local (développement)

Prérequis : Java 21, Docker, Node 22.

```bash
cp .env.example .env               # puis renseigner APP_JWT_SECRET (openssl rand -base64 48)
cd infra && docker compose --env-file ../.env -f docker-compose.dev.yml up -d postgres
cd ../backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Le profil `dev` charge des données de démo (15 clients, 50 dossiers) et active Swagger :
http://localhost:8080/swagger-ui

Comptes de démo (mot de passe `atelier123`) : `zak` (admin), `pere`, `frere` (techniciens).

Front (dans un autre terminal), sur http://localhost:4200 ; les appels `/api` sont relayés vers le backend :

```bash
cd front && npm ci && npm start
```

Tests (Docker requis, ils tournent sur un vrai PostgreSQL via Testcontainers) :

```bash
cd backend && ./mvnw test
```

## Déployer

L'appli est faite pour être accessible depuis l'atelier, la maison ou un téléphone, en HTTPS.
Il faut une machine allumée en permanence (serveur à l'atelier ou petit VPS) et un nom de domaine
(ex : `atelier.pcpro-services.be`). La check-list complète de mise en ligne est dans
[docs/SECURITE.md](docs/SECURITE.md).

Avec **Cloudflare Tunnel** (recommandé pour un serveur à l'atelier : aucun port à ouvrir sur la box) :

```bash
cp .env.example .env    # APP_DOMAIN, POSTGRES_PASSWORD, APP_JWT_SECRET, APP_BOOTSTRAP_ADMIN_PASSWORD, CLOUDFLARE_TUNNEL_TOKEN
cd infra && docker compose --env-file ../.env -f docker-compose.prod.yml -f docker-compose.tunnel.yml up -d --build
```

Sans tunnel (le domaine pointe vers la machine, ports 80/443 ouverts) :

```bash
cd infra && docker compose --env-file ../.env -f docker-compose.prod.yml up -d --build
```

- **Caddy** sert le front (construit dans son image depuis `front/`) et l'API (`/api`) sur le même domaine,
  avec les en-têtes de sécurité (CSP...). Sans tunnel, il obtient le certificat HTTPS tout seul.
  Le backend et PostgreSQL ne sont pas exposés.
- **Sauvegarde** chaque nuit dans `infra/backups` : la base (14 jours gardés) et une copie des photos.
  À copier aussi hors de l'atelier (voir docs/SECURITE.md).
- `docker-compose.dev.yml` lance la même chose en local, sans HTTPS.

Au premier démarrage, le compte `APP_BOOTSTRAP_ADMIN_LOGIN` reçoit le mot de passe
`APP_BOOTSTRAP_ADMIN_PASSWORD` et le rôle admin. L'admin crée ensuite les autres comptes
(`POST /api/employes`). Les photos sont stockées dans le volume `photos_data`.

| Variable | Rôle |
|---|---|
| `APP_DOMAIN` | Domaine public (compose de prod). |
| `CLOUDFLARE_TUNNEL_TOKEN` | Token du tunnel Cloudflare (avec `docker-compose.tunnel.yml`). |
| `APP_JWT_SECRET` | Clé de signature des tokens (32 caractères minimum). **Obligatoire**, l'appli refuse de démarrer sans. |
| `APP_BOOTSTRAP_ADMIN_LOGIN` / `_PASSWORD` | Premier compte admin (n'écrase jamais un mot de passe existant). |
| `APP_CORS_ALLOWED_ORIGINS` | URL(s) du front si elles diffèrent de l'API (inutile en prod derrière Caddy). |
| `APP_COOKIE_SECURE` | `false` seulement en local sans HTTPS (`true` par défaut). |
| `APP_SWAGGER_ENABLED` | `true` pour exposer Swagger (désactivé par défaut). |
| `APP_STORAGE_PHOTOS_DIRECTORY` | Dossier de stockage des photos. |
| `APP_ATELIER_*` | Identité imprimée sur les documents et dans les emails : `_NOM`, `_SLOGAN`, `_RAISON_SOCIALE` (nom légal du titulaire), `_BCE` (0895968808 par défaut), `_ADRESSE`, `_TELEPHONE`, `_EMAIL`, `_SITE_WEB`, `_HORAIRES` (lignes séparées par « · », jours et heures par « : », ex. `Samedi : 10h – 16h · Dimanche et lundi : Fermé`), `_IBAN` / `_BIC` (virement et QR code de paiement sur les factures), `_LOGO` (chemin d'un PNG/JPG, sinon logo dessiné). |
| `APP_ATELIER_TVA_REGIME` | `FRANCHISE` (par défaut : petite entreprise, pas de TVA, mention légale sur les factures) ou `ASSUJETTI` (prix saisis TVAC, factures en HTVA / TVA / TVAC). Taux : `APP_ATELIER_TVA_TAUX` (21). |
| `APP_DEVIS_VALIDITE_JOURS` | Durée de validité imprimée sur les devis (30). `APP_MENTION_FACTURE` : texte libre en bas des factures. |
| `APP_URL_PUBLIQUE` | Adresse de l'appli pour les liens de suivi et le QR code (mise automatiquement en prod). |
| `APP_GARANTIE_MOIS` | Garantie par défaut d'une réparation (3 mois). `APP_CONDITIONS_DEPOT` : conditions du bon de dépôt, une par ligne (vide : conditions par défaut). |
| `SPRING_MAIL_HOST` / `_USERNAME` / `_PASSWORD`, `APP_MAIL_EXPEDITEUR` | Envoi des emails (SMTP, ex : Brevo `smtp-relay.brevo.com`). Vide = aucun email. |

## Front

| Écran | Rôle |
|---|---|
| **Cadre** | Plein écran, menu fixe à gauche sur ordinateur ; barres en haut et en bas sur téléphone. Tout ce qui est cliquable a un contour ou une flèche. Mode clair et sombre. |
| **Recherche rapide** | `Ctrl+K` (ou `/`, ou la loupe sur téléphone) depuis n'importe quel écran : réparations, clients, pages, derniers dossiers ouverts. Flèches et Entrée au clavier. |
| **Accueil** | 4 gros boutons (Nouveau dépôt, Rendre un appareil, Mes réparations, Tout l'atelier) avec leurs compteurs, recherche, remises en retard (si il y en a), mes réparations. |
| **Nouveau dépôt** | Client retrouvé par téléphone ou nom, appareil, problème, accessoires, date promise. Alerte si une garantie est en cours. Récapitulatif toujours visible à droite avec le bouton d'enregistrement. Puis le bon de dépôt à imprimer. |
| **Fiche dossier** | Avancement en 5 étapes, encadré « Que faire maintenant ? » avec l'étape suivante en un clic (« Commencer le diagnostic », « C'est prêt », « Rendre au client » avec encaissement et garantie). Le reste en onglets : Résumé (infos, technicien, priorité, suivi en ligne), Devis et paiement, Pièces, Notes et photos (notes sensibles masquées, photos réduites avant l'envoi), Historique. |
| **Réparations**, **Clients** | Listes avec filtres et recherche ; onglets avec compteurs et étapes cliquables. |
| **Vente au comptoir** | Articles (raccourcis : protection d'écran, écouteurs, chargeur…), quantité, prix, prix d'achat facultatif ; ticket à droite avec le total et le moyen de paiement. Compté dans la caisse et le chiffre d'affaires. |
| **Caisse**, **Chiffre d'affaires**, **Employés** | Réservés aux admins. Chiffre d'affaires : graphique de l'encaissé par jour, semaine ou mois, puis par réparation rendue sur la période, encaissé, coût des pièces et marge, export CSV pour Excel. |
| **Factures** | Réservé aux admins : factures et notes de crédit de la période (PDF, annulation par note de crédit) et journal des recettes en PDF pour le comptable. Une facture se fait depuis la fiche (onglet Devis et paiement) ou après une vente. |
| **Connexion** | Panneau aux couleurs de Pc Pro sur ordinateur, mot de passe affichable. |
| **/suivi** | Page publique du client (QR code du bon de dépôt) : avancement, devis à accepter ou refuser, coordonnées de l'atelier. |

Le token d'accès reste en mémoire ; le cookie de session (30 jours) le renouvelle au chargement et quand l'API répond 401.
Pour que les liens de suivi pointent vers le front en local : `APP_URL_PUBLIQUE=http://localhost:4200`.

## API

Toutes les routes sauf `POST /api/auth/login|refresh|logout`, `GET /api/health` et `/api/public/**` demandent un header
`Authorization: Bearer <token>`. Les erreurs ont toujours le même format JSON
(`status`, `message`, `fieldErrors`, `traceId`).

| | |
|---|---|
| **Connexion** | `POST /api/auth/login` → token (15 min) + cookie de session · `POST /api/auth/refresh` · `POST /api/auth/logout` · `GET /api/auth/moi` · `POST /api/auth/mot-de-passe` |
| **Dossiers** | `POST /api/dossiers` · `GET /api/dossiers?q=&statut=&actifs=&priorite=&technicienId=&mesDossiers=&enRetard=&nonRecuperes=` · `GET /api/dossiers/stats` · `GET /api/dossiers/garanties?clientId=&numeroSerie=` · `GET/PUT /api/dossiers/{id}` · `PATCH /api/dossiers/{id}/statut` · `…/technicien` · `…/priorite` · `POST …/relance` · `GET …/bon-depot` (PDF, exemplaires client et atelier) |
| **Devis** | `PUT /api/dossiers/{id}/devis` · `GET …/devis/pdf` · `POST …/devis/decision` · `POST …/devis/envoi` (email avec le PDF) · `DELETE …/devis` |
| **Factures** | `POST /api/dossiers/{id}/facture` · `GET /api/dossiers/{id}/facture` · `POST /api/ventes/{id}/facture` · `GET /api/ventes/{id}/facture` · `GET /api/factures/{id}` · `GET …/pdf` · `POST …/envoi` (email) · admin : `GET /api/factures?du=&au=`, `POST /api/factures/{id}/annulation` (note de crédit), `GET /api/factures/journal?du=&au=` (PDF) |
| **Paiements** | `POST /api/dossiers/{id}/paiements` · admin : `DELETE …/paiements/{paiementId}`, `GET /api/caisse?du=&au=`, `GET /api/chiffre-affaires?du=&au=`, `GET /api/chiffre-affaires/export?du=&au=` (CSV) |
| **Ventes** | `POST /api/ventes` · admin : `DELETE /api/ventes/{id}` (caisse et chiffre d'affaires les incluent) |
| **Suivi client** (public) | `GET /api/public/suivi/{reference}?code=` (ou `?telephone=`) · `POST /api/public/suivi/{reference}/devis` |
| **Pièces** | `POST /api/dossiers/{id}/pieces` · `PUT …/pieces/{pieceId}` · `PATCH …/pieces/{pieceId}/etat` |
| **Notes** | `POST /api/dossiers/{id}/notes` |
| **Photos** | `POST /api/dossiers/{id}/photos` (multipart `file`) · `GET …/photos/{photoId}/contenu` · `DELETE …/photos/{photoId}` |
| **Clients** | `GET /api/clients?q=` · `GET /api/clients/{id}` (avec ses dossiers) · `PUT /api/clients/{id}` |
| **Employés** | `GET /api/employes` · admin : `POST /api/employes`, `PUT /api/employes/{id}`, `POST /api/employes/{id}/mot-de-passe` |

### Règles métier

- **Statuts du dossier** : `RECU → DIAGNOSTIC → ATTENTE_CLIENT / ATTENTE_PIECE → REPARATION → PRET → CLOTURE`,
  avec les retours en arrière utiles (test raté, client qui revient…). Le détail d'un dossier renvoie
  `statutsPossibles`. Un dossier clôturé n'accepte plus que des notes et seul un admin peut le rouvrir.
- **Dépôt** : le client qui a un email reçoit son bon de dépôt en PDF dès la création du dossier.
- **Prêt** : le client reçoit un email (s'il a une adresse, sauf `notifierClient: false`). Prêt depuis plus
  de 7 jours sans être récupéré : il apparaît dans `nonRecuperes`, et `POST …/relance` renvoie un rappel.
- **Devis** (prix TTC) : le faire met le dossier en `ATTENTE_CLIENT` ; accepté (par l'atelier ou par le
  client en ligne), le dossier repasse en `REPARATION`. Il peut être refait tant qu'il n'est pas accepté.
- **Clôture** : `PATCH …/statut` avec `paiement: {montant, moyen}` et `garantieMois` (3 par défaut, 0 = aucune).
  Un appareil qui revient (même n° de série, ou même client et même modèle) affiche ses garanties en cours.
- **Suivi client** : sans compte, avec la référence et le code de suivi (QR code du bon de dépôt, liens des
  emails) ou le numéro de téléphone. Seul le code permet de répondre au devis. Essais ratés limités.
- **Factures** : numérotées sans trou par année (`2026-0001`, notes de crédit `NC2026-0001`). Une facture
  recopie le client, les lignes (devis accepté, sinon le montant encaissé ; articles pour une vente) et le régime
  de TVA : elle ne change plus. Une erreur se corrige par une note de crédit, puis une nouvelle facture. Redemander
  la facture d'un dossier renvoie celle qui existe. Une vente facturée ne se supprime plus. La facture d'une
  réparation sert de bon de garantie ; s'il reste à payer, elle porte une communication structurée et un QR code
  de virement (norme EPC, lu par les applis bancaires belges).
- **Documents imprimés** : gabarits HTML dans `backend/src/main/resources/documents`, rendus en PDF A4 avec la
  police Inter embarquée. `mvn test -Dtest=ApercuDocumentsTest` génère des exemples dans `target/apercus`.
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
