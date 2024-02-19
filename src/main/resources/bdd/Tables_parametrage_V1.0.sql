-- Création de la table des types d'opérations
CREATE TABLE TypeOperation (
    TypeOperationID SERIAL PRIMARY KEY,
    CodeTransaction VARCHAR(50) NOT NULL,
    Description VARCHAR(255) NOT NULL
);

-- Mise à jour de la table des paliers avec une référence à TypeOperation
CREATE TABLE Palier (
    PalierID SERIAL PRIMARY KEY,
    TypeOperationID INTEGER REFERENCES TypeOperation(TypeOperationID),
    MinMontant NUMERIC NOT NULL,
    MaxMontant NUMERIC NOT NULL,
    CHECK (MinMontant < MaxMontant)
);

-- Création de la table des frais en pourcentage
CREATE TABLE FraisPourcentage (
    FraisPourcentageID SERIAL PRIMARY KEY,
    PalierID INTEGER REFERENCES Palier(PalierID),
    Pourcentage NUMERIC NOT NULL CHECK (Pourcentage > 0)
);

-- Création de la table des frais en pourcentage avec un minimum
CREATE TABLE FraisPourcentageAvecMin (
    FraisPourcentageAvecMinID SERIAL PRIMARY KEY,
    PalierID INTEGER REFERENCES Palier(PalierID),
    Pourcentage NUMERIC NOT NULL CHECK (Pourcentage > 0),
    MinFrais NUMERIC NOT NULL CHECK (MinFrais > 0)
);

-- Création de la table des frais fixes
CREATE TABLE FraisFixe (
    FraisFixeID SERIAL PRIMARY KEY,
    PalierID INTEGER REFERENCES Palier(PalierID),
    MontantFixe NUMERIC NOT NULL CHECK (MontantFixe > 0)
);

-- Création de la table des frais hybrides
CREATE TABLE FraisHybride (
    FraisHybrideID SERIAL PRIMARY KEY,
    PalierID INTEGER REFERENCES Palier(PalierID),
    Pourcentage NUMERIC NOT NULL CHECK (Pourcentage > 0),
    MinFrais NUMERIC CHECK (MinFrais > 0) -- Peut être NULL si pas de minimum
);

------------------------------------------------------------------------------------
------------ Montant fixe par transaction : 
------------ une transaction avec un montant de 1000 et un frais fixe de 60
------------ 1000  * 5% = 50
-----------------------------------------------------------------------------------------
-- Insérer le type d'opération
INSERT INTO TypeOperation (TypeOperationID, Description, CodeTransaction) VALUES (1, 'Montant fixe par transaction', 'FIXE');

-- Insérer le palier
INSERT INTO Palier (PalierID, TypeOperationID, MinMontant, MaxMontant) VALUES (1, 1, 0, 1000000);

-- Insérer les frais fixes
INSERT INTO FraisFixe (FraisFixeID, PalierID, MontantFixe) VALUES (1, 1, 60);

------------------------------------------------------------------------------------
------------ Pourcentage de la commission : 
------------ transaction avec un montant de 500 et  2% comme commission
------------  500  * 2% = 10
-----------------------------------------------------------------------------------------
-- Insérer le type d'opération
INSERT INTO TypeOperation (TypeOperationID, Description, CodeTransaction) VALUES (2, 'Pourcentage de la commission', 'POURCENTAGE');

-- Insérer le palier
INSERT INTO Palier (PalierID, TypeOperationID, MinMontant, MaxMontant) VALUES (2, 2, 0, 1000000);

-- Insérer les frais en pourcentage
INSERT INTO FraisPourcentage (FraisPourcentageID, PalierID, Pourcentage) VALUES (1, 2, 2);

------------------------------------------------------------------------------------
------------ Pourcentage par paliers du montant de la transaction 
------------ Jusqu'à 50  : 2% / De plus de 50  jusqu'à 100  : 3% / De plus de 100  jusqu'à 200  : 4%
-----------------------------------------------------------------------------------------
-- Insérer le type d'opération
INSERT INTO TypeOperation (TypeOperationID, Description, CodeTransaction) VALUES (3, 'Pourcentage par paliers du montant de la transaction', 'POURCENTAGE_PALIERS');

-- Insérer les paliers et les frais en pourcentage
-- Palier 1 : Jusqu'à 50
INSERT INTO Palier (PalierID, TypeOperationID, MinMontant, MaxMontant) VALUES (3, 3, 0, 50);
INSERT INTO FraisPourcentage (FraisPourcentageID, PalierID, Pourcentage) VALUES (2, 3, 2);

-- Palier 2 : De plus de 50 jusqu'à 100
INSERT INTO Palier (PalierID, TypeOperationID, MinMontant, MaxMontant) VALUES (4, 3, 51, 100);
INSERT INTO FraisPourcentage (FraisPourcentageID, PalierID, Pourcentage) VALUES (3, 4, 3);

-- Palier 3 : De plus de 100 jusqu'à 200
INSERT INTO Palier (PalierID, TypeOperationID, MinMontant, MaxMontant) VALUES (5, 3, 101, 200);
INSERT INTO FraisPourcentage (FraisPourcentageID, PalierID, Pourcentage) VALUES (4, 5, 4);

