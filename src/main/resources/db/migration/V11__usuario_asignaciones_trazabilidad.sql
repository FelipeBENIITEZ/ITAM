ALTER TABLE usuario_asignaciones
    ADD COLUMN IF NOT EXISTS usu_ejecutor_id INT NULL,
    ADD COLUMN IF NOT EXISTS solicitud_id INT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_usuario_asignaciones_ejecutor'
    ) THEN
        ALTER TABLE usuario_asignaciones
            ADD CONSTRAINT fk_usuario_asignaciones_ejecutor
            FOREIGN KEY (usu_ejecutor_id) REFERENCES usuario(usu_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_usuario_asignaciones_solicitud'
    ) THEN
        ALTER TABLE usuario_asignaciones
            ADD CONSTRAINT fk_usuario_asignaciones_solicitud
            FOREIGN KEY (solicitud_id) REFERENCES solicitudes(soli_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_usuario_asignaciones_ejecutor
ON usuario_asignaciones (usu_ejecutor_id);

CREATE INDEX IF NOT EXISTS idx_usuario_asignaciones_solicitud
ON usuario_asignaciones (solicitud_id);
