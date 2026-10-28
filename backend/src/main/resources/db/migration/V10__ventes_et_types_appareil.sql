-- Ventes au comptoir (accessoires, PC d'occasion...) : elles entrent dans la caisse et le chiffre d'affaires
CREATE TABLE vente (
  id BIGSERIAL PRIMARY KEY,
  moyen VARCHAR(20) NOT NULL,
  employe_id BIGINT NOT NULL REFERENCES employe(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT ck_vente_moyen CHECK (moyen IN ('ESPECES', 'BANCONTACT', 'VIREMENT', 'AUTRE'))
);

CREATE INDEX idx_vente_created_at ON vente(created_at);

CREATE TABLE vente_ligne (
  id BIGSERIAL PRIMARY KEY,
  vente_id BIGINT NOT NULL REFERENCES vente(id) ON DELETE CASCADE,
  position INT NOT NULL,
  article VARCHAR(200) NOT NULL,
  quantite INT NOT NULL,
  prix_unitaire NUMERIC(10,2) NOT NULL,
  cout_unitaire NUMERIC(10,2),
  CONSTRAINT ck_vente_ligne_quantite CHECK (quantite > 0),
  CONSTRAINT ck_vente_ligne_prix CHECK (prix_unitaire >= 0),
  CONSTRAINT ck_vente_ligne_cout CHECK (cout_unitaire IS NULL OR cout_unitaire >= 0)
);

CREATE INDEX idx_vente_ligne_vente_id ON vente_ligne(vente_id);

-- Nouveaux types d'appareil : TELEVISION, CONSOLE (colonne texte libre, pas de contrainte à changer)
COMMENT ON COLUMN dossier.type_appareil IS 'PORTABLE, TOUR, TELEVISION, CONSOLE, AUTRE';
