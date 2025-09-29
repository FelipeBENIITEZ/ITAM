CREATE TABLE persona (
    per_id SERIAL PRIMARY KEY,
    per_nom_1 VARCHAR(100) NOT NULL,
    per_nom_2 VARCHAR(100),
    per_ape_1 VARCHAR(100) NOT NULL,
    per_ape_2 VARCHAR(100),
    per_ci VARCHAR(20) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rol (
    rol_id SERIAL PRIMARY KEY,
    rol_nom VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE departamentos_organizacion (
    dept_id SERIAL PRIMARY KEY,
    dept_nom VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE usuario (
    usu_id SERIAL PRIMARY KEY,
    usu_login VARCHAR(50) UNIQUE NOT NULL,
    usu_password VARCHAR(255) NOT NULL,
    usu_mail VARCHAR(100) UNIQUE NOT NULL, 
    per_id INT REFERENCES persona(per_id),
    dept_id INT NOT NULL REFERENCES departamentos_organizacion(dept_id),
    rol_id INT NOT NULL REFERENCES Rol(rol_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE proveedores (
    prov_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    prov_nom VARCHAR(100) NOT NULL,
    prov_descri VARCHAR(255)
);

CREATE TABLE categorias_activo (
    cat_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cat_nom VARCHAR(100) NOT NULL,
    cat_descri VARCHAR(255)
);


CREATE TABLE marcas (
    marca_id SERIAL PRIMARY KEY,
    marca_nom VARCHAR(100) UNIQUE NOT NULL,
    marca_descri VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE modelo (
    model_id SERIAL PRIMARY KEY,
    marca_id INT NOT NULL REFERENCES Marcas(marca_id),
    model_nom VARCHAR(100) NOT NULL,
    model_descri VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE contrato_info (
    contrat_id SERIAL PRIMARY KEY,
    contrat_nom VARCHAR(100) NOT NULL,
    contrat_descri VARCHAR(255),
    contrat_fecha_inicio DATE NOT NULL,
    contrat_fecha_fin DATE,
    contrat_monto NUMERIC(12,2),
    prov_id INT NOT NULL REFERENCES Proveedores(prov_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE estado_activo (
    estado_id SERIAL PRIMARY KEY,
    estado_nom VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE activo (
    activo_id SERIAL PRIMARY KEY,
    contrat_id INT REFERENCES contrato_info(contrat_id),
    dept_id INT NOT NULL REFERENCES departamentos_organizacion(dept_id),
    cat_id INT NOT NULL REFERENCES categorias_Activo(cat_id),
    estado_id INT NOT NULL REFERENCES estado_Activo(estado_id),
    activo_nom VARCHAR(150) NOT NULL,
    activo_fecha_ingreso TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    activo_fecha_egreso TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE hardware_info (
    hw_id SERIAL PRIMARY KEY,
    activo_id INT NOT NULL REFERENCES activo(activo_id) ON DELETE CASCADE,
    model_id INT NOT NULL REFERENCES modelo(model_id),
    hw_serial_num VARCHAR(100) UNIQUE NOT NULL,
    hw_descri TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE garantias (
    garan_id SERIAL PRIMARY KEY,
    hw_id INT NOT NULL REFERENCES hardware_info(hw_id) ON DELETE CASCADE,
    garan_fecha_inicio DATE NOT NULL,
    garan_fecha_fin DATE NOT NULL,
    garan_vigencia VARCHAR(20), -- Ej: 'Activa', 'Expirada'
    garan_descri TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuario_asignaciones (
    usu_id INT NOT NULL REFERENCES usuario(usu_id) ON DELETE CASCADE,
    activo_id INT NOT NULL REFERENCES activo(activo_id) ON DELETE CASCADE,
    asignacion_fecha DATE NOT NULL DEFAULT CURRENT_DATE,
    asignacion_motivo VARCHAR(255),
    PRIMARY KEY (usu_id, activo_id)
);

CREATE TABLE presupuesto_areas (
    pres_id SERIAL PRIMARY KEY,
    dept_id INT NOT NULL,
    pres_asignado NUMERIC(12,2) NOT NULL,
    pres_usado NUMERIC(12,2) DEFAULT 0,
    pres_ini_vigencia DATE NOT NULL,
    pres_fin_vigencia DATE NOT NULL,

    CONSTRAINT fk_pres_departamento
        FOREIGN KEY (dept_id) 
        REFERENCES departamentos_organizacion (dept_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE hardware_costos (
    hw_id INT NOT NULL,
    pres_id INT NOT NULL,
    hw_valor_compra NUMERIC(12,2) NOT NULL,
    hw_valor_venta NUMERIC(12,2) DEFAULT 0,
    CONSTRAINT fk_hw_costos_hardware
        FOREIGN KEY (hw_id) 
        REFERENCES hardware_info (hw_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_hw_costos_presupuesto
        FOREIGN KEY (pres_id) 
        REFERENCES presupuesto_areas (pres_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);