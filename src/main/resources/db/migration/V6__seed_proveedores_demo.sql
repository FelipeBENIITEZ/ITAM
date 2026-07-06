INSERT INTO proveedores (prov_nom, prov_descri, prov_activo)
VALUES
    ('ACME Tecnologia', 'Proveedor general de equipos y accesorios TI', TRUE),
    ('TechSupply Uruguay', 'Suministro de hardware y consumibles TI', TRUE),
    ('Redes Integradas', 'Equipos de red y conectividad', TRUE),
    ('Soporte Empresarial', 'Provision y reposicion de activos para empresas', TRUE),
    ('Nexa Hardware', 'Equipos de escritorio, portatiles y perifericos', TRUE),
    ('Energia Segura', 'UPS y proteccion electrica', TRUE),
    ('Centro de Computo', 'Proveedor de estaciones de trabajo y perifericos', TRUE),
    ('Distribuciones IT', 'Distribucion de equipos y soluciones tecnologicas', TRUE)
ON CONFLICT (prov_nom) DO UPDATE
SET prov_descri = EXCLUDED.prov_descri,
    prov_activo = TRUE,
    updated_at = CURRENT_TIMESTAMP;
