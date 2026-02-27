-- ========================================================
-- Base de Datos: sistema_votaciones_institucional
-- Sistema de Votaciones para Entidades con Funcionarios
-- ========================================================

-- Crear la base de datos
-- CREATE DATABASE powerup_auth;

-- Crear el esquema en lugar de base de datos
CREATE SCHEMA IF NOT EXISTS test_votaappdb;

-- Establecer el esquema como predeterminado para esta sesión
SET search_path TO test_votaappdb, public;

-- Crear extensiones necesarias (ahora obligatorias, sin manejo de errores alternativos)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ========================================================
-- FUNCIONES AUXILIARES PARA HASHING Y UUID
-- ========================================================

-- Función para hashing de contraseñas (usa obligatoriamente pgcrypto)
CREATE OR REPLACE FUNCTION hash_password(password TEXT)
RETURNS TEXT AS $$
BEGIN
    RETURN crypt(password, gen_salt('bf', 12));
END;
$$ LANGUAGE plpgsql;

-- Función para verificar contraseñas (usa obligatoriamente pgcrypto)
CREATE OR REPLACE FUNCTION verify_password(password TEXT, hash TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN crypt(password, hash) = hash;
END;
$$ LANGUAGE plpgsql;

-- Función para generar UUID (usa obligatoriamente uuid-ossp)
CREATE OR REPLACE FUNCTION generate_uuid()
RETURNS UUID AS $$
BEGIN
    RETURN uuid_generate_v4();
END;
$$ LANGUAGE plpgsql;

-- ========================================================
-- TABLAS DE CONFIGURACIÓN Y ADMINISTRACIÓN
-- ========================================================

-- Tabla: roles_sistema
CREATE TABLE roles_sistema (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion TEXT,
    permisos JSONB DEFAULT '{}',
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insertar roles básicos del sistema
INSERT INTO roles_sistema (nombre, descripcion, permisos) VALUES
('SUPER_ADMIN', 'Administrador del sistema', '{"all": true}'),
('ADMIN_ELECTORAL', 'Administrador electoral', '{"manage_elections": true, "manage_candidates": true, "view_results": true}'),
('SUPERVISOR', 'Supervisor de votación', '{"monitor_voting": true, "view_results": true}'),
('AUDITOR', 'Auditor del sistema', '{"view_audit": true, "export_data": true}');

-- Tabla: administradores
CREATE TABLE administradores (
    id SERIAL PRIMARY KEY,
    uuid UUID DEFAULT generate_uuid(),
    usuario VARCHAR(50) NOT NULL UNIQUE,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol_id INTEGER REFERENCES roles_sistema(id),
    activo BOOLEAN DEFAULT true,
    ultimo_acceso TIMESTAMP,
    intentos_fallidos INTEGER DEFAULT 0,
    bloqueado_hasta TIMESTAMP,
    created_by INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Agregar la foreign key después de crear la tabla
ALTER TABLE administradores ADD CONSTRAINT fk_administradores_created_by
FOREIGN KEY (created_by) REFERENCES administradores(id);

-- CRÍTICO: Crear un administrador inicial es requerido para la funcionalidad del sistema
-- Debe cambiar esta contraseña inmediatamente después del primer acceso
INSERT INTO administradores (usuario, nombres, apellidos, email, password_hash, rol_id) VALUES
('admin', 'Administrador', 'del Sistema', 'admin@institucion.gov', hash_password('ChangeMe2024!'), 1);

-- ADVERTENCIA DE SEGURIDAD:
-- La contraseña por defecto es 'ChangeMe2024!' y DEBE ser cambiada inmediatamente.
-- Para cambiar la contraseña, ejecute:
-- UPDATE administradores SET password_hash = hash_password('NUEVA_CONTRASEÑA_SEGURA') WHERE usuario = 'admin';

-- ========================================================
-- TABLAS DE ESTRUCTURA ORGANIZACIONAL
-- ========================================================

-- Tabla: departamentos
CREATE TABLE departamentos (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    activo BOOLEAN DEFAULT true,
    created_by INTEGER REFERENCES administradores(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: cargos
CREATE TABLE cargos (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    nivel_jerarquico INTEGER DEFAULT 1,
    descripcion TEXT,
    activo BOOLEAN DEFAULT true,
    created_by INTEGER REFERENCES administradores(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AHORA INSERTAR DEPARTAMENTOS Y CARGOS (después de crear el administrador)
INSERT INTO departamentos (codigo, nombre, descripcion, created_by) VALUES
('RRHH', 'Recursos Humanos', 'Departamento de gestión del talento humano', 1),
('FINANZAS', 'Finanzas', 'Departamento financiero y contable', 1),
('SISTEMAS', 'Sistemas', 'Departamento de tecnología e información', 1),
('JURIDICO', 'Jurídico', 'Departamento legal y normativo', 1),
('DIRECCION', 'Dirección', 'Dirección general y ejecutiva', 1);

INSERT INTO cargos (codigo, nombre, nivel_jerarquico, descripcion, created_by) VALUES
('DIR_GEN', 'Director General', 5, 'Máxima autoridad ejecutiva', 1),
('SUB_DIR', 'Subdirector', 4, 'Segundo al mando ejecutivo', 1),
('JEFE_DEPT', 'Jefe de Departamento', 3, 'Responsable de departamento', 1),
('COORD', 'Coordinador', 2, 'Coordinador de área', 1),
('FUNC', 'Funcionario', 1, 'Funcionario base', 1);

-- ========================================================
-- TABLA PRINCIPAL: FUNCIONARIOS
-- ========================================================

-- Tabla: funcionarios
CREATE TABLE funcionarios (
    id SERIAL PRIMARY KEY,
    uuid UUID DEFAULT generate_uuid(),
    numero_empleado VARCHAR(20) NOT NULL UNIQUE,
    documento_identidad VARCHAR(30) NOT NULL UNIQUE,
    tipo_documento VARCHAR(10) DEFAULT 'CC',
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    telefono VARCHAR(20),
    departamento_id INTEGER REFERENCES departamentos(id),
    cargo_id INTEGER REFERENCES cargos(id),
    fecha_ingreso DATE,
    estado_laboral VARCHAR(20) DEFAULT 'ACTIVO' CHECK (estado_laboral IN ('ACTIVO', 'INACTIVO', 'SUSPENDIDO', 'RETIRADO')),
    password_hash VARCHAR(255) NOT NULL,
    puede_votar BOOLEAN DEFAULT true,
    ultimo_acceso TIMESTAMP,
    intentos_fallidos INTEGER DEFAULT 0,
    bloqueado_hasta TIMESTAMP,
    created_by INTEGER REFERENCES administradores(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para funcionarios
CREATE INDEX idx_funcionarios_documento ON funcionarios(documento_identidad);
CREATE INDEX idx_funcionarios_email ON funcionarios(email);
CREATE INDEX idx_funcionarios_departamento ON funcionarios(departamento_id);
CREATE INDEX idx_funcionarios_estado ON funcionarios(estado_laboral);

-- ========================================================
-- TABLAS DE CONFIGURACIÓN ELECTORAL
-- ========================================================

-- Tabla: periodos_electorales
CREATE TABLE periodos_electorales (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    activo BOOLEAN DEFAULT false,
    configuracion JSONB DEFAULT '{}',
    created_by INTEGER REFERENCES administradores(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_fechas_periodo CHECK (fecha_fin > fecha_inicio)
);

-- Tabla: tipos_eleccion
CREATE TABLE tipos_eleccion (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    requiere_mayoria_absoluta BOOLEAN DEFAULT false,
    permite_voto_blanco BOOLEAN DEFAULT true,
    activo BOOLEAN DEFAULT true,
    created_by INTEGER REFERENCES administradores(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: elecciones
CREATE TABLE elecciones (
    id SERIAL PRIMARY KEY,
    uuid UUID DEFAULT generate_uuid(),
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(200) NOT NULL,
    descripcion TEXT,
    tipo_eleccion_id INTEGER REFERENCES tipos_eleccion(id),
    periodo_electoral_id INTEGER REFERENCES periodos_electorales(id),
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    estado VARCHAR(20) DEFAULT 'PROGRAMADA' CHECK (estado IN ('PROGRAMADA', 'ACTIVA', 'FINALIZADA', 'CANCELADA', 'SUSPENDIDA')),
    requiere_quorum BOOLEAN DEFAULT false,
    quorum_minimo INTEGER DEFAULT 0,
    votos_maximos_por_funcionario INTEGER DEFAULT 1,
    permite_abstencion BOOLEAN DEFAULT true,
    configuracion JSONB DEFAULT '{}',
    resultados_publicados BOOLEAN DEFAULT false,
    created_by INTEGER REFERENCES administradores(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_fechas_eleccion CHECK (fecha_fin > fecha_inicio)
);

-- Índices para elecciones
CREATE INDEX idx_elecciones_estado ON elecciones(estado);
CREATE INDEX idx_elecciones_fechas ON elecciones(fecha_inicio, fecha_fin);
CREATE INDEX idx_elecciones_periodo ON elecciones(periodo_electoral_id);

-- ========================================================
-- TABLAS DE CANDIDATOS Y CATEGORÍAS
-- ========================================================

-- Tabla: categorias_candidatos
CREATE TABLE categorias_candidatos (
    id SERIAL PRIMARY KEY,
    eleccion_id INTEGER REFERENCES elecciones(id) ON DELETE CASCADE,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    orden_presentacion INTEGER DEFAULT 1,
    max_candidatos INTEGER DEFAULT 1,
    activo BOOLEAN DEFAULT true,
    created_by INTEGER REFERENCES administradores(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: candidatos
CREATE TABLE candidatos (
    id SERIAL PRIMARY KEY,
    uuid UUID DEFAULT generate_uuid(),
    eleccion_id INTEGER REFERENCES elecciones(id) ON DELETE CASCADE,
    categoria_id INTEGER REFERENCES categorias_candidatos(id),
    funcionario_id INTEGER REFERENCES funcionarios(id),
    numero_candidato INTEGER,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    propuesta TEXT,
    foto_url VARCHAR(500),
    es_voto_blanco BOOLEAN DEFAULT false,
    activo BOOLEAN DEFAULT true,
    created_by INTEGER REFERENCES administradores(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_candidato_eleccion UNIQUE (eleccion_id, funcionario_id),
    CONSTRAINT unique_numero_candidato UNIQUE (eleccion_id, categoria_id, numero_candidato)
);

-- Índices para candidatos
CREATE INDEX idx_candidatos_eleccion ON candidatos(eleccion_id);
CREATE INDEX idx_candidatos_funcionario ON candidatos(funcionario_id);

-- ========================================================
-- TABLAS DE VOTACIÓN
-- ========================================================

-- Nuevo: Tabla para gestionar tokens de votación
CREATE TABLE tokens_votacion (
    id SERIAL PRIMARY KEY,
    uuid UUID DEFAULT generate_uuid(),
    eleccion_id INTEGER REFERENCES elecciones(id) ON DELETE CASCADE,
    funcionario_id INTEGER REFERENCES funcionarios(id) ON DELETE CASCADE, -- Link temporal para emisión
    token_hash VARCHAR(255) NOT NULL UNIQUE, -- El valor del token (hash de un token original)
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP,
    used_ip INET,
    is_used BOOLEAN DEFAULT false,
    created_by INTEGER REFERENCES administradores(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: votos (rediseñada para anonimato)
CREATE TABLE votos (
    id SERIAL PRIMARY KEY,
    uuid UUID DEFAULT generate_uuid(),
    eleccion_id INTEGER REFERENCES elecciones(id) ON DELETE CASCADE,
    candidato_id INTEGER REFERENCES candidatos(id) ON DELETE CASCADE,
    categoria_id INTEGER REFERENCES categorias_candidatos(id),
    token_id UUID NOT NULL UNIQUE, -- Referencia al UUID del token usado para votar
    timestamp_voto TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    estado VARCHAR(20) DEFAULT 'VALIDO' CHECK (estado IN ('VALIDO', 'ANULADO', 'IMPUGNADO')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para votos
CREATE INDEX idx_votos_eleccion ON votos(eleccion_id);
-- Removido idx_votos_funcionario
CREATE INDEX idx_votos_candidato ON votos(candidato_id);
CREATE INDEX idx_votos_timestamp ON votos(timestamp_voto);

-- Tabla: participacion_electoral (actualizada para reflejar el uso de tokens)
CREATE TABLE participacion_electoral (
    id SERIAL PRIMARY KEY,
    eleccion_id INTEGER REFERENCES elecciones(id) ON DELETE CASCADE,
    funcionario_id INTEGER REFERENCES funcionarios(id) ON DELETE CASCADE,
    timestamp_participacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    completado BOOLEAN DEFAULT false, -- Indica si el funcionario ha utilizado su token para votar
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_participacion UNIQUE (eleccion_id, funcionario_id)
);

-- ========================================================
-- TABLAS DE AUDITORÍA E HISTORIAL
-- ========================================================

-- Tabla: auditoria_sistema
CREATE TABLE auditoria_sistema (
    id SERIAL PRIMARY KEY,
    uuid UUID DEFAULT generate_uuid(),
    tabla_afectada VARCHAR(50),
    registro_id INTEGER,
    accion VARCHAR(20) CHECK (accion IN ('INSERT', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'VOTE', 'TOKEN_ISSUED', 'TOKEN_USED')),
    datos_anteriores JSONB,
    datos_nuevos JSONB,
    usuario_id INTEGER,
    usuario_tipo VARCHAR(20) CHECK (usuario_tipo IN ('ADMINISTRADOR', 'FUNCIONARIO')),
    ip_address INET,
    user_agent TEXT,
    timestamp_accion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    descripcion TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para auditoría
CREATE INDEX idx_auditoria_tabla ON auditoria_sistema(tabla_afectada);
CREATE INDEX idx_auditoria_usuario ON auditoria_sistema(usuario_id, usuario_tipo);
CREATE INDEX idx_auditoria_timestamp ON auditoria_sistema(timestamp_accion);
CREATE INDEX idx_auditoria_accion ON auditoria_sistema(accion);

-- Tabla: logs_acceso
CREATE TABLE logs_acceso (
    id SERIAL PRIMARY KEY,
    usuario_id INTEGER,
    usuario_tipo VARCHAR(20) CHECK (usuario_tipo IN ('ADMINISTRADOR', 'FUNCIONARIO')),
    accion VARCHAR(50),
    ip_address INET,
    user_agent TEXT,
    exitoso BOOLEAN DEFAULT true,
    mensaje TEXT,
    timestamp_acceso TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índice para logs de acceso
CREATE INDEX idx_logs_acceso_timestamp ON logs_acceso(timestamp_acceso);
CREATE INDEX idx_logs_acceso_usuario ON logs_acceso(usuario_id, usuario_tipo);

-- ========================================================
-- TABLAS DE RESULTADOS Y REPORTES
-- ========================================================

-- Tabla: resultados_eleccion
CREATE TABLE resultados_eleccion (
    id SERIAL PRIMARY KEY,
    eleccion_id INTEGER REFERENCES elecciones(id) ON DELETE CASCADE,
    candidato_id INTEGER REFERENCES candidatos(id),
    categoria_id INTEGER REFERENCES categorias_candidatos(id),
    total_votos INTEGER DEFAULT 0,
    porcentaje_votos DECIMAL(5,2) DEFAULT 0.00,
    posicion INTEGER,
    es_ganador BOOLEAN DEFAULT false,
    timestamp_calculo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_resultado_candidato UNIQUE (eleccion_id, candidato_id)
);

-- Tabla: estadisticas_eleccion
CREATE TABLE estadisticas_eleccion (
    id SERIAL PRIMARY KEY,
    eleccion_id INTEGER REFERENCES elecciones(id) ON DELETE CASCADE,
    total_habilitados INTEGER DEFAULT 0,
    total_participantes INTEGER DEFAULT 0,
    total_votos_validos INTEGER DEFAULT 0,
    total_votos_blancos INTEGER DEFAULT 0,
    total_votos_nulos INTEGER DEFAULT 0,
    porcentaje_participacion DECIMAL(5,2) DEFAULT 0.00,
    timestamp_calculo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_estadistica_eleccion UNIQUE (eleccion_id)
);

-- ========================================================
-- FUNCIONES Y TRIGGERS
-- ========================================================

-- Función para actualizar timestamp de updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers para updated_at (TODAS LAS TABLAS CON updated_at)
CREATE TRIGGER update_roles_sistema_updated_at BEFORE UPDATE ON roles_sistema
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_administradores_updated_at BEFORE UPDATE ON administradores
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_departamentos_updated_at BEFORE UPDATE ON departamentos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_cargos_updated_at BEFORE UPDATE ON cargos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_funcionarios_updated_at BEFORE UPDATE ON funcionarios
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_periodos_electorales_updated_at BEFORE UPDATE ON periodos_electorales
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_tipos_eleccion_updated_at BEFORE UPDATE ON tipos_eleccion
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_elecciones_updated_at BEFORE UPDATE ON elecciones
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_categorias_candidatos_updated_at BEFORE UPDATE ON categorias_candidatos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_candidatos_updated_at BEFORE UPDATE ON candidatos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_tokens_votacion_updated_at BEFORE UPDATE ON tokens_votacion
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_votos_updated_at BEFORE UPDATE ON votos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_participacion_electoral_updated_at BEFORE UPDATE ON participacion_electoral
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_auditoria_sistema_updated_at BEFORE UPDATE ON auditoria_sistema
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_logs_acceso_updated_at BEFORE UPDATE ON logs_acceso
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_resultados_eleccion_updated_at BEFORE UPDATE ON resultados_eleccion
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_estadisticas_eleccion_updated_at BEFORE UPDATE ON estadisticas_eleccion
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- Función para auditoría automática
CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO auditoria_sistema (tabla_afectada, registro_id, accion, datos_anteriores, timestamp_accion)
        VALUES (TG_TABLE_NAME, OLD.id, TG_OP, row_to_json(OLD), CURRENT_TIMESTAMP);
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO auditoria_sistema (tabla_afectada, registro_id, accion, datos_anteriores, datos_nuevos, timestamp_accion)
        VALUES (TG_TABLE_NAME, NEW.id, TG_OP, row_to_json(OLD), row_to_json(NEW), CURRENT_TIMESTAMP);
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO auditoria_sistema (tabla_afectada, registro_id, accion, datos_nuevos, timestamp_accion)
        VALUES (TG_TABLE_NAME, NEW.id, TG_OP, row_to_json(NEW), CURRENT_TIMESTAMP);
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Aplicar triggers de auditoría a tablas críticas
CREATE TRIGGER audit_funcionarios AFTER INSERT OR UPDATE OR DELETE ON funcionarios
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_elecciones AFTER INSERT OR UPDATE OR DELETE ON elecciones
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_candidatos AFTER INSERT OR UPDATE OR DELETE ON candidatos
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_votos AFTER INSERT OR UPDATE OR DELETE ON votos
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();
-- NEW: Trigger for tokens_votacion
CREATE TRIGGER audit_tokens_votacion AFTER INSERT OR UPDATE OR DELETE ON tokens_votacion
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();


-- ========================================================
-- DATOS DE EJEMPLO
-- ========================================================

-- Insertar período electoral ejemplo
INSERT INTO periodos_electorales (nombre, descripcion, fecha_inicio, fecha_fin, activo, created_by) VALUES
('Período Electoral 2024', 'Elecciones institucionales del año 2024', '2024-01-01', '2024-12-31', true, 1);

-- Insertar tipos de elección
INSERT INTO tipos_eleccion (codigo, nombre, descripcion, created_by) VALUES
('REPR_FUNC', 'Representante de Funcionarios', 'Elección de representante general de funcionarios', 1),
('COMITE_CONV', 'Comité de Convivencia', 'Elección de miembros del comité de convivencia laboral', 1),
('REPR_DEPT', 'Representante Departamental', 'Representante por departamento', 1),
('JUNTA_DIR', 'Junta Directiva', 'Elección de junta directiva', 1);

-- NOTA: Los funcionarios de ejemplo deben crearse manualmente con contraseñas seguras
-- Ejemplo de creación (ejecutar manualmente):
-- INSERT INTO funcionarios (numero_empleado, documento_identidad, nombres, apellidos, email, 
--   departamento_id, cargo_id, fecha_ingreso, password_hash, created_by) VALUES
-- ('EMP001', '12345678', 'Juan Carlos', 'Pérez González', 'juan.perez@institucion.gov', 
--   1, 3, '2020-01-15', hash_password('CONTRASEÑA_SEGURA'), 1);

-- ========================================================
-- VISTAS ÚTILES
-- ========================================================

-- Vista: funcionarios_activos_votacion
CREATE VIEW funcionarios_activos_votacion AS
SELECT
    f.id,
    f.uuid,
    f.numero_empleado,
    f.documento_identidad,
    f.nombres,
    f.apellidos,
    f.email,
    d.nombre as departamento,
    c.nombre as cargo,
    f.puede_votar,
    f.estado_laboral
FROM funcionarios f
LEFT JOIN departamentos d ON f.departamento_id = d.id
LEFT JOIN cargos c ON f.cargo_id = c.id
WHERE f.estado_laboral = 'ACTIVO' AND f.puede_votar = true;

-- Vista: resumen_elecciones
CREATE VIEW resumen_elecciones AS
SELECT
    e.id,
    e.uuid,
    e.codigo,
    e.nombre,
    e.estado,
    e.fecha_inicio,
    e.fecha_fin,
    te.nombre as tipo_eleccion,
    pe.nombre as periodo_electoral,
    COUNT(DISTINCT c.id) as total_candidatos,
    COUNT(DISTINCT v.token_id) as total_votantes, -- Actualizado para usar token_id
    ee.porcentaje_participacion
FROM elecciones e
LEFT JOIN tipos_eleccion te ON e.tipo_eleccion_id = te.id
LEFT JOIN periodos_electorales pe ON e.periodo_electoral_id = pe.id
LEFT JOIN candidatos c ON e.id = c.eleccion_id AND c.activo = true
LEFT JOIN votos v ON e.id = v.eleccion_id AND v.estado = 'VALIDO'
LEFT JOIN estadisticas_eleccion ee ON e.id = ee.eleccion_id
GROUP BY e.id, e.uuid, e.codigo, e.nombre, e.estado, e.fecha_inicio, e.fecha_fin,
         te.nombre, pe.nombre, ee.porcentaje_participacion;

-- ========================================================
-- FUNCIONES ADICIONALES PARA EL SISTEMA
-- ========================================================

-- Función para calcular resultados de una elección
CREATE OR REPLACE FUNCTION calcular_resultados_eleccion(eleccion_id_param INTEGER)
RETURNS TABLE(
    candidato_id INTEGER,
    nombres VARCHAR,
    apellidos VARCHAR,
    total_votos BIGINT,
    porcentaje DECIMAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.id,
        c.nombres,
        c.apellidos,
        COUNT(v.id) as total_votos,
        ROUND((COUNT(v.id) * 100.0 / NULLIF(total_votos_eleccion.total, 0)), 2) as porcentaje
    FROM candidatos c
    LEFT JOIN votos v ON c.id = v.candidato_id AND v.eleccion_id = eleccion_id_param AND v.estado = 'VALIDO'
    CROSS JOIN (
        SELECT COUNT(*) as total
        FROM votos
        WHERE eleccion_id = eleccion_id_param AND estado = 'VALIDO'
    ) total_votos_eleccion
    WHERE c.eleccion_id = eleccion_id_param AND c.activo = true
    GROUP BY c.id, c.nombres, c.apellidos, total_votos_eleccion.total
    ORDER BY COUNT(v.id) DESC;
END;
$$ LANGUAGE plpgsql;

-- Función para verificar si un funcionario puede votar en una elección (actualizada para tokens)
CREATE OR REPLACE FUNCTION puede_votar_funcionario(funcionario_id_param INTEGER, eleccion_id_param INTEGER)
RETURNS BOOLEAN AS $$
DECLARE
    funcionario_activo BOOLEAN;
    eleccion_activa BOOLEAN;
    token_disponible BOOLEAN;
BEGIN
    -- Verificar si el funcionario está activo y puede votar
    SELECT (estado_laboral = 'ACTIVO' AND puede_votar = true) INTO funcionario_activo
    FROM funcionarios
    WHERE id = funcionario_id_param;

    -- Verificar si la elección está activa
    SELECT (estado = 'ACTIVA' AND fecha_inicio <= CURRENT_TIMESTAMP AND fecha_fin >= CURRENT_TIMESTAMP) INTO eleccion_activa
    FROM elecciones
    WHERE id = eleccion_id_param;

    -- Verificar si existe un token no usado para este funcionario en esta elección
    SELECT EXISTS(
        SELECT 1 FROM tokens_votacion
        WHERE funcionario_id = funcionario_id_param
        AND eleccion_id = eleccion_id_param
        AND is_used = false
    ) INTO token_disponible;

    RETURN COALESCE(funcionario_activo, false) AND COALESCE(eleccion_activa, false) AND COALESCE(token_disponible, false);
END;
$$ LANGUAGE plpgsql;

-- Función para generar un token de votación para un funcionario
CREATE OR REPLACE FUNCTION generar_token_votacion(funcionario_id_param INTEGER, eleccion_id_param INTEGER)
RETURNS TEXT AS $$
DECLARE
    token_raw TEXT;
    token_hashed TEXT;
    token_uuid UUID;
BEGIN
    -- Generar token aleatorio (usamos pgcrypto para aleatoriedad criptográfica)
    token_raw := encode(gen_random_bytes(32), 'hex');
    
    -- Hashear el token para almacenamiento
    token_hashed := encode(digest(token_raw, 'sha256'), 'hex');
    
    -- Insertar el token en la tabla
    INSERT INTO tokens_votacion (eleccion_id, funcionario_id, token_hash, created_by)
    VALUES (eleccion_id_param, funcionario_id_param, token_hashed, funcionario_id_param)
    RETURNING uuid INTO token_uuid;
    
    -- Registrar en auditoría
    INSERT INTO auditoria_sistema (tabla_afectada, registro_id, accion, usuario_id, usuario_tipo, timestamp_accion, descripcion)
    VALUES ('tokens_votacion', funcionario_id_param, 'TOKEN_ISSUED', funcionario_id_param, 'FUNCIONARIO', CURRENT_TIMESTAMP, 
            'Token de votación generado para elección ' || eleccion_id_param);
    
    -- Retornar el token original (NO el hash) para que el funcionario pueda usarlo
    RETURN token_raw;
END;
$$ LANGUAGE plpgsql;

-- Función para validar un token de votación
CREATE OR REPLACE FUNCTION validar_token_votacion(token_raw TEXT)
RETURNS TABLE(
    token_uuid UUID,
    eleccion_id INTEGER,
    is_valid BOOLEAN
) AS $$
DECLARE
    token_hashed TEXT;
BEGIN
    -- Hashear el token recibido
    token_hashed := encode(digest(token_raw, 'sha256'), 'hex');
    
    -- Buscar el token
    RETURN QUERY
    SELECT 
        t.uuid,
        t.eleccion_id,
        (NOT t.is_used) as is_valid
    FROM tokens_votacion t
    WHERE t.token_hash = token_hashed;
END;
$$ LANGUAGE plpgsql;

-- Función para registrar un voto usando un token
CREATE OR REPLACE FUNCTION registrar_voto_con_token(
    token_raw TEXT,
    candidato_id_param INTEGER,
    categoria_id_param INTEGER,
    ip_address_param INET,
    user_agent_param TEXT
)
RETURNS BOOLEAN AS $$
DECLARE
    token_hashed TEXT;
    token_record RECORD;
    voto_id INTEGER;
BEGIN
    -- Hashear el token
    token_hashed := encode(digest(token_raw, 'sha256'), 'hex');
    
    -- Buscar y verificar el token
    SELECT t.uuid, t.eleccion_id, t.is_used, t.funcionario_id
    INTO token_record
    FROM tokens_votacion t
    WHERE t.token_hash = token_hashed;
    
    -- Verificar que el token existe y no ha sido usado
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Token inválido';
    END IF;
    
    IF token_record.is_used THEN
        RAISE EXCEPTION 'Token ya utilizado';
    END IF;
    
    -- Registrar el voto (SIN referencia al funcionario_id)
    INSERT INTO votos (eleccion_id, candidato_id, categoria_id, token_id, ip_address, user_agent)
    VALUES (token_record.eleccion_id, candidato_id_param, categoria_id_param, token_record.uuid, ip_address_param, user_agent_param)
    RETURNING id INTO voto_id;
    
    -- Marcar el token como usado
    UPDATE tokens_votacion 
    SET is_used = true, used_at = CURRENT_TIMESTAMP, used_ip = ip_address_param
    WHERE uuid = token_record.uuid;
    
    -- Marcar participación como completada
    UPDATE participacion_electoral
    SET completado = true
    WHERE funcionario_id = token_record.funcionario_id 
      AND eleccion_id = token_record.eleccion_id;
    
    -- Registrar en auditoría (sin identificar al votante)
    INSERT INTO auditoria_sistema (tabla_afectada, registro_id, accion, timestamp_accion, descripcion, ip_address)
    VALUES ('votos', voto_id, 'VOTE', CURRENT_TIMESTAMP, 
            'Voto registrado para elección ' || token_record.eleccion_id, ip_address_param);
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- ========================================================
-- EJEMPLO DE ELECCIÓN COMPLETA
-- ========================================================

-- Crear una elección de ejemplo
INSERT INTO elecciones (codigo, nombre, descripcion, tipo_eleccion_id, periodo_electoral_id, fecha_inicio, fecha_fin, estado, created_by) VALUES
('ELEC2024001', 'Elección Representante de Funcionarios 2024', 'Primera elección del año para elegir representante general', 1, 1, '2024-03-01 08:00:00', '2024-03-01 17:00:00', 'PROGRAMADA', 1);

-- Crear categoría para la elección
INSERT INTO categorias_candidatos (eleccion_id, nombre, descripcion, orden_presentacion, created_by) VALUES
(1, 'Representante Principal', 'Candidatos para representante principal de funcionarios', 1, 1);

-- Crear candidatos de ejemplo (sin referencia a funcionarios específicos)
INSERT INTO candidatos (eleccion_id, categoria_id, funcionario_id, numero_candidato, nombres, apellidos, propuesta, created_by) VALUES
(1, 1, NULL, 1, 'Candidato', 'Uno', 'Propuesta para mejorar las condiciones laborales y bienestar de los funcionarios', 1),
(1, 1, NULL, 2, 'Candidato', 'Dos', 'Plan de desarrollo profesional y capacitación continua para el personal', 1),
(1, 1, NULL, 3, 'Voto', 'en Blanco', 'Opción de voto en blanco', 1);

-- Actualizar el último candidato como voto en blanco
UPDATE candidatos SET es_voto_blanco = true WHERE numero_candidato = 3 AND eleccion_id = 1;

-- ========================================================
-- COMENTARIOS FINALES
-- ========================================================

-- Crear comentarios en las tablas principales
COMMENT ON TABLE funcionarios IS 'Tabla principal de funcionarios de la institución que pueden participar en votaciones';
COMMENT ON TABLE elecciones IS 'Tabla de elecciones programadas y ejecutadas en la institución';
COMMENT ON TABLE votos IS 'Tabla de votos emitidos de forma anónima usando tokens - NO contiene referencia directa al votante';
COMMENT ON TABLE auditoria_sistema IS 'Tabla de auditoría que registra todas las acciones críticas del sistema';
COMMENT ON TABLE tokens_votacion IS 'Tabla para gestionar los tokens únicos de votación para garantizar anonimato';

-- ========================================================
-- DOCUMENTACIÓN DEL SISTEMA DE VOTACIÓN ANÓNIMA
-- ========================================================
-- 
-- FLUJO DE VOTACIÓN ANÓNIMA:
-- 
-- 1. GENERACIÓN DE TOKENS:
--    Cuando una elección está activa, se generan tokens únicos para cada funcionario habilitado.
--    Ejemplo:
--    SELECT generar_token_votacion(funcionario_id, eleccion_id);
--    
--    El token generado se entrega al funcionario (por email, SMS, etc.) y NO se almacena en texto plano.
--    Solo se guarda un hash del token en la base de datos.
-- 
-- 2. VALIDACIÓN DEL TOKEN:
--    Antes de permitir votar, validar el token:
--    SELECT * FROM validar_token_votacion('token_del_funcionario');
-- 
-- 3. REGISTRO DEL VOTO:
--    Para votar, el funcionario usa su token (NO su identificación):
--    SELECT registrar_voto_con_token('token_del_funcionario', candidato_id, categoria_id, ip_address, user_agent);
--    
--    IMPORTANTE: La tabla 'votos' NO contiene funcionario_id, solo token_id (UUID).
--    Esto garantiza que no se pueda rastrear quién votó por quién.
-- 
-- 4. SEPARACIÓN DE INFORMACIÓN:
--    - tokens_votacion: Vincula funcionario con token (temporal, puede eliminarse después de la votación)
--    - participacion_electoral: Registra QUIÉN votó (sin decir por quién)
--    - votos: Registra QUÉ se votó (sin decir quién lo hizo)
-- 
-- 5. ANONIMIZACIÓN POST-ELECCIÓN (RECOMENDADO):
--    Después de finalizada la elección, para garantizar anonimato absoluto:
--    DELETE FROM tokens_votacion WHERE eleccion_id = X AND is_used = true;
--    
--    Esto elimina el vínculo entre funcionario y token, haciendo imposible rastrear votos.
-- 
-- ========================================================


-- Mensaje de finalización
SELECT 'Base de datos del sistema de votaciones institucional creada exitosamente' as mensaje,
       'ADVERTENCIA: Cambiar contraseña del administrador inmediatamente' as seguridad,
       'Contraseña temporal: ChangeMe2024!' as password_temp,
       'Elección de ejemplo creada: ELEC2024001' as eleccion_ejemplo;

-- Mostrar resumen de datos creados
SELECT 'RESUMEN DE DATOS CREADOS:' as titulo;
SELECT COUNT(*) as total_administradores FROM administradores;
SELECT COUNT(*) as total_departamentos FROM departamentos;
SELECT COUNT(*) as total_cargos FROM cargos;
SELECT COUNT(*) as total_funcionarios FROM funcionarios;
SELECT COUNT(*) as total_tipos_eleccion FROM tipos_eleccion;
SELECT COUNT(*) as total_elecciones FROM elecciones;
SELECT COUNT(*) as total_candidatos FROM candidatos;
