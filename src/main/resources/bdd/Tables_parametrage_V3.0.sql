----------------------------------------------------------------------
---------- Tables DATA LAKE -----------------------------------------

CREATE TABLE IF NOT EXISTS Facturation.clients_transactions (
    id_client VARCHAR(255),
    num_phone VARCHAR(20),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    fullname VARCHAR(255),
    address TEXT,
    libelle_ville VARCHAR(255),
    code_ville VARCHAR(50),
    gender CHAR(1),
    civility VARCHAR(50),
    nationality VARCHAR(50),
    country_code VARCHAR(10),
    type_id VARCHAR(50),
    num_id VARCHAR(255),
    identity_expirdate DATE,
    niveau_wallet VARCHAR(255),
    dt_sous_wallet DATE,
    dt_naissance DATE,
    age INT,
    rib VARCHAR(255),
    id_client_m2t VARCHAR(255),
    rib_compte_interne VARCHAR(255),
    id_wallet VARCHAR(255),
    dt_entree_relation_m2t DATE,
    code_oper VARCHAR(50),
    type_transaction VARCHAR(50),
    mnt DECIMAL(19, 2),
    date_validation DATE,
    code_es VARCHAR(50)
);

INSERT INTO clients_transactions (id_client, num_phone, first_name, last_name, fullname, address, libelle_ville, code_ville, gender, civility, nationality, country_code, type_id, num_id, identity_expirdate, niveau_wallet, dt_sous_wallet, dt_naissance, age, rib, id_client_m2t, rib_compte_interne, id_wallet, dt_entree_relation_m2t, code_oper, type_transaction, mnt, date_validation, code_es) VALUES
('069XXXX124', 'XXXXXXXX', 'XXXXXXXX', 'XXXXXXXX', 'XXXXXXXX', 'XXXXXXXXXXXXXXXXXXXXXXXX', 'taounate', '', 'F', '', '', '504', 'CIN', 'XXXXXXX', '2024-05-02', 'WALLET 20000', '2023-05-15', '1988-10-26', NULL, '8.4178E+23', '8.4178E+23', '142', '142', NULL, '0056', 'creation_compte', 0, '2023-05-15', ''),
('06XXX48979', 'XXXXXXXX', 'XXXXXXXX', 'XXXXXXXX', 'XXXXXXXX', 'XXXXXXXXXXXXXXXXXXXXXXXX', 'fekih ben saleh', '', 'M', '', '', '504', 'CIN', 'XXXXXXX', '2031-03-08', 'WALLET 5000', '2023-05-03', '1973-07-15', NULL, '8.4178E+23', '8.4178E+23', '331', '331', NULL, '0056', 'creation_compte', 0, '2023-05-03', '');


-------------------------------------------------------------------
------------------ Tables paramétrages -----------------------------

CREATE SCHEMA facturation AUTHORIZATION postgres;
-- facturation.activities definition

-- Drop table

-- DROP TABLE facturation.activities;

CREATE TABLE facturation.activities (
	id varchar(255) NOT NULL,
	"name" varchar(255) NULL,
	description text NULL,
	CONSTRAINT activities_pkey PRIMARY KEY (id)
);


-- facturation.operator_services definition

-- Drop table

-- DROP TABLE facturation.operator_services;

create table espace_service
(
    id         varchar(255) not null
        primary key,
    code_es    varchar(255),
    date_ajout timestamp(6),
    date_modif timestamp(6)
);

alter table espace_service
    owner to globalperf;

create table operateur
(
    id         varchar(255) not null
        primary key,
    code_oper  varchar(255),
    date_ajout timestamp(6),
    date_modif timestamp(6)
);

alter table operateur
    owner to globalperf;

create table type_operation
(
    id               varchar(255) not null
        primary key,
    code_transaction varchar(255),
    code_type_oper   varchar(255),
    date_ajout       timestamp(6),
    date_modif       timestamp(6),
    description      varchar(255),
    oper_id          varchar(255)
        constraint fkgayq0qm4u3bqf3g99wu5vkias
            references operateur
);

alter table type_operation
    owner to globalperf;

create table paliers
(
    id                varchar(255) not null
        primary key,
    code_oper         varchar(255),
    date_ajout        timestamp(6),
    date_modif        timestamp(6),
    frais_fixe        double precision,
    frais_plafonne    double precision,
    frais_pourcentage double precision,
    max_paplier       double precision,
    min_paplier       double precision,
    es_id             varchar(255)
        constraint fkmuqgt02y82vgpf3bn7a6c7pss
            references espace_service,
    type_operation_id varchar(255)
        constraint fkrlju0iqm65j9tc9q8q2xgjlbn
            references type_operation
);

alter table paliers
    owner to globalperf;

-- Insertions pour la table 'activities'
INSERT INTO facturation.activities (id, "name", description) VALUES 
('act1', 'Activité A', 'Description de l''activité A'),
('act2', 'Activité B', 'Description de l''activité B');

-- Insertions pour 'operator_services'
INSERT INTO facturation.operator_services (id, "name", description, active, category) VALUES 
('serv1', 'Service 1', 'Description Service 1', true, 'Catégorie 1'),
('serv2', 'Service 2', 'Description Service 2', true, 'Catégorie 2');

-- Insertions pour 'organisms'
INSERT INTO facturation.organisms (id, "name", description) VALUES 
('org1', 'Organisme 1', 'Description Organisme 1'),
('org2', 'Organisme 2', 'Description Organisme 2');

-- Insertions pour 'regions'
INSERT INTO facturation.regions (id, "name", description) VALUES 
('reg1', 'Région 1', 'Description Région 1'),
('reg2', 'Région 2', 'Description Région 2');

-- Insertions pour 'cities'
INSERT INTO facturation.cities (id, "name", region_id) VALUES 
(1, 'Ville 1', 'reg1'),
(2, 'Ville 2', 'reg2');

-- Insertions pour 'pricings'
INSERT INTO facturation.pricings (id, active, created_by, label, pricing_type, service_id) VALUES 
('price1', true, 'Utilisateur A', 'Tarif Standard', 'Type 1', 'serv1'),
('price2', true, 'Utilisateur B', 'Tarif Premium', 'Type 2', 'serv2');

-- Insertions pour 'pricing_activities'
INSERT INTO facturation.pricing_activities (pricing_id, activity_id) VALUES 
('price1', 'act1'),
('price2', 'act2');

-- Insertions pour 'pricing_cities'
INSERT INTO facturation.pricing_cities (pricing_id, city_id) VALUES 
('price1', 1),
('price2', 2);

-- Insertions pour 'pricing_organisms'
INSERT INTO facturation.pricing_organisms (pricing_id, organism_id) VALUES 
('price1', 'org1'),
('price2', 'org2');

-- Insertions pour 'pricing_ranges'
INSERT INTO facturation.pricing_ranges (id, active, range_from, range_to, pricing_type, pricing_id) VALUES 
('range1', true, 0, 100, 'Type 1', 'price1'),
('range2', true, 101, 200, 'Type 2', 'price2');

-- Insertions pour 'pricing_regions'
INSERT INTO facturation.pricing_regions (pricing_id, region_id) VALUES 
('price1', 'reg1'),
('price2', 'reg2');
