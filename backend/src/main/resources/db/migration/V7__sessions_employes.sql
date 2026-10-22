-- V7__sessions_employes.sql
-- Sessions longues ("rester connecté") : un refresh token par appareil, stocké sous forme d'empreinte SHA-256.

CREATE TABLE session_employe (
  id BIGSERIAL PRIMARY KEY,
  employe_id BIGINT NOT NULL REFERENCES employe(id) ON DELETE CASCADE,
  token_hash VARCHAR(64) NOT NULL,
  user_agent VARCHAR(255),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ,
  -- true quand la session a été remplacée par un refresh (rotation), et non fermée (logout, mot de passe changé)
  remplacee BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT uq_session_employe_token UNIQUE (token_hash)
);

CREATE INDEX idx_session_employe_employe ON session_employe(employe_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_session_employe_expires ON session_employe(expires_at);
