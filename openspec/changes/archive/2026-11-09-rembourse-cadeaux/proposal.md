## Why

Les achats de cadeaux sont payés avec le compte courant, alors que l'argent a été épargné pour cela. Le propriétaire doit savoir ce qu'il lui reste à virer de son compte épargne vers son compte courant, et noter quand il l'a fait, sans tenir un tableau à côté.

## What Changes

- Un cadeau peut être marqué **remboursé** avec la date du jour. Seuls les cadeaux qui ont réellement quitté le compte courant peuvent l'être : statut **Commandé ou plus**, **avec un prix**, **pas pour un tiers** (la règle du « Déjà déboursé » du dashboard).
- Une **pastille** dans la ligne du cadeau (propriétaire seulement) coche ou décoche le remboursement : « € à rembourser » en gris, « remboursé le 03/12 » en vert. Le changement met à jour la ligne et la carte par HTMX.
- Une carte **« À rembourser »** du dashboard (propriétaire seulement) affiche ce qui reste à virer et ce qui est déjà remboursé. Elle propose **« Tout marquer remboursé »** : le montant « À virer : X € » est affiché, la date est préremplie à aujourd'hui (jamais dans le futur), et tous les cadeaux éligibles non remboursés sont marqués d'un coup.
- Un filtre **« À rembourser »** apparaît dans la ligne des filtres, pour le propriétaire seulement : il liste les cadeaux éligibles non remboursés et se combine avec un tag. Il est ignoré pour un lecteur.
- Le **prix** d'un cadeau remboursé ne peut plus être modifié : le champ est en lecture seule et le serveur refuse le changement.
- Quand un cadeau remboursé cesse d'être éligible (retour à « À acheter », suppression du cadeau, passage « pour un tiers », suppression de sa personne), sa date de remboursement est effacée et une **alerte « virement en trop »** est créée. L'alerte reste affichée sur le dashboard du propriétaire **jusqu'à ce qu'il la ferme**.
- Rien de tout cela n'est visible d'un lecteur ni envoyé dans le HTML qui lui est destiné.

## Capabilities

### New Capabilities
- `reimbursements`: marquage des cadeaux remboursés, carte « À rembourser », remboursement groupé, filtre, alertes de virement en trop, confidentialité.

### Modified Capabilities
<!-- Aucune : les exigences existantes des autres capabilities ne changent pas. -->

## Impact

- Données : colonne `cadeau.rembourse_le` (date, facultative) et table `alerte_remboursement` ; mise à jour de `docs/database.puml`. Compatible SQLite et PostgreSQL, ajout sur bases existantes sans valeur obligatoire.
- Code : `Cadeau`, `CadeauRepository`, `CadeauService`, `PersonneService`, nouveau service d'alertes, contrôleurs de cadeaux et de liste, `DashboardService`, vues, gabarits (`liste.html`, `liste-fragments.html`, `cadeau-form.html`), `app.css`.
- Configuration : propriété `lutin.fuseau-horaire` (défaut `Europe/Paris`) pour la date du jour.
- Tests : service, web, confidentialité et règles de sortie du déboursé.
- Documentation : `README.md`, `CHANGELOG.md`, version du `pom.xml`.
