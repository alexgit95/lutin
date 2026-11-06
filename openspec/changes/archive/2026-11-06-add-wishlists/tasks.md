## 1. Modèle de données

- [x] 1.1 Créer les entités `Wishlist` (avec `nom_cle`), `Souhait`, `WishlistPartage` et `Avis` (types et SQL compatibles SQLite et PostgreSQL, enum `TypeAvis`)
- [x] 1.2 Créer les repositories, dont les requêtes `@Modifying` de prise et de décochage, les suppressions groupées et l'index sur `avis(destinataire_id)`
- [x] 1.3 Mettre à jour `docs/database.puml`

## 2. Accès et wishlists

- [x] 2.1 Créer `AccesWishlistService` (propriétaire / lecteur, refus identique pour inexistant et inaccessible, aucun cas admin)
- [x] 2.2 Créer `WishlistService` : création, renommage, suppression avec unicité du nom par propriétaire
- [x] 2.3 Tests unitaires : droits, nom vide, doublon (casse et espaces), même nom chez deux propriétaires, suppression en cascade

## 3. Articles

- [x] 3.1 Créer `SouhaitService` : ajout, modification, suppression, validation (nom, URL http/https, prix >= 0), pagination
- [x] 3.2 Brancher la récupération du titre via `UrlTitleService`
- [x] 3.3 Créer les vues DTO distinctes propriétaire (sans « pris ») et lecteur (avec preneur)
- [x] 3.4 Tests unitaires : validations, tri indépendant de l'état « pris », DTO propriétaire sans donnée du preneur

## 4. Partage

- [x] 4.1 Créer `WishlistPartageService` : lister, candidats (hors propriétaire et déjà partagés), partager, retirer
- [x] 4.2 Au retrait, supprimer les avis du lecteur pour cette wishlist en laissant les articles « pris par »
- [x] 4.3 Tests unitaires : partage avec soi-même, doublon, retrait, accès refusé au lecteur et à l'admin

## 5. Prise et décochage

- [x] 5.1 Implémenter la prise atomique (login copié dans `pris_par_nom`, 403 propriétaire, message avec le preneur actuel en cas de concurrence)
- [x] 5.2 Implémenter le décochage avec paramètre `confirme` : libération directe si login = preneur, sinon fragment d'avertissement important nommant le preneur
- [x] 5.3 Purger les avis `MODIFIE` de l'article au décochage
- [x] 5.4 Tests unitaires : prise, prise concurrente, propriétaire refusé, décochage propre, décochage d'un autre sans et avec confirmation, lecteur non identifié

## 6. Avis

- [x] 6.1 Créer `AvisService` avec un `Clock` injecté : génération `MODIFIE` (sans doublon), `SUPPRIME` (remplace les `MODIFIE`), `WISHLIST_SUPPRIMEE` (remplace les avis d'articles, seulement si article pris), destinataires = partages uniquement
- [x] 6.2 Appeler `AvisService` depuis la modification et suppression d'article, et la suppression de wishlist, dans la même transaction
- [x] 6.3 Lister, compter et fermer les avis avec filtre d'expiration à 90 jours et purge à l'affichage de la page des wishlists
- [x] 6.4 Tests unitaires : article libre sans avis, modifications répétées, suppression, suppression de wishlist avec et sans article pris, fermeture, expiration à 90 jours

## 7. Interface web

- [x] 7.1 Ajouter l'entrée « Wishlists » et le compteur d'avis dans la barre (`fragments.html`, `LayoutAdvice`)
- [x] 7.2 Créer les contrôleurs MVC et templates Thymeleaf/HTMX : page des wishlists (deux sections et avis), page de wishlist (vue propriétaire, vue lecteur), formulaire d'article, partages
- [x] 7.3 Afficher l'avertissement de décochage (page de confirmation) et le bouton « OK » des avis (formulaires classiques, sans HTMX)
- [x] 7.4 Tests web : 403 pour lecteur sur édition, pour propriétaire et admin sur prise, absence de toute donnée « pris », preneur ou avis dans le HTML et les fragments du propriétaire

## 8. Livraison

- [x] 8.1 Mettre à jour `README.md` (fonctionnalité wishlists, règles d'avis et d'aveuglement)
- [x] 8.2 Mettre à jour `CHANGELOG.md` et incrémenter la version dans `pom.xml` et le changelog
- [x] 8.3 Exécuter la suite de tests avec Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`

## 9. Onglets et tuiles

- [x] 9.1 Déplacer renommer et supprimer dans un onglet Réglages et ne montrer que les articles sur la page principale
- [x] 9.2 Rendre les articles en tuiles, avec rendu distinct des articles pris pour les lecteurs
- [x] 9.3 Tests web et service, README, CHANGELOG et version 0.3.1
- [x] 9.4 Landing page à deux grandes tuiles (listes d'achat sur /listes, wishlists) et tests
