# Mettre l'appli en ligne sur Oracle Cloud (gratuit)

L'appli tourne sur un serveur gratuit d'Oracle (offre « Always Free »), allumé 24 h/24. Rien ne tourne
au magasin ni à la maison. Compter une heure la première fois.

Ce qu'il faut savoir sur l'offre gratuite :

- gratuit « pour la vie du compte », dans la limite de 2 processeurs, 12 Go de RAM et 200 Go de disque ;
- Oracle peut supprimer une machine qu'il juge inactive (processeur, réseau **et** mémoire sous 20 %
  pendant 7 jours). L'appli garde 2 Go de mémoire occupés en permanence (`APP_JAVA_OPTIONS`), ce qui
  suffit sur une machine de 6 Go ;
- Oracle peut changer ses règles (il a divisé la part gratuite par deux en juin 2026) : la copie des
  sauvegardes hors du serveur (étape 7) n'est pas optionnelle.

## 1. Créer le compte (téléphone ou PC)

1. https://www.oracle.com/cloud/free/ > « Start for free ».
2. **Région d'origine** : `Germany Central (Frankfurt)` ou `Netherlands Northwest (Amsterdam)`.
   Elle ne peut plus être changée ensuite, et les machines gratuites ne peuvent être créées que là.
3. La carte bancaire sert à vérifier l'identité. Rien n'est prélevé tant qu'on reste dans le gratuit
   (ne pas cliquer sur « Upgrade to Pay As You Go »).

## 2. Créer le serveur (téléphone ou PC)

Menu > Compute > Instances > **Create instance** :

| Réglage | Valeur |
|---|---|
| Name | `atelier` |
| Image | **Canonical Ubuntu 24.04** (bouton « Change image ») |
| Shape | « Change shape » > Ampere > **VM.Standard.A1.Flex**, **1 OCPU**, **6 GB** de mémoire |
| Networking | laisser par défaut (nouveau réseau, **adresse IPv4 publique** cochée) |
| SSH keys | **Generate a key pair for me** > télécharger la **clé privée** (et la garder précieusement) |
| Boot volume | laisser par défaut (environ 50 Go) |

Puis « Create ». Si Oracle répond « Out of capacity », réessayer plus tard (le soir ou le matin tôt)
ou changer d'« Availability domain ».

Une fois la machine en vert (« Running »), noter son **Public IP address**.

## 3. Ouvrir les ports 80 et 443

Sur la page de l'instance : Networking > le **Subnet** > Security > la « Default Security List » >
**Add Ingress Rules**, trois fois :

| Source CIDR | IP Protocol | Destination port |
|---|---|---|
| `0.0.0.0/0` | TCP | `80` |
| `0.0.0.0/0` | TCP | `443` |
| `0.0.0.0/0` | UDP | `443` |

(Le port 22 pour SSH est déjà ouvert.)

## 4. Se connecter au serveur (PC conseillé)

Depuis Windows (PowerShell), dans le dossier où est la clé :

```powershell
ssh -i .\ssh-key-AAAA-MM-JJ.key ubuntu@IP_DU_SERVEUR
```

Si Windows refuse la clé (« permissions too open ») : clic droit sur le fichier > Propriétés >
Sécurité > Avancé > désactiver l'héritage, et ne garder que son propre utilisateur.

Sans PC : dans la console Oracle, bouton **Cloud Shell** (icône terminal en haut à droite), y
envoyer la clé (menu du Cloud Shell > Upload), puis `chmod 600 cle.key && ssh -i cle.key ubuntu@IP`.

## 5. Installer et lancer l'appli (une commande)

Sur le serveur :

```bash
git clone https://github.com/ZMehdiDev/PcPro_Atelier.git
cd PcPro_Atelier
sudo bash infra/installer-serveur.sh
```

Le script met Ubuntu à jour (et active les mises à jour de sécurité automatiques), installe Docker,
ouvre les ports dans le pare-feu d'Ubuntu, crée `.env` avec des mots de passe aléatoires, puis
construit et lance l'appli (10 à 20 minutes la première fois). À la fin, il affiche :

- l'adresse provisoire, par exemple `https://141-145-12-34.sslip.io` (en attendant le vrai domaine) ;
- le mot de passe du premier compte admin `zak`.

**Copier `.env` dans le gestionnaire de mots de passe** (`cat .env`) : sans lui, il faut tout
reconfigurer. Ne pas l'envoyer par WhatsApp ou par e-mail.

Après la première connexion : changer le mot de passe dans Mon compte, puis vider la ligne
`APP_BOOTSTRAP_ADMIN_PASSWORD=` dans `.env` (`nano .env`).

## 6. Le vrai domaine : atelier.pcpro-services.be

Le domaine est géré chez **Hostinger**. Dans hPanel > Domaines > pcpro-services.be > **DNS / Nameservers**,
ajouter un enregistrement :

| Type | Nom | Pointe vers | TTL |
|---|---|---|---|
| A | `atelier` | IP du serveur | 3600 |

Le site actuel n'est pas touché. Attendre quelques minutes, puis sur le serveur :

```bash
cd ~/PcPro_Atelier && sudo bash infra/installer-serveur.sh atelier.pcpro-services.be
```

Caddy obtient alors le certificat HTTPS pour le vrai domaine. Les QR codes des bons de dépôt pointent
vers l'adresse en place au moment de l'impression : passer au vrai domaine **avant** d'imprimer des
bons pour de vrais clients.

## 7. Le reste de la check-list

Dans [SECURITE.md](SECURITE.md), à faire aussi sur Oracle :

- **E-mails (Brevo)** : authentifier `pcpro-services.be` dans Brevo (Senders, domains > Domains), et
  ajouter chez Hostinger les enregistrements DNS qu'il donne (SPF, DKIM, DMARC). Puis remplir
  `SPRING_MAIL_*` et `APP_MAIL_EXPEDITEUR` dans `.env` et relancer le script.
- **Comptes** (section 4) : un compte par personne, le père en **admin** (il a besoin de la caisse).
- **Sauvegardes hors du serveur** (section 5) : `rclone` chiffré vers Google Drive, et un test de
  restauration.
- **Mises à jour** (section 6) : `git pull` puis relancer le script.

Les sections « pare-feu `ufw` », « BIOS » et « Cloudflare » de SECURITE.md concernent un serveur au
magasin : **ne pas activer `ufw` sur Oracle** (il casse les règles réseau d'Oracle), et pas de BIOS
à régler. Le SSH d'Oracle n'accepte déjà que les clés.

## Si quelque chose ne marche pas

| Problème | Quoi vérifier |
|---|---|
| La page ne s'ouvre pas du tout | Étape 3 (règles d'entrée), puis `sudo docker ps` : 4 conteneurs `Up`. |
| Erreur de certificat | `sudo docker logs atelier-caddy` : le domaine doit pointer vers l'IP du serveur. Avec `sslip.io`, si Let's Encrypt refuse (trop de certificats cette semaine), essayer `IP.nip.io` à la place. |
| L'appli ne démarre pas | `sudo docker logs atelier-backend` |
| Machine supprimée par Oracle | Recréer le serveur (étapes 2 à 5), remettre l'ancien `.env`, puis restaurer la dernière sauvegarde (SECURITE.md, section 5). |
