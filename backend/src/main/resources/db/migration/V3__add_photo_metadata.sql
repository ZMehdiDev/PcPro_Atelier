-- V3__add_photo_metadata.sql
-- Ajout des colonnes mime_type et taille_bytes pour le stockage réel des photos

ALTER TABLE photo 
  ADD COLUMN mime_type VARCHAR(100),
  ADD COLUMN taille_bytes BIGINT;

-- Mettre à jour les photos existantes avec des valeurs par défaut si nécessaire
UPDATE photo 
SET mime_type = 'image/jpeg', taille_bytes = 0 
WHERE mime_type IS NULL;
