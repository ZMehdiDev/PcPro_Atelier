-- Factures et notes de crédit, pour une réparation (dossier) ou une vente au comptoir.
-- Une facture émise ne change plus : ses lignes, le client et le régime de TVA sont recopiés à l'émission.
-- Une erreur se corrige par une note de crédit, qui annule la facture (on peut ensuite en émettre une nouvelle).

CREATE TABLE facture (
  id BIGSERIAL PRIMARY KEY,
  type VARCHAR(20) NOT NULL,
  annee INT NOT NULL,
  sequence INT NOT NULL,
  numero VARCHAR(20) NOT NULL,
  date_emission DATE NOT NULL,
  dossier_id BIGINT REFERENCES dossier(id),
  vente_id BIGINT REFERENCES vente(id),
  facture_origine_id BIGINT REFERENCES facture(id),
  client_nom VARCHAR(150) NOT NULL,
  client_adresse VARCHAR(300),
  client_tva VARCHAR(30),
  client_email VARCHAR(150),
  regime_tva VARCHAR(20) NOT NULL,
  taux_tva NUMERIC(5,2) NOT NULL,
  total NUMERIC(10,2) NOT NULL,
  employe_id BIGINT NOT NULL REFERENCES employe(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_facture_numero UNIQUE (numero),
  CONSTRAINT uq_facture_sequence UNIQUE (type, annee, sequence),
  CONSTRAINT ck_facture_type CHECK (type IN ('FACTURE', 'NOTE_CREDIT')),
  CONSTRAINT ck_facture_regime CHECK (regime_tva IN ('FRANCHISE', 'ASSUJETTI')),
  CONSTRAINT ck_facture_objet CHECK ((dossier_id IS NULL) <> (vente_id IS NULL)),
  CONSTRAINT ck_facture_origine CHECK ((type = 'NOTE_CREDIT') = (facture_origine_id IS NOT NULL))
);

CREATE INDEX idx_facture_dossier_id ON facture(dossier_id);
CREATE INDEX idx_facture_vente_id ON facture(vente_id);
CREATE INDEX idx_facture_date_emission ON facture(date_emission);
-- Une facture n'est annulée qu'une fois
CREATE UNIQUE INDEX uq_facture_note_credit ON facture(facture_origine_id) WHERE facture_origine_id IS NOT NULL;

CREATE TABLE facture_ligne (
  id BIGSERIAL PRIMARY KEY,
  facture_id BIGINT NOT NULL REFERENCES facture(id) ON DELETE CASCADE,
  position INT NOT NULL,
  libelle VARCHAR(200) NOT NULL,
  detail VARCHAR(100),
  quantite NUMERIC(8,2) NOT NULL,
  prix_unitaire NUMERIC(10,2) NOT NULL,
  total NUMERIC(10,2) NOT NULL
);

CREATE INDEX idx_facture_ligne_facture_id ON facture_ligne(facture_id);

-- Numérotation continue par type et par année (2026-0001, NC2026-0001), attribuée atomiquement
CREATE TABLE facture_compteur (
  type VARCHAR(20) NOT NULL,
  annee INT NOT NULL,
  dernier INT NOT NULL,
  PRIMARY KEY (type, annee)
);
