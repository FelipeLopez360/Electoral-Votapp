-- ============================================================
-- Electoral-Votapp — MVP Base Schema
-- V1__Initial_schema.sql
-- ============================================================
-- Design principles:
--   • Logic lives in Java domain (no business PL/pgSQL functions)
--   • DB enforces: FK, UNIQUE, CHECK, indexes, UUID extension
--   • SHA-256 token hash only — raw token never persisted
--   • votos has no funcionario_id (anonymity by design)
--   • participacion_electoral is a separate materialised table
-- ============================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- ORGANISATIONAL STRUCTURE
-- ============================================================

CREATE TABLE departamentos (
    id   SERIAL PRIMARY KEY,
    codigo  VARCHAR(20) NOT NULL UNIQUE,
    nombre  VARCHAR(100) NOT NULL,
    descripcion TEXT,
    activo  BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cargos (
    id   SERIAL PRIMARY KEY,
    codigo  VARCHAR(20) NOT NULL UNIQUE,
    nombre  VARCHAR(100) NOT NULL,
    nivel_jerarquico INTEGER DEFAULT 1,
    descripcion TEXT,
    activo  BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- FUNCIONARIOS
-- ============================================================

CREATE TABLE funcionarios (
    id   SERIAL PRIMARY KEY,
    numero_empleado    VARCHAR(20) NOT NULL UNIQUE,
    documento_identidad VARCHAR(30) NOT NULL UNIQUE,
    tipo_documento     VARCHAR(10) DEFAULT 'CC',
    nombres  VARCHAR(100) NOT NULL,
    apellidos  VARCHAR(100) NOT NULL,
    email   VARCHAR(100) UNIQUE,
    telefono  VARCHAR(20),
    departamento_id    INTEGER REFERENCES departamentos(id),
    cargo_id  INTEGER REFERENCES cargos(id),
    fecha_ingreso DATE,
    estado_laboral     VARCHAR(20) DEFAULT 'ACTIVO'
        CHECK (estado_laboral IN ('ACTIVO','INACTIVO','SUSPENDIDO','RETIRADO')),
    password_hash      VARCHAR(255) NOT NULL,
    puede_votar BOOLEAN DEFAULT true,
    ultimo_acceso      TIMESTAMP,
    intentos_fallidos  INTEGER DEFAULT 0,
    bloqueado_hasta    TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_funcionarios_documento ON funcionarios(documento_identidad);
CREATE INDEX idx_funcionarios_email     ON funcionarios(email);
CREATE INDEX idx_funcionarios_estado    ON funcionarios(estado_laboral);

-- ============================================================
-- ELECCIONES
-- ============================================================

CREATE TABLE elecciones (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    codigo  VARCHAR(50) NOT NULL UNIQUE,
    nombre  VARCHAR(200) NOT NULL,
    descripcion TEXT,
    estado  VARCHAR(20) NOT NULL DEFAULT 'PROGRAMADA'
        CHECK (estado IN ('PROGRAMADA','ACTIVA','FINALIZADA','CANCELADA','SUSPENDIDA')),
    fecha_inicio TIMESTAMPTZ NOT NULL,
    fecha_fin    TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_elecciones_fechas CHECK (fecha_fin > fecha_inicio)
);

CREATE INDEX idx_elecciones_estado  ON elecciones(estado);
CREATE INDEX idx_elecciones_fechas  ON elecciones(fecha_inicio, fecha_fin);

-- ============================================================
-- CANDIDATOS
-- ============================================================

CREATE TABLE candidatos (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    eleccion_id  UUID NOT NULL REFERENCES elecciones(id) ON DELETE CASCADE,
    nombre  VARCHAR(200) NOT NULL,
    descripcion TEXT,
    es_voto_en_blanco BOOLEAN NOT NULL DEFAULT false,
    numero_orden INTEGER NOT NULL,
    created_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (eleccion_id, numero_orden),
    CONSTRAINT chk_candidatos_voto_en_blanco
        CHECK (es_voto_en_blanco = false OR nombre = 'Voto en Blanco')
);

CREATE INDEX idx_candidatos_eleccion ON candidatos(eleccion_id);

-- ============================================================
-- TOKENS DE VOTACIÓN
-- ============================================================

CREATE TABLE tokens_votacion (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    eleccion_id  UUID NOT NULL REFERENCES elecciones(id),
    funcionario_id     INTEGER NOT NULL REFERENCES funcionarios(id),
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    status  VARCHAR(20) NOT NULL DEFAULT 'ISSUED'
        CHECK (status IN ('ISSUED','USED','INVALIDATED')),
    issued_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at  TIMESTAMPTZ,
    used_ip  VARCHAR(45),
    user_agent  TEXT,
    created_by_admin_id INTEGER,
    created_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Partial unique index: only one ISSUED token per funcionario per election
CREATE UNIQUE INDEX uq_token_issued_funcionario_eleccion
    ON tokens_votacion (eleccion_id, funcionario_id)
    WHERE status = 'ISSUED';

CREATE INDEX idx_tokens_eleccion     ON tokens_votacion(eleccion_id);
CREATE INDEX idx_tokens_funcionario  ON tokens_votacion(funcionario_id);
CREATE INDEX idx_tokens_status       ON tokens_votacion(status);

-- ============================================================
-- VOTOS  (anonymous — no funcionario_id)
-- ============================================================

CREATE TABLE votos (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    eleccion_id  UUID NOT NULL REFERENCES elecciones(id),
    candidato_id UUID NOT NULL REFERENCES candidatos(id),
    token_id     UUID NOT NULL UNIQUE REFERENCES tokens_votacion(id),
    created_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_votos_eleccion   ON votos(eleccion_id);
CREATE INDEX idx_votos_candidato  ON votos(candidato_id);

-- ============================================================
-- PARTICIPACIÓN ELECTORAL  (who voted — not what they voted)
-- ============================================================

CREATE TABLE participacion_electoral (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    eleccion_id  UUID NOT NULL REFERENCES elecciones(id),
    funcionario_id     INTEGER NOT NULL REFERENCES funcionarios(id),
    completado  BOOLEAN NOT NULL DEFAULT false,
    completed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (eleccion_id, funcionario_id)
);

CREATE INDEX idx_participacion_eleccion    ON participacion_electoral(eleccion_id);
CREATE INDEX idx_participacion_funcionario ON participacion_electoral(funcionario_id);

-- ============================================================
-- AUDITORÍA DE EVENTOS
-- ============================================================

CREATE TABLE auditoria_eventos (
    id   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tipo  VARCHAR(30) NOT NULL
        CHECK (tipo IN (
            'TOKEN_ISSUED',
            'VOTE_ACCEPTED',
            'TOKEN_INVALID',
            'VOTE_REJECTED',
            'TOKEN_INVALIDATED'
        )),
    eleccion_id  UUID REFERENCES elecciones(id),
    funcionario_id     INTEGER REFERENCES funcionarios(id),  -- NULL for VOTE_ACCEPTED
    candidato_id UUID REFERENCES candidatos(id),
    metadata     JSONB,
    created_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auditoria_tipo       ON auditoria_eventos(tipo);
CREATE INDEX idx_auditoria_eleccion   ON auditoria_eventos(eleccion_id);
CREATE INDEX idx_auditoria_created_at ON auditoria_eventos(created_at);

-- ============================================================
-- SEED DATA — organisational base + test funcionarios
-- ============================================================

INSERT INTO departamentos (codigo, nombre) VALUES
    ('RRHH',      'Recursos Humanos'),
    ('SISTEMAS',  'Sistemas'),
    ('JURIDICO',  'Jurídico'),
    ('FINANZAS',  'Finanzas'),
    ('DIRECCION', 'Dirección');

INSERT INTO cargos (codigo, nombre, nivel_jerarquico) VALUES
    ('DIR_GEN',   'Director General',    5),
    ('JEFE_DEPT', 'Jefe de Departamento',3),
    ('COORD',     'Coordinador',         2),
    ('FUNC',      'Funcionario',         1);

-- Test funcionarios: ACTIVO + puede_votar = true (required by eligibility spec)
INSERT INTO funcionarios
    (numero_empleado, documento_identidad, nombres, apellidos, email,
     departamento_id, cargo_id, estado_laboral, puede_votar, password_hash)
VALUES
    ('EMP001','10000001','Juan Carlos','Pérez González','juan.perez@votapp.test',
     1,4,'ACTIVO',true,'$2a$12$placeholder_hash_001'),
    ('EMP002','10000002','María Isabel','García López','maria.garcia@votapp.test',
     2,4,'ACTIVO',true,'$2a$12$placeholder_hash_002'),
    ('EMP003','10000003','Carlos Alberto','Rodríguez Mejía','carlos.rodriguez@votapp.test',
     3,3,'ACTIVO',true,'$2a$12$placeholder_hash_003'),
    ('EMP004','10000004','Ana Lucía','Martínez Torres','ana.martinez@votapp.test',
     1,4,'INACTIVO',false,'$2a$12$placeholder_hash_004');
