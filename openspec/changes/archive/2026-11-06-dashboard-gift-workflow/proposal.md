## Why

Le dashboard présente actuellement tous les cadeaux dans un tableau paginé et les répartitions de statuts sous forme de pastilles séparées. Regrouper les cadeaux par personne, permettre de parcourir la liste sans contrôles de pagination visibles et rendre le workflow des statuts plus lisible donnera une vue plus naturelle et un retour immédiat lors d’un changement de statut.

## What Changes

- Regrouper les cadeaux du dashboard par personne destinataire.
- Remplacer la pagination visible par un chargement continu au défilement, tout en conservant le chargement des cadeaux par lots côté serveur.
- Présenter les nombres de cadeaux par statut dans une barre compacte persistante, indépendante du filtre par tag.
- Animer le déplacement visuel d’un cadeau vers son nouveau statut et l’évolution des compteurs lors d’un changement de statut, sans déplacer durablement le cadeau hors de son groupe.
- Respecter les règles de visibilité des cadeaux masqués et le réglage de réduction des animations du navigateur.

## Capabilities

### New Capabilities

Aucune.

### Modified Capabilities

- `dashboard`: grouper les cadeaux par personne, parcourir les résultats en continu sans pagination visible et afficher un workflow persistant avec compteurs animés, sans dépendance au filtre par tag.

## Impact

- Vue et contrôleur du dashboard, service et projections de données des cadeaux.
- Template du dashboard, styles et JavaScript de progressive enhancement pour le chargement continu et les animations.
- Tests du service et des vues web concernant le regroupement, les lots, les compteurs, les rôles lecteur/propriétaire et la confidentialité des cadeaux masqués.
- Aucun changement de schéma de base de données ni nouvelle dépendance prévus.
