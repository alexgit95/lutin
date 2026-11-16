# Admin Version

## Purpose
Identifier le build déployé depuis la page d'administration.

## Requirements

### Requirement: Identification de la version déployée
La page d'administration SHALL afficher la version Maven du build exécuté et le SHA du commit embarqué, sous la forme « version - SHA ». Le SHA valide SHALL être raccourci à ses sept premiers caractères. L'information SHALL être accessible uniquement aux administrateurs et SHALL provenir des métadonnées du build exécuté.

#### Scenario: Build identifié
- **WHEN** un administrateur ouvre la page d'administration d'un build disposant d'une version et d'un SHA
- **THEN** la page affiche la version Maven suivie des sept premiers caractères du SHA

#### Scenario: Retour après génération de clé
- **WHEN** un administrateur génère une clé d'API et que la page d'administration est rendue directement en réponse
- **THEN** la version et le SHA restent affichés

#### Scenario: Accès non administrateur
- **WHEN** un utilisateur non administrateur tente d'accéder à la page d'administration
- **THEN** l'accès à la page et à son identification du build est refusé

### Requirement: Métadonnées de build indisponibles
La page d'administration SHALL rester utilisable lorsque les métadonnées de build sont absentes ou incomplètes, et SHALL afficher une indication explicite pour chaque information manquante. La valeur par défaut `unknown` du commit SHALL être traitée comme indisponible.

#### Scenario: Commit non renseigné
- **WHEN** la version du build est connue mais que le commit est absent ou vaut `unknown`
- **THEN** la page affiche la version suivie de « inconnu »

#### Scenario: Informations de build absentes
- **WHEN** aucune métadonnée de build n'est disponible
- **THEN** la page affiche « inconnue - inconnu » et les fonctions d'administration restent accessibles