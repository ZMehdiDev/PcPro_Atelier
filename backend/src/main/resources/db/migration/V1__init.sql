-- V1__init.sql
-- Core schema for workshop repair tracking (V1)

CREATE TABLE employe (
  id BIGSERIAL PRIMARY KEY,
  prenom VARCHAR(80) NOT NULL,
  actif BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE client (
  id BIGSERIAL PRIMARY KEY,
  nom VARCHAR(120) NOT NULL,
  telephone VARCHAR(32) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_client_telephone UNIQUE (telephone)
);

-- Enums stored as VARCHAR for simplicity
-- statut: RECU, DIAGNOSTIC, ATTENTE_CLIENT, ATTENTE_PIECE, REPARATION, TESTS, PRET, CLOTURE
-- priorite: BASSE, NORMALE, HAUTE

CREATE TABLE dossier (
  id BIGSERIAL PRIMARY KEY,
  reference VARCHAR(40) NOT NULL,
  client_id BIGINT NOT NULL REFERENCES client(id),
  type_appareil VARCHAR(30) NOT NULL, -- PORTABLE, TOUR, AUTRE
  marque VARCHAR(80),
  modele VARCHAR(120),
  numero_serie VARCHAR(120),

  statut VARCHAR(30) NOT NULL,
  bloque BOOLEAN NOT NULL DEFAULT FALSE,
  raison_blocage VARCHAR(200),

  technicien_assigne_id BIGINT REFERENCES employe(id),
  priorite VARCHAR(20) NOT NULL DEFAULT 'NORMALE',

  date_entree DATE NOT NULL,
  date_estimee DATE,

  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  closed_at TIMESTAMPTZ
);

CREATE INDEX idx_dossier_client_id ON dossier(client_id);
CREATE INDEX idx_dossier_statut ON dossier(statut);
CREATE INDEX idx_dossier_reference ON dossier(reference);
ALTER TABLE dossier ADD CONSTRAINT uq_dossier_reference UNIQUE (reference);

-- etat: A_COMMANDER, COMMANDEE, RECUE, MONTEE
CREATE TABLE piece (
  id BIGSERIAL PRIMARY KEY,
  dossier_id BIGINT NOT NULL REFERENCES dossier(id),
  reference VARCHAR(120) NOT NULL,
  fournisseur VARCHAR(120),
  cout NUMERIC(10,2),
  etat VARCHAR(30) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_piece_dossier_id ON piece(dossier_id);
CREATE INDEX idx_piece_etat ON piece(etat);

CREATE TABLE note (
  id BIGSERIAL PRIMARY KEY,
  dossier_id BIGINT NOT NULL REFERENCES dossier(id),
  contenu TEXT NOT NULL,
  sensible BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_note_dossier_id ON note(dossier_id);

CREATE TABLE photo (
  id BIGSERIAL PRIMARY KEY,
  dossier_id BIGINT NOT NULL REFERENCES dossier(id),
  chemin_fichier VARCHAR(500) NOT NULL,
  description VARCHAR(200),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_photo_dossier_id ON photo(dossier_id);

-- type_action: CREATION_DOSSIER, CHANGEMENT_STATUT, AJOUT_NOTE, AJOUT_PIECE, MODIF_PIECE, AJOUT_PHOTO, CLOTURE, ACTION_SENSIBLE
CREATE TABLE evenement (
  id BIGSERIAL PRIMARY KEY,
  dossier_id BIGINT NOT NULL REFERENCES dossier(id),
  employe_id BIGINT NOT NULL REFERENCES employe(id),

  type_action VARCHAR(40) NOT NULL,
  ancienne_valeur TEXT,
  nouvelle_valeur TEXT,
  commentaire TEXT,

  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evenement_dossier_id ON evenement(dossier_id);
CREATE INDEX idx_evenement_employe_id ON evenement(employe_id);
CREATE INDEX idx_evenement_created_at ON evenement(created_at);

-- Seed minimal employees (optional, adjust names)
INSERT INTO employe (prenom) VALUES ('Pere'), ('Frere'), ('Zak');
