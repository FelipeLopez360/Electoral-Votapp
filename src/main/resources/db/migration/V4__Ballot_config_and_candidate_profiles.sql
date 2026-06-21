-- ============================================================
-- Electoral-Votapp — Ballot Config & Candidate Rich Profiles
-- V4__Ballot_config_and_candidate_profiles.sql
-- ============================================================
-- Design decisions:
--   • Additive forward migration — existing elections get safe defaults
--     (permite_voto_blanco=true, max_votos_por_elector=1 → behavior unchanged)
--   • Rich candidate fields are NULLable TEXT — synthetic candidates use NULL
--   • Dropping votos.token_id UNIQUE (PostgreSQL auto-name votos_token_id_key)
--     allows N rows per token for multi-candidate voting
--   • Replacing the unique constraint with a plain index preserves query perf
--   • No data backfill — only structural changes
-- ============================================================

-- ─── ELECCIONES: ballot configuration ────────────────────────────────────────

ALTER TABLE elecciones
    ADD COLUMN permite_voto_blanco BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE elecciones
    ADD COLUMN max_votos_por_elector INTEGER NOT NULL DEFAULT 1
        CONSTRAINT chk_elecciones_max_votos CHECK (max_votos_por_elector >= 1);

-- ─── CANDIDATOS: rich profile fields (all NULLable — synthetic candidates use NULL) ──

ALTER TABLE candidatos
    ADD COLUMN foto_url TEXT;

ALTER TABLE candidatos
    ADD COLUMN biografia TEXT;

ALTER TABLE candidatos
    ADD COLUMN propuestas TEXT;

ALTER TABLE candidatos
    ADD COLUMN afiliacion_politica TEXT;

-- ─── VOTOS: drop UNIQUE constraint on token_id ───────────────────────────────
-- Allows one token to own N rows (multi-candidate voting).
-- Atomicity is guaranteed by Redis SETNX lock + token USED state.

ALTER TABLE votos
    DROP CONSTRAINT votos_token_id_key;

-- Maintain query performance with a plain (non-unique) index
CREATE INDEX idx_votos_token ON votos(token_id);
