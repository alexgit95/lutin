## 1. Modèle et attribution du tiers

- [x] 1.1 Ajouter au cadeau la relation facultative vers le tiers et la date distincte de règlement tiers, compatibles SQLite et PostgreSQL ; mettre à jour le diagramme de données.
- [x] 1.2 Ajouter le choix du tiers sur le formulaire et son nom dans la vue propriétaire de la liste ; valider la même liste côté serveur et préserver les cadeaux historiques sans attribution.
- [x] 1.3 Appliquer les protections de suppression des personnes référencées et des cadeaux réglés, ainsi que les protections du prix, de l'attribution et de l'éligibilité après règlement.
- [x] 1.4 Adapter l'export et la restauration aux nouveaux champs ; tester le retour des données et la restauration d'un ancien export sans ces champs.

## 2. Suivi des remboursements reçus

- [x] 2.1 Implémenter les requêtes de total par tiers et de détail paginé (non réglés d'abord), ainsi que le suivi des cadeaux sans tiers identifié.
- [x] 2.2 Ajouter les sous-vues « Épargne » et « Tiers », avec la navigation, le total par personne, le détail et la section « Tiers à préciser ».
- [x] 2.3 Ajouter les actions de règlement intégral et d'annulation, contrôlées côté serveur, avec mise à jour HTMX de la ligne et du total.

## 3. Tests et documentation

- [x] 3.1 Couvrir les scénarios métier et web : attribution, éligibilité, totaux avec pagination, date et annulation, mutations refusées, accès propriétaire et absence de données dans les vues et fragments lecteur.
- [ ] 3.2 Vérifier la non-régression des budgets et de l'épargne et la mise à niveau du schéma sur SQLite et PostgreSQL ; exécuter les tests avec le binaire Maven imposé par le projet.
- [x] 3.3 Vérifier dans un navigateur le parcours d'attribution puis de règlement et d'annulation, sur mobile et desktop.
- [x] 3.4 Documenter le parcours dans le README et incrémenter la version dans le POM et le changelog.