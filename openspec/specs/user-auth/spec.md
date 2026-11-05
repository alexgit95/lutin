# User Authentication

## Purpose
Define invitation-based registration, administrator privileges, and session-based authentication.

## Requirements

### Requirement: Inscription par clé d'invitation
Le système SHALL permettre de s'inscrire avec un login unique, un mot de passe et la clé d'invitation configurée dans la variable d'environnement `INVITATION_KEY`.

#### Scenario: Inscription valide
- **WHEN** un visiteur soumet un login libre, un mot de passe et la bonne clé
- **THEN** le compte est créé et le mot de passe est stocké haché

#### Scenario: Clé invalide
- **WHEN** un visiteur soumet une clé incorrecte
- **THEN** l'inscription est refusée et aucun compte n'est créé

#### Scenario: Login déjà utilisé
- **WHEN** un visiteur soumet un login déjà existant
- **THEN** l'inscription est refusée avec un message explicite

### Requirement: Premier inscrit administrateur
Le système SHALL attribuer le rôle administrateur au premier compte créé, et à lui seul, y compris en cas d'inscriptions simultanées.

#### Scenario: Premier compte
- **WHEN** aucun utilisateur n'existe et qu'une inscription valide est soumise
- **THEN** le compte créé est administrateur

#### Scenario: Comptes suivants
- **WHEN** un utilisateur existe déjà et qu'une nouvelle inscription valide est soumise
- **THEN** le nouveau compte n'est pas administrateur

### Requirement: Écran d'administration
Le système SHALL afficher la clé d'invitation en lecture seule aux administrateurs uniquement.

#### Scenario: Accès administrateur
- **WHEN** l'administrateur ouvre l'écran d'administration
- **THEN** la clé d'invitation est affichée

#### Scenario: Accès refusé
- **WHEN** un utilisateur non administrateur accède à l'écran d'administration
- **THEN** l'accès est refusé (403)

### Requirement: Connexion par session
Le système SHALL authentifier les utilisateurs par session cookie avec protection CSRF et réserver toutes les pages applicatives aux utilisateurs connectés.

#### Scenario: Connexion réussie
- **WHEN** un utilisateur soumet un login et un mot de passe valides
- **THEN** une session est ouverte et il est redirigé vers son dashboard

#### Scenario: Page protégée sans session
- **WHEN** un visiteur non connecté demande une page applicative
- **THEN** il est redirigé vers la page de connexion
