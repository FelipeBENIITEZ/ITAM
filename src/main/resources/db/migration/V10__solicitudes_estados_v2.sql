INSERT INTO soli_estados (soli_estado_nom)
VALUES
    ('En ejecución'),
    ('Resuelta')
ON CONFLICT (soli_estado_nom) DO NOTHING;
