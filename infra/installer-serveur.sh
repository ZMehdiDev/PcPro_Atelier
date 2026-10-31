#!/usr/bin/env bash
# Installe et lance l'appli sur un serveur Ubuntu neuf (Oracle Cloud ou autre), en une commande.
# Guide complet : docs/MISE-EN-LIGNE-ORACLE.md
#
#   sudo bash infra/installer-serveur.sh                              # adresse provisoire <ip>.sslip.io
#   sudo bash infra/installer-serveur.sh atelier.pcpro-services.be    # le vrai domaine
#
# Relançable sans risque : les mots de passe déjà dans .env sont gardés, seul le domaine est mis à jour.
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "À lancer avec sudo : sudo bash infra/installer-serveur.sh" >&2
  exit 1
fi

racine="$(cd "$(dirname "$0")/.." && pwd)"
env_fichier="$racine/.env"

etape() { printf '\n==> %s\n' "$1"; }

# Écrit CLE=valeur dans .env (remplace la ligne si elle existe). Les valeurs ne contiennent ni | ni espace.
ecrire() {
  if grep -q "^$1=" "$env_fichier"; then
    sed -i "s|^$1=.*|$1=$2|" "$env_fichier"
  else
    echo "$1=$2" >> "$env_fichier"
  fi
}
lire() { grep "^$1=" "$env_fichier" | head -n1 | cut -d= -f2-; }

etape "Mises à jour du système"
export DEBIAN_FRONTEND=noninteractive
apt-get update -q
apt-get upgrade -y -q
apt-get install -y -q curl openssl git unattended-upgrades
# Mises à jour de sécurité automatiques
echo 'APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";' > /etc/apt/apt.conf.d/20auto-upgrades

etape "Docker"
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
fi
systemctl enable --now docker

etape "Ports 80 et 443"
# Les images Ubuntu d'Oracle bloquent tout sauf SSH dans iptables (en plus de la "security list" de la
# console Oracle, à ouvrir aussi). Ne pas utiliser ufw sur ces images : il entre en conflit avec ces règles.
if [ -f /etc/iptables/rules.v4 ] && command -v netfilter-persistent >/dev/null 2>&1; then
  for regle in "-p tcp --dport 80" "-p tcp --dport 443" "-p udp --dport 443"; do
    # shellcheck disable=SC2086
    iptables -C INPUT $regle -j ACCEPT 2>/dev/null || iptables -I INPUT 1 $regle -j ACCEPT
  done
  netfilter-persistent save
  echo "Ouverts dans iptables."
else
  echo "Pas de règles iptables Oracle : rien à faire ici."
fi

etape "Fichier .env (mots de passe)"
if [ ! -f "$env_fichier" ]; then
  cp "$racine/.env.example" "$env_fichier"
  ecrire POSTGRES_PASSWORD "$(openssl rand -hex 24)"
  ecrire APP_JWT_SECRET "$(openssl rand -hex 48)"
  ecrire APP_CORS_ALLOWED_ORIGINS ""
  ecrire APP_BOOTSTRAP_ADMIN_LOGIN zak
  ecrire APP_BOOTSTRAP_ADMIN_PASSWORD "$(openssl rand -base64 18 | tr -d '/+=' | cut -c1-16)"
  nouveau=1
  echo "Créé avec des mots de passe aléatoires."
else
  nouveau=0
  echo "Déjà présent : mots de passe gardés."
fi

domaine="${1:-}"
if [ -z "$domaine" ]; then
  domaine="$(lire APP_DOMAIN)"
fi
if [ -z "$domaine" ]; then
  ip="$(curl -4fsS https://api.ipify.org)"
  domaine="${ip//./-}.sslip.io"
fi
ecrire APP_DOMAIN "$domaine"
chmod 600 "$env_fichier"
# Lisible par l'utilisateur qui a lancé sudo (pour le copier ou le modifier sans sudo), personne d'autre
if [ -n "${SUDO_USER:-}" ]; then chown "$SUDO_USER" "$env_fichier"; fi
echo "Adresse de l'appli : https://$domaine"

etape "Construction et lancement de l'appli (10 à 20 minutes la première fois)"
cd "$racine/infra"
docker compose --env-file ../.env -f docker-compose.prod.yml up -d --build

etape "Terminé"
echo "Ouvre https://$domaine (le certificat HTTPS peut prendre une minute à arriver)."
if [ "$nouveau" -eq 1 ]; then
  echo
  echo "Premier compte admin : identifiant zak, mot de passe $(lire APP_BOOTSTRAP_ADMIN_PASSWORD)"
  echo "Après ta première connexion : change ce mot de passe dans Mon compte, puis vide la ligne"
  echo "APP_BOOTSTRAP_ADMIN_PASSWORD dans .env (nano .env)."
fi
