CREATE TABLE IF NOT EXISTS customers (
    id serial PRIMARY KEY,
    username varchar(255),
    password varchar(255),
    role_id integer
);

CREATE TABLE IF NOT EXISTS roles (
    id serial PRIMARY KEY,
    rol varchar(15) NOT NULL UNIQUE,
    created_at timestamp NOT NULL
);

CREATE TABLE IF NOT EXISTS authority (
    id serial PRIMARY KEY,
    authority varchar(15) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS role_authorities (
    role_id integer NOT NULL REFERENCES roles(id),
    authority_id integer NOT NULL REFERENCES authority(id),
    PRIMARY KEY (role_id, authority_id)
);

ALTER TABLE customers ADD CONSTRAINT fk_customers_role
    FOREIGN KEY (role_id) REFERENCES roles(id);
