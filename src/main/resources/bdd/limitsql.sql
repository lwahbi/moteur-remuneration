CREATE TABLE clients_transactions AS SELECT * FROM clients_transactions_old LIMIT 100;
--CREATE TABLE clients_transactions_old AS TABLE clients_transactions;
drop table clients_transactions;