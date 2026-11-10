## MODIFIED Requirements

### Requirement: Écran d'administration
Le système SHALL réserver l'écran d'administration aux administrateurs dans tous les modes. Il SHALL proposer la section de sauvegarde (export et import de la base) dans les deux modes. En mode `local`, il SHALL aussi afficher la clé d'invitation en lecture seule ; en mode `cloudflare`, il SHALL ne montrer aucune clé d'invitation.

#### Scenario: Accès administrateur
- **WHEN** l'administrateur ouvre l'écran d'administration en mode local
- **THEN** la clé d'invitation et la section de sauvegarde sont affichées

#### Scenario: Accès administrateur en production
- **WHEN** l'administrateur ouvre l'écran d'administration en mode cloudflare
- **THEN** la section de sauvegarde est affichée, sans clé d'invitation

#### Scenario: Accès refusé
- **WHEN** un utilisateur non administrateur accède à l'écran d'administration
- **THEN** l'accès est refusé (403)
