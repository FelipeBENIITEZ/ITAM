INSERT INTO Rol (rol_nom) VALUES ('Administrador'),('Tecnico Soporte'),('Desarrollador');

INSERT INTO Departamentos_organizacion (dept_nom) VALUES 
  ('Tecnología de la Información'),
  ('Contabilidad'),
  ('Recursos Humanos'),
  ('Atencion al cliente');

INSERT INTO Persona (per_nom_1, per_nom_2, per_ape_1, per_ape_2, per_ci) VALUES 
  ('Felipe', 'Isaias', 'Benítez', 'Martínez', '8321452'),
  ('Patrik', NULL, 'Jose', 'Kluivert', '654321');


INSERT INTO Usuario (per_id, dept_id, rol_id, usu_login, usu_mail, usu_password)
VALUES
  (1, 1, 1, 'admin', 'admin@example.com', '$2a$10$kjidv7yZzEcjjjPNOvDNzOSz3vtHtx20AtYPezVtyViCAuOkLL8t2'),
  (2, 2, 3, 'patrik', 'ana@example.com', '$2a$10$hTxLX4G2HrDw7ODHu0h9YOabQ1lB5PM3JvJKQj0QX7to0Sk.KIbIu');

INSERT INTO presupuesto_areas (dept_id, pres_asignado, pres_usado, pres_ini_vigencia, pres_fin_vigencia) 
VALUES 
(1, 50000.00, 0.00, '2025-01-01', '2025-12-31'),
(2, 30000.00, 0.00, '2025-01-01', '2025-12-31'),
(3, 40000.00, 0.00, '2025-01-01', '2025-12-31');