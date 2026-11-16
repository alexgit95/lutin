## 1. Dashboard

- [x] 1.1 Harmoniser la structure et les largeurs des colonnes entre groupes, en préservant la cellule des actions et le chargement progressif.
- [x] 1.2 Permettre le retour à la ligne des noms longs, y compris sans espaces, et adapter les lignes aux écrans étroits sans perdre l'accès aux informations ni aux actions.
- [x] 1.3 Adapter la barre fixe des cinq statuts et l'espace inférieur du dashboard aux petits écrans et aux zones de sécurité.
- [x] 1.4 Vérifier les vues propriétaire et lecteur, les cadeaux masqués, les lots HTMX et les changements de statut sans régression de confidentialité ni des compteurs.

## 2. Version en administration

- [x] 2.1 Alimenter toutes les réponses de la page d'administration avec la version et le SHA issus des `BuildProperties` optionnelles, avec les valeurs de repli prévues.
- [x] 2.2 Afficher la version et le SHA abrégé dans la page d'administration sans modifier les droits d'accès.
- [x] 2.3 Ajouter les tests JUnit des métadonnées présentes, absentes ou partielles, du SHA `unknown`, du rendu après génération de clé et du refus aux non-administrateurs.

## 3. Documentation et validation

- [x] 3.1 Mettre à jour le README, incrémenter la version Maven et documenter les changements dans le changelog selon les règles du projet.
- [x] 3.2 Exécuter les tests ciblés puis les tests pertinents avec le binaire Maven imposé par le projet.
- [x] 3.3 Vérifier dans un navigateur les largeurs 320, 375 et 768 px ainsi qu'un écran desktop : alignement, titres longs, absence de défilement horizontal, barre visible et dernier cadeau accessible.
- [x] 3.4 Vérifier les interactions et les compteurs après chargement de lots et changement de statut, en vues propriétaire et lecteur, y compris sur mobile.