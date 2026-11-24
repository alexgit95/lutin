## Context

Une personne appartient à une seule liste et ne possède aujourd'hui aucun état d'avancement. L'onglet Personnes est réservé au propriétaire. Le dashboard regroupe les cadeaux visibles par destinataire et charge les résultats par lots ; un lecteur reçoit une vue expurgée des cadeaux masqués. Les sauvegardes JSON décrivent explicitement les colonnes et n'acceptent que la version courante du format.

## Goals / Non-Goals

**Goals:**

- Enregistrer par personne et par liste un état manuel, réversible, dont la valeur initiale est non terminée.
- Fournir au propriétaire un compteur global et une indication lisible dans les groupes de cadeaux, sans transmettre cet état au lecteur.
- Conserver cet état dans les sauvegardes de la nouvelle version et le restaurer à l'identique.
- Préserver les données existantes et la compatibilité des deux profils de base de données.

**Non-Goals:**

- Déduire l'état des statuts des cadeaux, modifier ces statuts ou interdire l'ajout de cadeaux.
- Afficher au lecteur un indicateur, un compteur ou une donnée HTML permettant de déduire cet état.
- Changer le mécanisme de versionnement des sauvegardes ou garantir l'import d'anciens formats.
- Ajouter au dashboard des groupes vides pour les personnes sans cadeau ou exclues par le filtre.

## Decisions

### État rattaché à la personne de la liste

Ajouter un booléen persistant à `Personne` plutôt que déduire la complétion des cadeaux : le propriétaire décide lui-même que tous les cadeaux sont présents, indépendamment de leur statut. Les nouvelles personnes, y compris importées depuis une autre liste, commencent non terminées ; les personnes déjà présentes ne changent pas. Une modification explicite du propriétaire est la seule action qui change l'état.

Les personnes existantes doivent aussi être lues comme non terminées après mise à jour. La solution de stockage et de valeur par défaut doit être vérifiée sur des bases SQLite et PostgreSQL déjà peuplées, sans supposer qu'Hibernate peut ajouter une colonne non nullable à une table existante. Mettre à jour le diagramme des données.

### Actions explicites et contrôles serveur

Le menu de chaque personne propose « Marquer comme terminé » ou « Marquer comme à compléter » selon l'état courant. Une requête protégée par CSRF fixe explicitement la valeur demandée ; le service contrôle la propriété de la liste et l'appartenance de la personne. Cette approche évite qu'un double envoi inverse involontairement l'état, contrairement à un simple basculement.

### Indicateurs du dashboard

Calculer le nombre total de personnes et le nombre de personnes terminées à l'échelle de la liste, indépendamment du tag et des lots, par des requêtes d'agrégation plutôt que par chargement massif. Présenter le compteur dans l'en-tête du dashboard du propriétaire, y compris `0 / 0` pour une liste vide. Une personne sans cadeau contribue au compteur mais ne crée pas de groupe vide.

Pour chaque groupe de cadeaux du propriétaire, afficher près du nom une coche et le texte « Tous les cadeaux dans la liste » lorsqu'il est terminé, avec une bordure verte. Les groupes non terminés gardent leur présentation habituelle. L'indicateur doit aussi fonctionner sur les lots HTMX, y compris si un groupe traverse deux lots, et rester lisible sur mobile sans dépendre de la couleur.

Les vues et fragments destinés au lecteur ne doivent contenir ni compte, ni état, ni indicateur, ni attribut HTML permettant de le déduire, y compris pour les cadeaux masqués. Ne pas s'appuyer uniquement sur un masquage CSS ; les requêtes de mutation restent réservées au propriétaire.

### Sauvegarde et restauration

Ajouter la colonne aux descripteurs de transfert et incrémenter la version du format, conformément aux règles existantes. Vérifier l'aller-retour des deux états sur SQLite et PostgreSQL lorsque les environnements sont disponibles, ainsi que l'API de sauvegarde qui partage le même export.

Les anciennes sauvegardes d'une autre version restent refusées conformément à la spécification actuelle ; leur prise en charge nécessiterait un changement séparé du contrat de transfert.

## Risks / Trade-offs

- **Base existante** : une colonne obligatoire peut échouer lors d'une mise à niveau SQLite ou laisser des valeurs nulles → vérifier la mise à niveau réelle sur les deux moteurs et prévoir une valeur initiale compatible sans perte de données.
- **Confidentialité** : un compteur ou un attribut diffusé à un lecteur peut révéler l'état d'une personne → contrôler les réponses HTML initiales et les lots HTMX, y compris le groupe neutre des cadeaux masqués.
- **Pagination** : une personne peut apparaître à la frontière de deux lots → conserver la fusion des groupes et tester l'indication après chargement et changement de statut d'un cadeau.
- **Anciennes sauvegardes** : l'incrément de version rend les exports antérieurs incompatibles avec l'import actuel → le documenter clairement avant déploiement ; ne pas prétendre qu'ils sont restaurables par la nouvelle version.

## Migration Plan

Avant déploiement, conserver une sauvegarde et vérifier le comportement de mise à jour sur des copies SQLite et PostgreSQL contenant des personnes. Initialiser les personnes existantes à non terminées sans toucher aux autres données, puis vérifier un export et une restauration du nouveau format. En cas de retour arrière, restaurer avec la version compatible de l'application et sa sauvegarde correspondante ; ne pas supposer que l'ancien logiciel sait lire le nouveau format.

## Open Questions

Aucune pour le périmètre convenu ; la compatibilité des sauvegardes d'anciennes versions reste hors périmètre.