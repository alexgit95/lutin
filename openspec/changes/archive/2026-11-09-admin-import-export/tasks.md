## 1. Spike et descripteurs

- [x] 1.1 Spike sur SQLite : insérer à identifiant explicite par SQL natif paramétré des lignes de chaque type (Long, String, Boolean, BigDecimal, LocalDate, Instant), les relire par Hibernate et vérifier que le compteur d'identifiants suit ; rédiger la requête de recalage PostgreSQL ; si le SQL natif échoue, repli documenté sur la persistance par entités avec remappage des identifiants (y compris `souhait_id` et `wishlist_id` d'`avis`) et mettre à jour `design.md`
- [x] 1.2 Descripteurs des 12 tables (nom, colonnes, type logique, colonnes obligatoires, références) dans l'ordre des clés étrangères, et constante de version de format
- [x] 1.3 Test de conformité : comparer les descripteurs au métamodèle JPA (une colonne d'entité absente des descripteurs fait échouer le test)
- [x] 1.4 Configuration : `spring.servlet.multipart.max-file-size` et `max-request-size` à 100 Mo, message lisible en cas de dépassement

## 2. Export

- [x] 2.1 Service d'export en flux : en-tête (format, version, mode, version de l'application, date), tables dans l'ordre, lecture par lots de 500 triés par identifiant avec libération du contexte de persistance
- [x] 2.2 `POST /admin/export` : réponse en flux avec `Content-Disposition` daté et `Cache-Control: no-store`, administrateur seulement, CSRF actif

## 3. Import

- [x] 3.1 Passe de validation sans écriture : en-tête (format, version, mode), tables dans l'ordre, colonnes connues et obligatoires, types, longueurs, JSON complet, identifiants uniques, références résolues, garde-fou du compte administrateur (empreinte en `cloudflare`, login en `local`)
- [x] 3.2 Passe d'import dans une transaction unique : suppression des tables dans l'ordre inverse, insertion par lots avec les identifiants du fichier, recalage des compteurs d'identifiants
- [x] 3.3 Confirmation « IMPORTER » vérifiée côté serveur, verrou d'exclusion (409), compte rendu par table, journal limité à l'événement et aux totaux
- [x] 3.4 `POST /admin/import` multipart avec CSRF, messages d'erreur clairs et retour sur l'écran Admin

## 4. Interface

- [x] 4.1 `admin.html` : section « Sauvegarde » (bouton d'export, formulaire d'import avec fichier, champ de confirmation et avertissement sur le wipe total et la sensibilité du fichier) dans les deux modes ; clé d'invitation en mode local seulement
- [x] 4.2 `AdminController` : indicateur de mode, compte rendu de l'import, messages
- [x] 4.3 `app.css` : styles de la section de sauvegarde

## 5. Tests

- [x] 5.1 Export : en-tête, toutes les tables, grand volume sur plusieurs lots sans doublon, `no-store` et nom daté
- [x] 5.2 Aller-retour : export puis import, mêmes lignes, valeurs et identifiants ; liens publics conservés ; création après import avec identifiant supérieur
- [x] 5.3 Fichiers invalides sans modification de la base : version, mode, tronqué, table ou colonne inconnue, référence manquante, type ou longueur invalides, trop gros
- [x] 5.4 Garde-fous : administrateur absent ou sans rôle administrateur dans le fichier (les deux modes), confirmation absente ou incorrecte, import concurrent en 409
- [x] 5.5 Droits et CSRF : 403 pour un non-administrateur et sans jeton CSRF, administrateur toujours connecté après l'import (mode `cloudflare` avec le jeton de test)
- [x] 5.6 Atomicité : une erreur à l'écriture annule la suppression
- [x] 5.7 Aucune donnée du fichier dans les journaux
- [x] 5.8 Vérifier que le reste de la suite passe

## 6. Livraison

- [x] 6.1 Mettre à jour `README.md` (sauvegarde et restauration, wipe total, garde-fou, limite de 100 Mo, données sensibles du fichier, secret HMAC à conserver)
- [x] 6.2 Mettre à jour `CHANGELOG.md` et incrémenter la version dans `pom.xml` et le changelog (indiquer qu'aucune variable d'environnement n'est ajoutée)
- [x] 6.3 Exécuter la suite de tests avec Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
