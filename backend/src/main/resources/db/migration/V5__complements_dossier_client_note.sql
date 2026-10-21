-- V5__complements_dossier_client_note.sql
-- Informations manquantes pour le suivi au quotidien.

-- Dossier : description du problème (auparavant seulement dans l'historique) et accessoires déposés
ALTER TABLE dossier
  ADD COLUMN description_probleme TEXT,
  ADD COLUMN accessoires VARCHAR(255);

-- Récupère la description saisie à la création, stockée jusqu'ici dans le commentaire de l'événement
UPDATE dossier d
SET description_probleme = e.commentaire
FROM evenement e
WHERE e.dossier_id = d.id
  AND e.type_action = 'CREATION_DOSSIER'
  AND e.commentaire IS NOT NULL
  AND d.description_probleme IS NULL;

CREATE INDEX idx_dossier_technicien_assigne_id ON dossier(technicien_assigne_id);
CREATE INDEX idx_dossier_created_at ON dossier(created_at);

-- Client : email (pour prévenir le client plus tard) et téléphone normalisé (chiffres et + en tête)
ALTER TABLE client ADD COLUMN email VARCHAR(255);

UPDATE client
SET telephone = CASE WHEN btrim(telephone) LIKE '+%' THEN '+' ELSE '' END
                || regexp_replace(telephone, '[^0-9]', '', 'g');

-- Note : auteur
ALTER TABLE note ADD COLUMN employe_id BIGINT REFERENCES employe(id);

-- Photo : métadonnées obligatoires (renseignées par V3 pour les lignes existantes)
ALTER TABLE photo
  ALTER COLUMN mime_type SET NOT NULL,
  ALTER COLUMN taille_bytes SET NOT NULL;
