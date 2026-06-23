-- ============================================================
-- Electoral-Votapp — Candidate Funcionario Link & Ordering
-- V5__candidate_funcionario_and_ordering.sql
-- ============================================================
-- Design decisions:
--   • Drop numero_orden: ordering is now alphabetical by nombre (Java layer)
--   • Drop afiliacion_politica: internal election, affiliation irrelevant
--   • Drop old UNIQUE(eleccion_id, numero_orden) — was inline in V1 definition
--   • Add funcionario_id INTEGER FK → funcionarios(id), nullable
--     (synthetic blank/null vote candidates have NULL funcionario_id)
--   • Partial unique index WHERE funcionario_id IS NOT NULL:
--     one funcionario can be candidate only once per election
--     NULL excluded → many synthetics allowed in same election
-- ============================================================

-- ─── DROP legacy ordering/affiliation columns ─────────────────────────────────

ALTER TABLE candidatos
    DROP COLUMN numero_orden;

ALTER TABLE candidatos
    DROP COLUMN afiliacion_politica;

-- ─── ADD funcionario_id with FK ──────────────────────────────────────────────

ALTER TABLE candidatos
    ADD COLUMN funcionario_id INTEGER REFERENCES funcionarios(id);

-- ─── PARTIAL unique index: one funcionario per election (NULLs excluded) ──────

CREATE UNIQUE INDEX uq_candidatos_eleccion_funcionario
    ON candidatos (eleccion_id, funcionario_id)
    WHERE funcionario_id IS NOT NULL;
