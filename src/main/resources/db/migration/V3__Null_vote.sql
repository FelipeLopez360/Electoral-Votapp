-- ============================================================
-- Electoral-Votapp — Null Vote Support
-- V3__Null_vote.sql
-- ============================================================
-- Design principles:
--   • Additive only — existing rows unaffected (DEFAULT false)
--   • Mirrors es_voto_en_blanco pattern from V1
--   • No backfill: finalized elections without null candidate report nullVotes=0
--   • CHECK constraint relaxed to allow synthetic blank AND null vote names
-- ============================================================

-- Add null-vote flag to candidatos (additive, backward-compatible)
ALTER TABLE candidatos
    ADD COLUMN es_voto_nulo BOOLEAN NOT NULL DEFAULT false;

-- Drop the old constraint that only allowed 'Voto en Blanco' as special name
ALTER TABLE candidatos
    DROP CONSTRAINT chk_candidatos_voto_en_blanco;

-- New constraint: if blank → must be named 'Voto en Blanco'; if null → must be named 'Voto Nulo'
ALTER TABLE candidatos
    ADD CONSTRAINT chk_candidatos_special_votes
        CHECK (
            (es_voto_en_blanco = false OR nombre = 'Voto en Blanco')
            AND (es_voto_nulo = false OR nombre = 'Voto Nulo')
        );

-- A candidato cannot be both blank and null simultaneously
ALTER TABLE candidatos
    ADD CONSTRAINT chk_candidatos_not_both_special
        CHECK (NOT (es_voto_en_blanco = true AND es_voto_nulo = true));
