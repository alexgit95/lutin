## 1. Modèle et données

- [x] 1.1 Ajouter l'état persistant de complétion des personnes, initialisé à non terminé, et actualiser `docs/database.puml`.
- [x] 1.2 Vérifier sur des bases SQLite et PostgreSQL déjà peuplées que la mise à jour conserve les données et initialise les personnes existantes à non terminées ; adapter le mécanisme de mise à jour compatible avec les profils du projet.
	- SQLite : mise à jour Hibernate testée sur une base temporaire peuplée. PostgreSQL : vérification réussie confirmée par l'utilisateur.
- [x] 1.3 Tester la création et l'import de personnes, notamment la remise à zéro des personnes importées et la conservation des personnes déjà présentes.

## 2. Actions des personnes

- [x] 2.1 Ajouter une action serveur explicite et idempotente permettant au propriétaire de fixer l'état d'une personne, avec contrôles d'appartenance et protection CSRF.
- [x] 2.2 Ajouter dans le menu de l'onglet Personnes l'action adaptée à l'état courant et une indication lisible des personnes terminées.
- [x] 2.3 Tester les deux transitions, les requêtes répétées, les accès non autorisés, CSRF et l'indépendance vis-à-vis des cadeaux et de leurs statuts.

## 3. Dashboard

- [x] 3.1 Calculer par agrégation les nombres globaux de personnes et de personnes terminées, y compris sans cadeau, indépendamment du filtre et des lots.
- [x] 3.2 Afficher au propriétaire le compteur dans l'en-tête et la coche, le libellé et la bordure verte pour les groupes terminés ; conserver une présentation accessible sur mobile.
- [x] 3.3 Propager l'état aux groupes et lots du propriétaire sans le transmettre au lecteur, y compris dans les réponses HTMX et le groupe des cadeaux masqués.
- [x] 3.4 Tester les compteurs, les listes vides, les filtres, les groupes répartis sur plusieurs lots, les modifications de cadeau et l'absence de fuite dans les réponses lecteur.

## 4. Sauvegarde et restauration

- [x] 4.1 Ajouter l'état au descripteur de la table des personnes et incrémenter la version du format d'export.
- [x] 4.2 Tester l'export administrateur et API ainsi que la restauration des deux états, la validation du format et la conformité des descripteurs au modèle.
- [x] 4.3 Vérifier l'aller-retour et la mise à jour des données sur SQLite et PostgreSQL lorsque les environnements sont disponibles ; signaler toute vérification impossible.

## 5. Documentation et validation

- [x] 5.1 Mettre à jour `README.md` pour décrire l'état manuel, le compteur, l'import de personnes et la compatibilité des sauvegardes.
- [x] 5.2 Mettre à jour `CHANGELOG.md` et incrémenter la version dans `pom.xml` conformément aux conventions du projet.
- [x] 5.3 Exécuter les tests ciblés puis les vérifications pertinentes avec le Maven imposé par le projet ; consigner les éventuelles limites de vérification.