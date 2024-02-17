CREATE TABLE param_TypeOperation (
    TypeOperationID SERIAL PRIMARY KEY,
    CodeTransaction VARCHAR(50) NOT NULL,
    Description VARCHAR(255) NOT NULL
);

CREATE TABLE param_Palier (
    PalierID SERIAL PRIMARY KEY,
    TypeOperationID INTEGER REFERENCES param_TypeOperation(TypeOperationID), 
    MinPaplier NUMERIC,
    MaxPalier NUMERIC CHECK (MinPaplier< MaxPalier ),
	FraisFixe NUMERIC ,
	FraisPoucentage NUMERIC, 
	MinMontant NUMERIC
); 



-- Insertion pour les types d'opérations
INSERT INTO param_TypeOperation (CodeTransaction, Description) VALUES 
('FIXE60', 'Montant fixe de 60 pour transaction de 1000'),
('COM2PC', '2% de commission pour transaction de 500'),
('POURCENTAGE_PALIER_VAR_AVEC_LIMMIT', 'Pourcentage variable par palier avec limite'),
('PALIERMIN', 'Pourcentage par palier avec minimum'),
('FIXE_PALIER_AVEC_LIMIT', 'Montant fixe par palier avec Limite'),
('FIXE_PALIER_SANS_LIMIT', 'Montant fixe par palier sans Limite'),
('POUCENTAGE_PALIER_VAR_SANS_LIMIT', 'Pourcentage variable par palier Sans Limite');

-- Assurez-vous d'obtenir les TypeOperationID générés pour les utiliser dans les insertions suivantes. 
-- Je vais supposer que les IDs générés sont 1, 2, 3, 4 et 5 respectivement.

-- Insertion pour les paliers de transactions
-- Pour le montant fixe par transaction
INSERT INTO param_Palier (TypeOperationID, MinPaplier, MaxPalier, FraisFixe) VALUES (1, 0, NULL, 60);

-- Pour le pourcentage de commission
INSERT INTO param_Palier (TypeOperationID, MinPaplier, MaxPalier, FraisPoucentage) VALUES (2, 0, NULL, 2);

-- Pour le pourcentage par paliers
INSERT INTO param_Palier (TypeOperationID, MinPaplier, MaxPalier, FraisPoucentage) VALUES 
(3, 0, 50, 2),
(3, 50, 100, 3),
(3, 100, 200, 4);

-- Pour le pourcentage par paliers avec un minimum
INSERT INTO param_Palier (TypeOperationID, MinPaplier, MaxPalier, FraisPoucentage, MinMontant) VALUES 
(4, 0, 50, 2, 1),
(4, 50, 100, 3, 2),
(4, 100, 200, 4, NULL); -- NULL représente l'absence de minimum dans ce cas

-- Pour le montant fixe par palier
INSERT INTO param_Palier (TypeOperationID, MinPaplier, MaxPalier, FraisFixe) VALUES 
(5, 0, 50, 5),
(5, 50, 100, 10),
(5, 100, 200, 15);

-- Insertion pour les montants fixes par palier
-- jusqu'à 50 : 5 /  De plus de 50 jusqu'à 100 : 10 / De plus de 100 jusqu'à 200 : 15 / plus de 200  : 20
INSERT INTO param_Palier (TypeOperationID, MinPaplier, MaxPalier, FraisFixe) VALUES 
(6, 0, 50, 5),
(6, 50.01, 100, 10),  -- On commence à 50.01 pour éviter le chevauchement avec le palier précédent
(6, 100.01, 200, 15), -- De même, on commence à 100.01
(6, 200.01, NULL, 20); -- NULL pour MaxPalier indique qu'il n'y a pas de limite supérieure pour ce palier

-- Insertion pour les pourcentages par palier de montant de la transaction
-- Jusqu'à 50  : 2% / De plus de 50  jusqu'à 100  : 3% / De plus de 100  jusqu'à 200  : 4% / plus de 200  : 1 %
INSERT INTO param_Palier (TypeOperationID, MinPaplier, MaxPalier, FraisPoucentage) VALUES 
(7, 0, 50, 2),
(7, 50.01, 100, 3),  -- On commence à 50.01 pour éviter le chevauchement avec le palier précédent
(7, 100.01, 200, 4), -- De même, on commence à 100.01
(7, 200.01, NULL, 1); -- NULL pour MaxPalier indique qu'il n'y a pas de limite supérieure pour ce palier