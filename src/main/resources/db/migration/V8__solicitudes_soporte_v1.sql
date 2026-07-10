ALTER TABLE solicitudes
    ADD COLUMN IF NOT EXISTS activo_id INT NULL,
    ADD COLUMN IF NOT EXISTS marca_id INT NULL,
    ADD COLUMN IF NOT EXISTS model_id INT NULL,
    ADD COLUMN IF NOT EXISTS soli_cantidad INT NULL,
    ADD COLUMN IF NOT EXISTS usu_responsable_id INT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_solicitudes_activo'
    ) THEN
        ALTER TABLE solicitudes
            ADD CONSTRAINT fk_solicitudes_activo
            FOREIGN KEY (activo_id) REFERENCES activo(activo_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_solicitudes_marca'
    ) THEN
        ALTER TABLE solicitudes
            ADD CONSTRAINT fk_solicitudes_marca
            FOREIGN KEY (marca_id) REFERENCES marcas(marca_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_solicitudes_modelo'
    ) THEN
        ALTER TABLE solicitudes
            ADD CONSTRAINT fk_solicitudes_modelo
            FOREIGN KEY (model_id) REFERENCES modelo(model_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_solicitudes_responsable'
    ) THEN
        ALTER TABLE solicitudes
            ADD CONSTRAINT fk_solicitudes_responsable
            FOREIGN KEY (usu_responsable_id) REFERENCES usuario(usu_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_solicitudes_cantidad'
    ) THEN
        ALTER TABLE solicitudes
            ADD CONSTRAINT chk_solicitudes_cantidad
            CHECK (soli_cantidad IS NULL OR soli_cantidad > 0);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS solicitud_historial_estados (
    historial_id SERIAL PRIMARY KEY,
    soli_id INT NOT NULL REFERENCES solicitudes(soli_id) ON DELETE CASCADE,
    estado_anterior_id INT NULL REFERENCES soli_estados(soli_estado_id),
    estado_nuevo_id INT NOT NULL REFERENCES soli_estados(soli_estado_id),
    usuario_id INT NOT NULL REFERENCES usuario(usu_id),
    fecha_cambio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observaciones TEXT NULL
);

CREATE INDEX IF NOT EXISTS idx_solicitud_historial_solicitud_fecha
ON solicitud_historial_estados (soli_id, fecha_cambio DESC);

INSERT INTO soli_tipos (soli_tipo_nom)
VALUES
    ('Mantenimiento'),
    ('Baja'),
    ('Compra'),
    ('Asignación')
ON CONFLICT (soli_tipo_nom) DO NOTHING;

INSERT INTO soli_estados (soli_estado_nom)
VALUES
    ('En análisis'),
    ('Cerrada'),
    ('Cancelada')
ON CONFLICT (soli_estado_nom) DO NOTHING;
