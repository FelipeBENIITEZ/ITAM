INSERT INTO marcas (marca_nom, marca_descri, marca_activa)
VALUES
    ('HP', 'Fabricante de equipos de computo e impresion', TRUE),
    ('Epson', 'Fabricante de impresion y perifericos', TRUE),
    ('Fortinet', 'Fabricante de soluciones de red y seguridad', TRUE),
    ('Logitech', 'Fabricante de perifericos y accesorios', TRUE)
ON CONFLICT (marca_nom) DO UPDATE
SET marca_descri = EXCLUDED.marca_descri,
    marca_activa = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO modelo (marca_id, model_nom, model_descri, model_activo)
SELECT m.marca_id, v.model_nom, v.model_descri, TRUE
FROM marcas m
JOIN (
    VALUES
        ('HP', 'HP 250 G10', 'Notebook empresarial de entrada'),
        ('Epson', 'Epson D240', 'Impresora de escritorio'),
        ('Fortinet', 'FortiGate 40F', 'Firewall de sucursal'),
        ('Logitech', 'Logitech M90', 'Mouse optico basico')
) AS v(marca_nom, model_nom, model_descri)
    ON m.marca_nom = v.marca_nom
ON CONFLICT (marca_id, model_nom) DO UPDATE
SET model_descri = EXCLUDED.model_descri,
    model_activo = TRUE,
    updated_at = CURRENT_TIMESTAMP;
