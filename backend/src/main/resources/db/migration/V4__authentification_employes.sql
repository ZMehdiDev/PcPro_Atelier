-- V4__authentification_employes.sql
-- Connexion des employés : login, mot de passe (bcrypt), rôle, révocation des tokens.

ALTER TABLE employe
  ADD COLUMN login VARCHAR(50),
  ADD COLUMN password_hash VARCHAR(100),
  ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'TECHNICIEN',
  ADD COLUMN credentials_changed_at TIMESTAMPTZ;

-- Les employés existants reçoivent leur prénom en minuscules comme login (ex : "zak").
-- Ils n'ont pas de mot de passe : un admin le leur définit (ou APP_BOOTSTRAP_ADMIN_* au démarrage).
UPDATE employe SET login = lower(prenom) || CASE
    WHEN (SELECT count(*) FROM employe e2 WHERE lower(e2.prenom) = lower(employe.prenom)) > 1 THEN '-' || id
    ELSE ''
  END;

ALTER TABLE employe ALTER COLUMN login SET NOT NULL;
ALTER TABLE employe ADD CONSTRAINT uq_employe_login UNIQUE (login);
ALTER TABLE employe ADD CONSTRAINT ck_employe_role CHECK (role IN ('ADMIN', 'TECHNICIEN'));
