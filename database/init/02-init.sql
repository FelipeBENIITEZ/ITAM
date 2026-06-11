INSERT INTO rol (rol_nom)
VALUES
    ('Administrador'),
    ('Tecnico Soporte'),
    ('Desarrollador');

INSERT INTO departamentos_organizacion (dept_nom, dept_descri)
VALUES
    ('TI', 'Departamento de Tecnologia de la Informacion'),
    ('Administracion', 'Departamento administrativo'),
    ('Contabilidad', 'Departamento contable'),
    ('Ventas', 'Departamento comercial');

INSERT INTO persona (per_nom_1, per_nom_2, per_ape_1, per_ape_2, per_ci)
VALUES
    ('Felipe', 'Isaias', 'Benitez', 'Martinez', '8321452'),
    ('Patrik', NULL, 'Jose', 'Kluivert', '654321');

INSERT INTO usuario (per_id, dept_id, rol_id, usu_login, usu_mail, usu_password)
VALUES
    (1, 1, 1, 'admin', 'admin@example.com', '$2a$10$kjidv7yZzEcjjjPNOvDNzOSz3vtHtx20AtYPezVtyViCAuOkLL8t2'),
    (2, 2, 3, 'patrik', 'ana@example.com', '$2a$10$hTxLX4G2HrDw7ODHu0h9YOabQ1lB5PM3JvJKQj0QX7to0Sk.KIbIu');

INSERT INTO estado_activo (estado_nom, estado_descri)
VALUES
    ('Disponible', 'Activo disponible para asignacion'),
    ('Asignado', 'Activo asignado a un usuario'),
    ('En mantenimiento', 'Activo temporalmente fuera de uso por mantenimiento'),
    ('Dado de baja', 'Activo retirado del inventario operativo'),
    ('Extraviado', 'Activo no localizado');

INSERT INTO categorias_activo (cat_nom, cat_descri)
VALUES
    ('Notebook', 'Computadora portatil'),
    ('PC Escritorio', 'Computadora de escritorio'),
    ('Monitor', 'Pantalla o monitor'),
    ('Impresora', 'Equipo de impresion'),
    ('Router', 'Equipo de enrutamiento'),
    ('Switch', 'Equipo de conmutacion de red'),
    ('Access Point', 'Punto de acceso inalambrico'),
    ('Servidor', 'Equipo servidor'),
    ('UPS', 'Sistema de alimentacion ininterrumpida');
