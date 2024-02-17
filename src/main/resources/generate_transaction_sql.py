import os
#Execution python generate_transaction_sql.py
# Définition des paramètres initiaux pour la génération des données SQL
nombre_enregistrements = 1000000  # Nombre d'enregistrements à générer
codes_es = [f'ES{i}' for i in range(1, 5001)]  # Codes ES possibles pour la variation

# Chemin pour enregistrer le fichier sur le bureau de l'utilisateur sous Windows
chemin_fichier = os.path.join('C:', 'Users', 'Hp', 'Desktop', 'clients_transactions_insert.sql')

# Début de la génération du script SQL
sql_script = """-- Script SQL pour insérer 1 million d'enregistrements dans public.clients_transactions
BEGIN;\n"""

# Générer les enregistrements
for i in range(1, nombre_enregistrements + 1):
    code_es = codes_es[i % len(codes_es)]  # Sélectionner un code ES de manière cyclique
    sql_script += f"INSERT INTO public.clients_transactions (id, id_client, num_phone, first_name, last_name, code_es, dt_naissance, mnt) VALUES ({i}, 'ID_{i}', '06000000{i % 1000000:06d}', 'FirstName_{i}', 'LastName_{i}', '{code_es}', '1980-01-01', {100.00 + i % 100});\n"

# Fin du script
sql_script += "COMMIT;\n"

# Sauvegarder le script dans un fichier sur le bureau
with open(chemin_fichier, 'w') as file:
    file.write(sql_script)

print(f"Le script SQL a été enregistré sur le bureau : {chemin_fichier}")
