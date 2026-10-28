# Sécurité de l'atelier

Audit fait le 28 octobre 2026, avant la mise en ligne au magasin (serveur à l'atelier derrière
Cloudflare Tunnel). La première partie résume ce qui a été vérifié et corrigé ; la seconde est la
check-list à suivre le jour de l'installation.

## Ce qui était déjà solide

- **Connexion** : mots de passe en bcrypt, même temps de réponse pour un login inconnu ou un mauvais
  mot de passe (on ne peut pas deviner les logins), token d'accès de 15 minutes signé (HS256, clé de
  32 caractères minimum, l'appli refuse de démarrer sans).
- **Sessions longues** : cookie HttpOnly, Secure, SameSite=Strict, limité à `/api/auth`. Le token change
  à chaque utilisation et un token volé puis rejoué déconnecte tous les appareils de l'employé.
  Seule son empreinte (SHA-256) est en base.
- **Droits** : le rôle est relu en base à chaque requête (un employé désactivé ou rétrogradé perd
  l'accès tout de suite). Caisse, chiffre d'affaires, annulation de paiement, gestion des comptes :
  réservés aux admins. Un admin ne peut pas se retirer ses propres droits.
- **Suivi client** : code de suivi aléatoire de 8 caractères (environ 10^12 possibilités), comparaison
  à temps constant, essais limités. Le client ne voit ni les notes internes ni l'historique. Répondre
  au devis demande le code (le téléphone seul ne suffit pas).
- **Photos** : le type est vérifié sur le contenu du fichier (JPEG, PNG, WebP), nom de fichier généré,
  impossible de sortir du dossier de stockage, 10 Mo maximum.
- **Divers** : pas de message technique renvoyé aux clients, emails échappés (pas d'injection HTML),
  export CSV protégé contre les formules Excel, Swagger désactivé en production, backend et base non
  exposés, conteneur du backend sans root.

## Constats et corrections

| Gravité | Constat | Correction |
|---|---|---|
| **Haute** | Derrière Cloudflare Tunnel, toutes les requêtes arrivent de `cloudflared` : l'appli voyait la même adresse IP pour tout le monde. Un seul inconnu qui se trompe 20 fois de mot de passe (ou des clients qui se trompent de code de suivi) bloquait **toutes** les connexions et tout le suivi pendant 15 minutes. | Caddy lit la vraie adresse du client (`CF-Connecting-IP`), seulement quand la requête vient du réseau Docker, et la transmet au backend. Vérifié de bout en bout. |
| **Moyenne** | Après 5 échecs sur un login, ce login était bloqué pour tout le monde : n'importe qui pouvait bloquer le compte du père en tapant 5 faux mots de passe. | Trois compteurs : login + adresse (5 échecs), adresse (20), login toutes adresses confondues (50, pour une attaque répartie). Un inconnu ne bloque plus que lui-même. |
| **Moyenne** | Pas de moyen de couper un téléphone perdu ou volé (sessions de 30 jours) sans changer le mot de passe. | « Déconnecter mes autres appareils » dans Mon compte, et pour un admin « Déconnecter de tous ses appareils » sur la fiche d'un employé. |
| Moyenne | Pas de Content-Security-Policy : en cas de faille XSS, un script étranger pouvait s'exécuter et lire les données. | CSP stricte dans Caddy (seuls les scripts du site), plus COOP, CORP, X-Robots-Tag. Le script du thème est sorti de `index.html` et l'inlining CSS d'Angular (qui utilisait un `onload`) est coupé, pour ne pas devoir autoriser de script en ligne. Testé dans Chromium : aucune violation (connexion, fiche, photos, bon de dépôt, suivi). |
| Moyenne | Les photos n'étaient pas sauvegardées (seulement la base), et les sauvegardes étaient lisibles par tous les utilisateurs du serveur. | Copie quotidienne des photos dans `infra/backups/photos`, fichiers en `700/600`. |
| Basse | Avec la référence (qui se suit) et le numéro de téléphone d'un client, n'importe qui voyait son devis et le reste à payer. | Par téléphone : seulement l'avancement. Montants et devis par le code. |
| Basse | Le changement de mot de passe permettait d'essayer l'ancien sans limite (quelqu'un qui récupère un téléphone resté connecté). | Même limite d'essais que la connexion. |
| Basse | Aucune trace des connexions réussies ou ratées. | Journal : connexions, échecs (login nettoyé + adresse), blocages, changements de mot de passe, déconnexions à distance. |
| Basse | Journaux Docker sans limite de taille (finissent par remplir le disque). | 5 x 10 Mo par conteneur. Conteneurs en `no-new-privileges`, backend sans aucune capacité Linux. |
| Basse | Après connexion, le paramètre `retour` acceptait `//autre-site`. Le routeur Angular ne sortait pas du site, mais autant le refuser. | Seules les pages de l'appli sont acceptées. |
| Basse | L'export CSV ne neutralisait pas les cellules commençant par une tabulation ou un retour chariot. | Ajoutés. |
| Info | `theme-init.js` n'a pas d'empreinte dans son nom : avec l'ancien cache d'un an sur tous les `.js`, une mise à jour ne serait jamais arrivée. | Cache long seulement pour les fichiers avec empreinte. |

## Risques restants (choix assumés ou à décider)

- **Suivi par téléphone** : la référence (`PCP-2026-000123`, qui se suit) et le numéro de téléphone
  suffisent pour voir l'appareil et où en est la réparation. Le devis et le reste à payer ne
  s'affichent plus que par le code (QR du bon de dépôt ou lien de l'email) ; par téléphone, la page
  explique comment les voir.
- **Pas de double authentification**. Les sessions de 30 jours sur téléphone rendent un téléphone
  déverrouillé dangereux : verrouillage du téléphone obligatoire, et « Déconnecter » en cas de perte.
  Option plus tard : Cloudflare Access devant la partie employés (code par email), au prix d'une
  étape en plus pour le père.
- **Limites d'essais en mémoire** : remises à zéro à chaque redémarrage du backend. Suffisant ici.
- **Données en clair sur le disque du serveur** (nom, téléphone, email des clients). Si le serveur est
  volé, elles le sont aussi. Le chiffrement du disque empêcherait le redémarrage automatique après
  une coupure de courant ; on compense par la check-list ci-dessous (BIOS, emplacement, sauvegardes
  externes chiffrées).
- **Dépendances** : `npm audit` ne trouve rien dans ce qui est livré au navigateur. Deux alertes
  concernent `undici`, utilisé seulement par les outils de build. Dependabot propose maintenant les
  mises à jour une fois par mois (`.github/dependabot.yml`).

## Check-list de mise en ligne

### 1. Le serveur (Ubuntu Server 24.04)

- [ ] Mises à jour automatiques : `sudo apt install unattended-upgrades && sudo dpkg-reconfigure -plow unattended-upgrades`
- [ ] Pare-feu : `sudo ufw default deny incoming && sudo ufw allow from 192.168.0.0/16 to any port 22 && sudo ufw enable`
      (adapter au réseau de l'atelier ; avec le tunnel, rien d'autre n'a besoin d'entrer).
- [ ] SSH par clé seulement : dans `/etc/ssh/sshd_config`, `PasswordAuthentication no` et `PermitRootLogin no`.
- [ ] BIOS : mot de passe, démarrage USB désactivé, redémarrage automatique après coupure de courant.
- [ ] Serveur rangé hors de vue des clients (pas sur le comptoir).
- [ ] Ne jamais lancer `docker-compose.dev.yml` sur le serveur : il publie PostgreSQL sur le réseau
      (et Docker passe outre `ufw` pour les ports publiés).

### 2. Les secrets (`.env`)

- [ ] `APP_JWT_SECRET` : `openssl rand -base64 48`
- [ ] `POSTGRES_PASSWORD` : `openssl rand -base64 24` (pas `atelier`)
- [ ] `APP_BOOTSTRAP_ADMIN_PASSWORD` : seulement pour le premier démarrage, **à vider ensuite**.
- [ ] `chmod 600 .env` et ne jamais l'envoyer par WhatsApp/email ni le committer.
- [ ] Garder une copie de `.env` dans un gestionnaire de mots de passe (sans lui, les sauvegardes
      restent lisibles mais il faut tout reconfigurer).

### 3. Cloudflare

- [ ] Zero Trust > Networks > Tunnels : créer le tunnel, mettre son token dans `CLOUDFLARE_TUNNEL_TOKEN`,
      ajouter le « Public hostname » `atelier.pcpro-services.be` → `HTTP` → `caddy:80`.
- [ ] Lancer : `docker compose --env-file ../.env -f docker-compose.prod.yml -f docker-compose.tunnel.yml up -d --build`
- [ ] SSL/TLS > Edge Certificates : « Always Use HTTPS » activé, version TLS minimum 1.2.
- [ ] Security > WAF > Rate limiting rules (1 règle gratuite) : si le chemin commence par `/api/auth/`
      ou `/api/public/`, plus de 20 requêtes en 10 secondes par adresse IP → bloquer 10 secondes.
      L'appli a déjà ses propres limites ; celle-ci arrête le gros trafic avant le serveur.
- [ ] Security > Bots : « Bot Fight Mode » activé.
- [ ] Vérifier que les en-têtes arrivent bien : `curl -sI https://atelier.pcpro-services.be | grep -i content-security`

### 4. Les comptes

- [ ] Un compte par personne (pas de compte partagé), mots de passe de 12 caractères ou plus.
- [ ] Rôle admin seulement pour qui doit voir la caisse et le chiffre d'affaires.
- [ ] Téléphones des employés avec un code de verrouillage.
- [ ] Vérifier dans les journaux que l'adresse des connexions est bien celle du client et pas une
      adresse `172.x` : `docker logs atelier-backend 2>&1 | grep Connexion`

### 5. Les sauvegardes

La sauvegarde de nuit reste sur le serveur : elle ne protège ni du vol, ni de l'incendie, ni d'un disque
mort. Il faut une copie ailleurs, chiffrée. Le plus simple et gratuit : `rclone` vers un Google Drive
(15 Go offerts), avec un « remote » chiffré.

```bash
sudo apt install rclone
rclone config          # 1) remote "drive" (Google Drive)  2) remote "chiffre" de type crypt sur drive:atelier
# Copie chaque nuit à 4 h (après la sauvegarde) : sudo crontab -e
0 4 * * * rclone sync /chemin/vers/PcPro_Atelier/infra/backups chiffre: --log-file /var/log/rclone-atelier.log
```

- [ ] Garder le mot de passe du remote chiffré dans le gestionnaire de mots de passe (sans lui, la copie
      est illisible, même pour nous).
- [ ] **Tester une restauration** une fois (sur un PC à part), et ensuite tous les 6 mois :

```bash
cd infra
# Base : remplace le contenu actuel par la sauvegarde
docker compose --env-file ../.env -f docker-compose.prod.yml exec -T postgres \
  pg_restore -U atelier -d atelier --clean --if-exists < backups/atelier-AAAA-MM-JJ.dump
# Photos
docker run --rm -v infra_photos_data:/app/uploads -v "$PWD/backups/photos:/src:ro" alpine \
  sh -c 'cp -R /src/. /app/uploads/ && chown -R 100:101 /app/uploads'
```

### 6. Mises à jour de l'appli

- [ ] Une fois par mois : `git pull` puis la commande de lancement avec `--build --pull`.
- [ ] Fusionner les propositions de Dependabot après un test rapide.

## En cas de problème

| Situation | Quoi faire |
|---|---|
| Téléphone perdu ou volé | Un admin : Employés > Modifier > « Déconnecter de tous ses appareils ». |
| Mot de passe peut-être connu de quelqu'un | Mon compte > changer le mot de passe (déconnecte les autres appareils). |
| Un employé part | Employés > Modifier > décocher « Compte actif ». |
| `.env` divulgué | Changer `APP_JWT_SECRET`, `POSTGRES_PASSWORD` (aussi dans la base : `ALTER USER atelier PASSWORD '...'`) et le token du tunnel, puis redémarrer. Fermer toutes les sessions : `docker compose ... exec postgres psql -U atelier -c "UPDATE session_employe SET revoked_at = now() WHERE revoked_at IS NULL"` |
| Comportement bizarre | `docker logs atelier-backend 2>&1 \| grep -E "Échec de connexion\|bloqué\|Réutilisation"` |
