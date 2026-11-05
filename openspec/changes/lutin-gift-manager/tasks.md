## 1. Socle projet

- [x] 1.1 Nettoyer le `pom.xml` : renommer (nom, description), retirer JJWT et `webclient`, ajouter Thymeleaf, Thymeleaf Spring Security, Jsoup, driver PostgreSQL ; incrémenter la version
- [x] 1.2 Créer la classe principale et l'arborescence de packages (`com.alexgit95.lutin`)
- [x] 1.3 Créer `application.properties` (profil par défaut `dev`), `application-dev.properties` (SQLite, `ddl-auto=update`) et `application-prod.properties` (PostgreSQL, cookies `Secure`, `X-Forwarded-*`)
- [x] 1.4 Mettre à jour `Dockerfile` et `docker-compose.yml` (app + PostgreSQL, variables `INVITATION_KEY`, base de données), sans toucher aux workflows GitHub
- [x] 1.5 Réécrire `.github/copilot-instructions.md` pour deux profils (`dev` SQLite, `prod` Docker + PostgreSQL) et le contexte Thymeleaf/HTMX

## 2. Modèle de données

- [x] 2.1 Créer les entités `Utilisateur`, `ListeAchat`, `Personne`, `Cadeau`, `Tag`, `Partage` (types compatibles SQLite et PostgreSQL) et l'enum `Statut`
- [x] 2.2 Créer les repositories avec pagination là où nécessaire
- [x] 2.3 Créer `docs/database.puml` à jour avec le schéma
- [x] 2.4 Tests unitaires du modèle et des repositories (SQLite)

## 3. Authentification et administration

- [x] 3.1 Configurer Spring Security : session cookie, CSRF, BCrypt, pages publiques (connexion, inscription), reste protégé
- [x] 3.2 Implémenter le service d'inscription : contrôle de la clé `INVITATION_KEY`, unicité du login, premier inscrit administrateur de façon atomique
- [x] 3.3 Créer les pages de connexion et d'inscription
- [x] 3.4 Créer l'écran d'administration affichant la clé, réservé à l'admin
- [x] 3.5 Tests : clé invalide, login dupliqué, premier admin, concurrence d'inscriptions, accès 403 à l'écran admin

## 4. Listes, personnes et cadeaux

- [x] 4.1 Service et contrôleur des listes d'achat avec contrôle du propriétaire
- [x] 4.2 Service et contrôleur des personnes dans un onglet dédié (création, budget, suppression avec confirmation), hors dashboard
- [x] 4.3 Service et contrôleur des cadeaux : CRUD, prix facultatif, masqué, pour compte de tiers
- [x] 4.4 Service de récupération du titre depuis l'URL avec garde-fous SSRF (schémas, IP privées, timeout, taille, redirections)
- [x] 4.5 Workflow de statut et endpoint HTMX de changement de statut
- [x] 4.6 Tests : droits, statuts, SSRF (URL interdites, échec de récupération), règles de gestion

## 5. Tags

- [x] 5.1 Service et contrôleur des tags par liste, affectation aux cadeaux
- [x] 5.2 Filtrage des cadeaux par tag
- [x] 5.3 Tests : affectation, tag d'une autre liste refusé, filtre

## 6. Dashboard

- [x] 6.1 Service de calcul : budget global, déboursé, reste à payer, répartition par statut (exclusion des cadeaux pour compte de tiers)
- [x] 6.2 Page dashboard Thymeleaf : cartes KPI, liste des cadeaux, pastilles de statut, changement de statut en HTMX
- [x] 6.3 CSS maison inspiré de la maquette, adapté au mobile (navigation basse)
- [x] 6.4 Tests du service de calcul (exemple de la spec, prix absent, cadeaux masqués inclus)

## 7. Partage en lecture seule

- [x] 7.1 Service et page de partage par liste (liste déroulante des logins, ajout et retrait)
- [x] 7.2 Service de projection : cadeaux masqués réduits à leur nom pour les lecteurs, aucun champ masqué dans le HTML
- [x] 7.3 Refuser toute écriture d'un lecteur (403) et afficher les listes partagées
- [x] 7.4 Tests : lecture seule, masquage, totaux inchangés, retrait du partage

## 8. PWA

- [x] 8.1 Ajouter `manifest.webmanifest`, icônes et lien dans les templates
- [x] 8.2 Ajouter un service worker minimal sans cache hors ligne et son enregistrement
- [x] 8.3 Vérifier la présence du manifeste et du service worker via un test d'intégration

## 9. Documentation et finalisation

- [x] 9.1 Rédiger le `README.md` fonctionnel (fonctionnalités, profils, variables d'environnement, lancement)
- [x] 9.2 Créer le `CHANGELOG.md` (format Keep a Changelog, version alignée sur le pom)
- [x] 9.3 Lancer la compilation et tous les tests avec le Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
- [ ] 9.4 Valider le démarrage en `dev` (fait : démarrage et pages publiques OK) et la construction de l'image Docker (à faire : Docker absent de ce poste)
