-- ============================================================
-- Electoral-Votapp — Censo Electoral Schema
-- V2__Censo_electoral.sql
-- ============================================================
-- Design principles:
--   • census ownership: electoral context
--   • UNIQUE(eleccion_id, funcionario_id) enforces idempotent membership
--   • ON DELETE CASCADE from elecciones propagates cleanly
--   • funcionario_id is INTEGER (SERIAL FK) matching V1 funcionarios.id
--   • agregado_por stores the admin's funcionario_id (nullable for MVP)
-- ============================================================

CREATE TABLE censo_electoral (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    eleccion_id    UUID    NOT NULL REFERENCES elecciones(id) ON DELETE CASCADE,
    funcionario_id INTEGER NOT NULL REFERENCES funcionarios(id),
    agregado_por   INTEGER REFERENCES funcionarios(id),
    created_at     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (eleccion_id, funcionario_id)
);

CREATE INDEX idx_censo_eleccion     ON censo_electoral(eleccion_id);
CREATE INDEX idx_censo_funcionario  ON censo_electoral(funcionario_id);
