CREATE TABLE IF NOT EXISTS activo_historial_estados (
    historial_id SERIAL PRIMARY KEY,
    activo_id INT NOT NULL REFERENCES activo(activo_id),
    estado_anterior_id INT REFERENCES estado_activo(estado_id),
    estado_nuevo_id INT NOT NULL REFERENCES estado_activo(estado_id),
    usuario_id INT NOT NULL REFERENCES usuario(usu_id),
    fecha_cambio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo TEXT,
    observaciones TEXT
);

CREATE INDEX IF NOT EXISTS idx_activo_historial_activo_fecha
ON activo_historial_estados (activo_id, fecha_cambio DESC);

CREATE TABLE IF NOT EXISTS soli_estados (
    soli_estado_id SERIAL PRIMARY KEY,
    soli_estado_nom VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS soli_tipos (
    soli_tipo_id SERIAL PRIMARY KEY,
    soli_tipo_nom VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS solicitudes (
    soli_id SERIAL PRIMARY KEY,
    soli_descri TEXT NOT NULL,
    soli_motivo TEXT,
    soli_estados_soli_estado_id INT NOT NULL REFERENCES soli_estados(soli_estado_id),
    soli_tipos_soli_tipo_id INT NOT NULL REFERENCES soli_tipos(soli_tipo_id),
    usuario_us_id INT NOT NULL REFERENCES usuario(usu_id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO soli_estados (soli_estado_nom)
VALUES
    ('Pendiente'),
    ('Aprobada'),
    ('Rechazada'),
    ('Completada')
ON CONFLICT (soli_estado_nom) DO NOTHING;

INSERT INTO soli_tipos (soli_tipo_nom)
VALUES
    ('Incidencia'),
    ('Solicitud de activo'),
    ('Cambio de estado'),
    ('Otro')
ON CONFLICT (soli_tipo_nom) DO NOTHING;
