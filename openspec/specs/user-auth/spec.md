# User Authentication

## Purpose
Define registration, administrator privileges and authentication: form login with an invitation key in local mode, Cloudflare Access with a chosen nickname in production.

## Requirements

### Requirement: Inscription par clé d'invitation
En mode d'accès `local` (hors production), le système SHALL permettre de s'inscrire avec un login unique, un mot de passe et la clé d'invitation configurée dans la variable d'environnement `INVITATION_KEY`. En mode `cloudflare`, l'inscription et la clé d'invitation n'existent pas.

#### Scenario: Inscription valide
- **WHEN** un visiteur soumet un login libre, un mot de passe et la bonne clé en mode local
- **THEN** le compte est créé et le mot de passe est stocké haché

#### Scenario: Clé invalide
- **WHEN** un visiteur soumet une clé incorrecte en mode local
- **THEN** l'inscription est refusée et aucun compte n'est créé

#### Scenario: Login déjà utilisé
- **WHEN** un visiteur soumet un login déjà existant en mode local
- **THEN** l'inscription est refusée avec un message explicite

#### Scenario: Inscription absente en production
- **WHEN** un visiteur demande `/register` en mode cloudflare
- **THEN** la réponse est 404

### Requirement: Premier inscrit administrateur
En mode `local`, le système SHALL attribuer le rôle administrateur au premier compte créé, et à lui seul, y compris en cas d'inscriptions simultanées. En mode `cloudflare`, la même règle s'applique au premier pseudo créé.

#### Scenario: Premier compte
- **WHEN** aucun utilisateur n'existe et qu'une inscription valide est soumise
- **THEN** le compte créé est administrateur

#### Scenario: Comptes suivants
- **WHEN** un utilisateur existe déjà et qu'une nouvelle inscription valide est soumise
- **THEN** le nouveau compte n'est pas administrateur

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
### Requirement: Connexion par session
En mode `local`, le système SHALL authentifier les utilisateurs par session cookie avec protection CSRF et réserver toutes les pages applicatives aux utilisateurs connectés. En mode `cloudflare`, l'authentification suit la capacité `cloudflare-access` et la protection CSRF reste active.

#### Scenario: Connexion réussie
- **WHEN** un utilisateur soumet un login et un mot de passe valides en mode local
- **THEN** une session est ouverte et il est redirigé vers son dashboard

#### Scenario: Page protégée sans session
- **WHEN** un visiteur non connecté demande une page applicative en mode local
- **THEN** il est redirigé vers la page de connexion

#### Scenario: Protection CSRF en production
- **WHEN** une requête de modification est envoyée sans jeton CSRF en mode cloudflare
- **THEN** elle est refusée (403)
