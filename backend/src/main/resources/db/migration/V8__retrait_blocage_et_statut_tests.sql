-- V8__retrait_blocage_et_statut_tests.sql
-- Simplification décidée avec l'atelier : le "blocage" faisait doublon avec le statut ATTENTE_CLIENT,
-- et le statut TESTS est fusionné dans REPARATION.

-- La raison d'un blocage en cours est gardée sous forme de note pour ne rien perdre
INSERT INTO note (dossier_id, contenu, sensible, created_at)
SELECT id, 'Ancien blocage : ' || coalesce(raison_blocage, 'sans raison indiquée'), FALSE, NOW()
FROM dossier
WHERE bloque;

ALTER TABLE dossier
  DROP COLUMN bloque,
  DROP COLUMN raison_blocage;

UPDATE dossier SET statut = 'REPARATION' WHERE statut = 'TESTS';
