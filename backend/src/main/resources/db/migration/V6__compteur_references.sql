-- V6__compteur_references.sql
-- Compteur annuel des références de dossier (PCP-YYYY-NNNNNN), incrémenté atomiquement.

CREATE TABLE reference_compteur (
  annee INT PRIMARY KEY,
  dernier INT NOT NULL
);

-- Reprend le plus grand numéro déjà attribué pour chaque année
INSERT INTO reference_compteur (annee, dernier)
SELECT CAST(substring(reference FROM 5 FOR 4) AS INT),
       max(CAST(substring(reference FROM 10) AS INT))
FROM dossier
WHERE reference ~ '^PCP-[0-9]{4}-[0-9]{6}$'
GROUP BY substring(reference FROM 5 FOR 4);
