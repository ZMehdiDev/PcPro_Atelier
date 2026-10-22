-- V9__suivi_devis_paiement_garantie.sql
-- Suivi en ligne par le client, devis, encaissement, garantie et appareils prêts non récupérés.

ALTER TABLE dossier
  -- Code secret imprimé sur le bon de dépôt (QR code) : donne accès au suivi en ligne du dossier
  ADD COLUMN code_suivi VARCHAR(12),
  -- Date de passage au statut PRET (appareils non récupérés)
  ADD COLUMN pret_at TIMESTAMPTZ,
  -- Fin de garantie de la réparation, fixée à la clôture
  ADD COLUMN fin_garantie DATE;

UPDATE dossier SET code_suivi = upper(substr(md5(random()::text || id::text), 1, 8));
ALTER TABLE dossier ALTER COLUMN code_suivi SET NOT NULL;

UPDATE dossier d
SET pret_at = coalesce(
  (SELECT max(e.created_at) FROM evenement e
   WHERE e.dossier_id = d.id AND e.nouvelle_valeur IN ('Statut: PRET', 'PRET')),
  NOW())
WHERE d.statut = 'PRET';

CREATE INDEX idx_dossier_pret_at ON dossier(pret_at) WHERE statut = 'PRET';
CREATE INDEX idx_dossier_numero_serie ON dossier(lower(numero_serie)) WHERE numero_serie IS NOT NULL;

-- Une action peut venir du client lui-même (décision sur un devis depuis le suivi en ligne)
ALTER TABLE evenement ALTER COLUMN employe_id DROP NOT NULL;

-- Devis : un par dossier, remplacé tant qu'il n'est pas accepté
CREATE TABLE devis (
  id BIGSERIAL PRIMARY KEY,
  dossier_id BIGINT NOT NULL REFERENCES dossier(id) ON DELETE CASCADE,
  statut VARCHAR(20) NOT NULL,
  commentaire VARCHAR(500),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  decide_at TIMESTAMPTZ,
  CONSTRAINT uq_devis_dossier UNIQUE (dossier_id),
  CONSTRAINT ck_devis_statut CHECK (statut IN ('EN_ATTENTE', 'ACCEPTE', 'REFUSE'))
);

CREATE TABLE devis_ligne (
  id BIGSERIAL PRIMARY KEY,
  devis_id BIGINT NOT NULL REFERENCES devis(id) ON DELETE CASCADE,
  position INT NOT NULL,
  type VARCHAR(20) NOT NULL,
  libelle VARCHAR(200) NOT NULL,
  quantite NUMERIC(8,2) NOT NULL,
  prix_unitaire NUMERIC(10,2) NOT NULL,
  CONSTRAINT ck_devis_ligne_type CHECK (type IN ('MAIN_OEUVRE', 'PIECE', 'AUTRE')),
  CONSTRAINT ck_devis_ligne_quantite CHECK (quantite > 0),
  CONSTRAINT ck_devis_ligne_prix CHECK (prix_unitaire >= 0)
);

CREATE INDEX idx_devis_ligne_devis_id ON devis_ligne(devis_id);

-- Encaissements (un dossier peut être payé en plusieurs fois : acompte puis solde)
CREATE TABLE paiement (
  id BIGSERIAL PRIMARY KEY,
  dossier_id BIGINT NOT NULL REFERENCES dossier(id),
  montant NUMERIC(10,2) NOT NULL,
  moyen VARCHAR(20) NOT NULL,
  employe_id BIGINT NOT NULL REFERENCES employe(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT ck_paiement_montant CHECK (montant > 0),
  CONSTRAINT ck_paiement_moyen CHECK (moyen IN ('ESPECES', 'BANCONTACT', 'VIREMENT', 'AUTRE'))
);

CREATE INDEX idx_paiement_dossier_id ON paiement(dossier_id);
CREATE INDEX idx_paiement_created_at ON paiement(created_at);
