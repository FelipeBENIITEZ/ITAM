INSERT INTO categorias_activo (cat_nom, cat_descri, cat_activo)
VALUES
    ('Computadora de Escritorio', 'Equipo de escritorio para uso general', TRUE),
    ('Laptop', 'Equipo portatil de uso general', TRUE),
    ('Servidor', 'Equipo servidor o plataforma dedicada', TRUE),
    ('Dispositivo de Red', 'Equipos de red como router, switch o access point', TRUE),
    ('Periférico', 'Dispositivos perifericos y de apoyo', TRUE),
    ('Componente', 'Piezas y componentes fisicos del inventario', TRUE),
    ('Dispositivo IoT', 'Dispositivos conectados de proposito especifico', TRUE),
    ('Impresora / Multifunción', 'Equipos de impresion o multifuncion', TRUE),
    ('Equipo de Energía', 'UPS y otros equipos de energia', TRUE),
    ('Dispositivo Móvil', 'Telefonos, tablets y equipos moviles', TRUE),
    ('Otro', 'Categoria generica para activos fuera de las familias principales', TRUE)
ON CONFLICT (cat_nom) DO UPDATE
SET cat_descri = EXCLUDED.cat_descri,
    cat_activo = TRUE,
    updated_at = CURRENT_TIMESTAMP;

UPDATE categorias_activo
SET cat_activo = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE cat_nom IN (
    'Notebook',
    'PC Escritorio',
    'Monitor',
    'Impresora',
    'Router',
    'Switch',
    'Access Point',
    'Firewall',
    'UPS'
);
