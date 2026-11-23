## 1. Calcul et acces

- [x] 1.1 Ajouter au repository un calcul global par wishlist du total des prix renseignes et du nombre de prix manquants, sans charger tous les articles et sans condition sur les reservations.
- [ ] 1.2 Tester les cas vide, sans prix, prix zero, decimales, plusieurs pages et isolation entre wishlists ; verifier la precision au centime et la compatibilite effective SQLite et PostgreSQL, en signalant tout environnement indisponible.
- [x] 1.3 Integrer le resultat aux DTO proprietaire et lecteur apres controle d'acces, y compris au parcours public, sans exposer de donnees de reservation au proprietaire.

## 2. Presentation et comportement

- [x] 2.1 Afficher en haut des trois vues le total des prix renseignes avec le format monetaire existant et le nombre de prix manquants lorsqu'il est non nul.
- [x] 2.2 Ajouter des tests de service et web pour les trois vues, les pages suivantes, les modifications et suppressions, le prix zero et l'etat vide.
- [x] 2.3 Tester prise et liberation sans variation du total ni fuite de reservation cote proprietaire ; verifier les refus d'acces et les jetons publics invalides.
- [x] 2.4 Verifier le rendu desktop et mobile, notamment les montants longs, sans debordement.

## 3. Documentation et validation

- [x] 3.1 Mettre a jour le README, le changelog et la version du POM conformement aux conventions du depot, sans modifier le schema ou le pipeline.
- [x] 3.2 Executer les tests cibles puis la suite pertinente avec le Maven impose par le projet ; consigner les resultats et les verifications SQLite/PostgreSQL non realisees.
- [x] 3.3 Executer la validation stricte OpenSpec et verifier les changements limites au perimetre.

## Resultats de verification - 2026-11-23

- Tests cibles du repository, des services et des vues reussis sur SQLite ; suite Maven complete : 443 tests, 34 suites, aucun echec, erreur ou test ignore.
- Recette navigateur sur une base SQLite distincte : vues proprietaire, lecteur et publique a 320, 360 et 1280 pixels, total de 9 999 999 999,99 euros et article sans prix ; aucune superposition ou largeur excedentaire detectee dans les mesures DOM, avec captures mobile et desktop.
- PostgreSQL non teste : ni Docker ni executables PostgreSQL locaux disponibles. La tache 1.2 reste ouverte jusqu'a une verification effective sur PostgreSQL.