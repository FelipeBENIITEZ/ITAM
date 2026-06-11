CREATE TABLE persona (
    per_id SERIAL PRIMARY KEY,
    per_nom_1 VARCHAR(100) NOT NULL,
    per_nom_2 VARCHAR(100),
    per_ape_1 VARCHAR(100) NOT NULL,
    per_ape_2 VARCHAR(100),
    per_ci VARCHAR(20) UNIQUE NOT NULL,
    per_activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rol (
    rol_id SERIAL PRIMARY KEY,
    rol_nom VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE departamentos_organizacion (
    dept_id SERIAL PRIMARY KEY,
    dept_nom VARCHAR(100) NOT NULL UNIQUE,
    dept_descri VARCHAR(255),
    dept_activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE usuario (
    usu_id SERIAL PRIMARY KEY,
    usu_login VARCHAR(50) UNIQUE NOT NULL,
    usu_password VARCHAR(255) NOT NULL,
    usu_mail VARCHAR(100) UNIQUE NOT NULL,
    per_id INT NOT NULL UNIQUE REFERENCES persona(per_id),
    dept_id INT NOT NULL REFERENCES departamentos_organizacion(dept_id),
    rol_id INT NOT NULL REFERENCES rol(rol_id),
    usu_activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE proveedores (
    prov_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    prov_nom VARCHAR(100) NOT NULL UNIQUE,
    prov_descri VARCHAR(255),
    prov_activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categorias_activo (
    cat_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cat_nom VARCHAR(100) NOT NULL UNIQUE,
    cat_descri VARCHAR(255),
    cat_activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE marcas (
    marca_id SERIAL PRIMARY KEY,
    marca_nom VARCHAR(100) UNIQUE NOT NULL,
    marca_descri VARCHAR(255),
    marca_activa BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE modelo (
    model_id SERIAL PRIMARY KEY,
    marca_id INT NOT NULL REFERENCES marcas(marca_id),
    model_nom VARCHAR(100) NOT NULL,
    model_descri VARCHAR(255),
    model_activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (marca_id, model_nom)
);

CREATE TABLE estado_activo (
    estado_id SERIAL PRIMARY KEY,
    estado_nom VARCHAR(50) UNIQUE NOT NULL,
    estado_descri VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE activo (
    activo_id SERIAL PRIMARY KEY,
    activo_codigo VARCHAR(50) UNIQUE NOT NULL,
    prov_id INT REFERENCES proveedores(prov_id),
    cat_id INT NOT NULL REFERENCES categorias_activo(cat_id),
    estado_id INT NOT NULL REFERENCES estado_activo(estado_id),
    activo_nom VARCHAR(150) NOT NULL,
    activo_descri TEXT,
    activo_fecha_ingreso TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    activo_fecha_egreso TIMESTAMP,
    activo_activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE hardware_info (
    hw_id SERIAL PRIMARY KEY,
    activo_id INT NOT NULL UNIQUE REFERENCES activo(activo_id) ON DELETE CASCADE,
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
    garan_estado VARCHAR(20),
    garan_descri TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (garan_fecha_fin >= garan_fecha_inicio)
);

CREATE TABLE usuario_asignaciones (
    asignacion_id SERIAL PRIMARY KEY,
    usu_id INT NOT NULL REFERENCES usuario(usu_id),
    activo_id INT NOT NULL REFERENCES activo(activo_id),
    asignacion_fecha DATE NOT NULL DEFAULT CURRENT_DATE,
    devolucion_fecha DATE,
    asignacion_motivo VARCHAR(255),
    asignacion_observacion TEXT,
    asignacion_activa BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (
        devolucion_fecha IS NULL
        OR devolucion_fecha >= asignacion_fecha
    )
);

CREATE UNIQUE INDEX ux_activo_asignacion_activa
ON usuario_asignaciones (activo_id)
WHERE asignacion_activa = TRUE;