------------------------------------------------------------------------------------
------------ Pourcentage par paliers du montant de la transaction avec un minimum :
------------ jusqu'à 50  : 2%, avec un minimum de 1 /De plus de 50  jusqu'à 100  : 3%, avec un minimum de 2 / De plus de 100 jusqu'à 200 : 4%, sans minimum
-----------------------------------------------------------------------------------------
-- Insérer le type d'opération
INSERT INTO TypeOperation (TypeOperationID, Description, CodeTransaction) VALUES (4, 'Pourcentage par paliers du montant de la transaction avec un minimum', 'POURCENTAGE_PALIERS_MINIMUM');

-- Insérer les paliers et les frais en pourcentage avec un minimum
-- Palier 1 : Jusqu'à 50
INSERT INTO Palier (PalierID, TypeOperationID, MinMontant, MaxMontant) VALUES (6, 4, 0, 50);
INSERT INTO FraisPourcentageAvecMin (FraisPourcentageAvecMinID, PalierID, Pourcentage, MinFrais) VALUES (1, 6, 2, 1);

-- Palier 2 : De plus de 50 jusqu'à 100
INSERT INTO Palier (PalierID, TypeOperationID, MinMontant, MaxMontant) VALUES (7, 4, 51, 100);
INSERT INTO FraisPourcentageAvecMin (FraisPourcentageAvecMinID, PalierID, Pourcentage, MinFrais) VALUES (2, 7, 3, 2);

-- Palier 3 : De plus de 100 jusqu'à 200
INSERT INTO Palier (PalierID, TypeOperationID, MinMontant, MaxMontant) VALUES (8, 4, 101, 200);
INSERT INTO FraisPourcentageAvecMin (FraisPourcentageAvecMinID, PalierID, Pourcentage, MinFrais) VALUES (3, 8, 4, NULL);

------------------------------------------------------------------------------------
------------ Montant fixe par palier 
------------ jusqu'à 50 : 5 /  De plus de 50 jusqu'à 100 : 10 / De plus de 100 jusqu'à 200 : 15
-----------------------------------------------------------------------------------------
-- Insérer le type d'opération
INSERT INTO TypeOperation (TypeOperationID, Description, CodeTransaction) VALUES (5, 'Montant fixe par palier', 'FIXE_PALIERS');

-- Insérer les paliers et les frais fixes
-- Palier 1 : Jusqu'à 50
INSERT INTO Palier (PalierID, TypeOperationID, MinMontant, MaxMontant) VALUES (9, 5, 0, 50);
INSERT INTO FraisFixe (FraisFixeID, PalierID, MontantFixe) VALUES (2, 9, 5);

-- Palier 2 : De plus de 50 jusqu'à 100
INSERT INTO Palier (PalierID, TypeOperationID, MinMontant, MaxMontant) VALUES (10, 5, 51, 100);
INSERT INTO FraisFixe (FraisFixeID, PalierID, MontantFixe) VALUES (3, 10, 10);

-- Palier 3 : De plus de 100 jusqu'à 200
INSERT INTO Palier (PalierID, TypeOperationID, MinMontant, MaxMontant) VALUES (11, 5, 101, 200);
INSERT INTO FraisFixe (FraisFixeID, PalierID, MontantFixe) VALUES (4, 11, 15);

------------------------------------------------------------------------------------
------------ Modèle hybride : pourcentage par paliers puis un montant fixe
------------ Jusqu'à 50 : 2%, avec un minimum de 1 / plus de 50 jusqu'à 100 : 3%, avec un minimum de 2 / Au-delà de 100 : 4%, avec un minimum de 5
-----------------------------------------------------------------------------------------
-- Insérer le type d'opération
INSERT INTO TypeOperation (TypeOperationID, Description, CodeTransaction) VALUES (6, 'Modèle hybride : pourcentage par paliers puis un montant fixe', 'HYBRIDE');

-- Insérer les paliers et les frais pourcentage avec un minimum
-- Palier 1 : Jusqu'à 50
INSERT INTO Palier (PalierID, TypeOperationID, MinMontant, MaxMontant) VALUES (12, 6, 0, 50);
INSERT INTO FraisPourcentageAvecMin (FraisPourcentageAvecMinID, PalierID, Pourcentage, MinFrais) VALUES (4, 12, 2, 1);

-- Palier 2 : De plus de 50 jusqu'à 100
INSERT INTO Palier (PalierID, TypeOperationID, MinMontant, MaxMontant) VALUES (13, 6, 51, 100);
INSERT INTO FraisPourcentageAvecMin (FraisPourcentageAvecMinID, PalierID, Pourcentage, MinFrais) VALUES (5, 13, 3, 2);

-- Palier 3 : Au-delà de 100
INSERT INTO Palier (PalierID, TypeOperationID, MinMontant) VALUES (14, 6, 101);
INSERT INTO FraisPourcentageAvecMin (FraisPourcentageAvecMinID, PalierID, Pourcentage, MinFrais) VALUES (6, 14, 4, 5);
