ALTER TABLE solicitudes
    ADD COLUMN IF NOT EXISTS usu_destino_id INT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_solicitudes_destino'
    ) THEN
        ALTER TABLE solicitudes
            ADD CONSTRAINT fk_solicitudes_destino
            FOREIGN KEY (usu_destino_id) REFERENCES usuario(usu_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_solicitudes_destino
ON solicitudes (usu_destino_id);

INSERT INTO soli_tipos (soli_tipo_nom)
VALUES ('Reasignación')
ON CONFLICT (soli_tipo_nom) DO NOTHING;
