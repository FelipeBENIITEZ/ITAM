INSERT INTO persona (per_nom_1, per_nom_2, per_ape_1, per_ape_2, per_ci)
VALUES
    ('Felipe', 'Isaias', 'Benitez', 'Martinez', '8321452'),
    ('Patrik', NULL, 'Jose', 'Kluivert', '654321')
ON CONFLICT (per_ci) DO NOTHING;

INSERT INTO usuario (per_id, dept_id, rol_id, usu_login, usu_mail, usu_password)
SELECT p.per_id, d.dept_id, r.rol_id, 'admin', 'admin@example.com',
       '$2a$10$kjidv7yZzEcjjjPNOvDNzOSz3vtHtx20AtYPezVtyViCAuOkLL8t2'
FROM persona p
JOIN departamentos_organizacion d ON d.dept_nom = 'TI'
JOIN rol r ON r.rol_nom = 'Administrador'
WHERE p.per_ci = '8321452'
ON CONFLICT (usu_login) DO NOTHING;

INSERT INTO usuario (per_id, dept_id, rol_id, usu_login, usu_mail, usu_password)
SELECT p.per_id, d.dept_id, r.rol_id, 'patrik', 'ana@example.com',
       '$2a$10$hTxLX4G2HrDw7ODHu0h9YOabQ1lB5PM3JvJKQj0QX7to0Sk.KIbIu'
FROM persona p
JOIN departamentos_organizacion d ON d.dept_nom = 'Administracion'
JOIN rol r ON r.rol_nom = 'Desarrollador'
WHERE p.per_ci = '654321'
ON CONFLICT (usu_login) DO NOTHING;
