## Why

Un cadeau masqué laisse aujourd'hui son nom visible aux lecteurs, et les lecteurs voient ces lignes éparpillées entre les groupes de personnes (leur place révèle le rang alphabétique du destinataire). Le filtre par tag révèle aussi les tags d'un cadeau masqué. Le masquage doit être total pour tout lecteur : seule l'existence du cadeau est visible.

## What Changes

- Un lecteur voit tout comme le propriétaire sauf les cadeaux masqués, réduits à une ligne « Cadeau masqué » : plus de nom, d'URL, de prix, de statut, de tags ni de destinataire. **BREAKING** : le nom d'un cadeau masqué n'est plus envoyé aux lecteurs.
- Pour un lecteur, tous les cadeaux masqués forment un seul groupe neutre placé après les autres groupes, trié par identifiant, jamais par nom de personne ou de cadeau. Le tri du propriétaire est inchangé.
- Pour un lecteur, un cadeau masqué est traité comme s'il n'avait aucun tag : un filtre par tag ne le renvoie jamais.
- Inchangés : les montants (budget, déboursé, reste à payer) incluent les cadeaux masqués, et la barre de workflow les exclut pour un lecteur.
- Limite connue conservée : avec un seul cadeau masqué, les changements de statut et de montants permettent d'en déduire le prix.

## Capabilities

### New Capabilities
<!-- Aucune -->

### Modified Capabilities
- `list-sharing`: la ligne masquée ne porte plus le nom ; groupe unique en fin de liste, tri indépendant des noms, aucune fuite dans le HTML.
- `dashboard`: le groupe des cadeaux masqués d'un lecteur est unique, en dernière position, sans nom ni statut.
- `tags`: pour un lecteur, le filtre par tag ignore les cadeaux masqués.

## Impact

- Code : `VueCadeau`, `DashboardService`, `CadeauRepository` (requêtes et tri propres au lecteur), `liste-fragments.html`.
- Aucun changement de schéma ni d'API REST.
- Tests : `ListesWebTest` et `ParcoursListesTest` (le nom n'est plus visible pour un lecteur), nouveaux tests de tri et de filtre.
- Documentation : `README.md`, `CHANGELOG.md`, version du `pom.xml`.
