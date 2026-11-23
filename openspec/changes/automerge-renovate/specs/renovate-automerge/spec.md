## ADDED Requirements

### Requirement: Automerge des mises a jour Renovate mineures et correctives
Le systeme SHALL activer l'automerge des PR Renovate de type `minor` et `patch` lorsque les protections applicables sont satisfaites. Il SHALL conserver une validation manuelle pour les mises a jour majeures et SHALL NOT ajouter de regle d'automerge pour `pin`, `pinDigest` ou `digest`.

#### Scenario: Mise a jour eligible
- **WHEN** Renovate propose une PR `minor` ou `patch` et que tous les controles et protections applicables sont satisfaits
- **THEN** la PR est eligible a une fusion automatique

#### Scenario: Mise a jour majeure ou epinglage
- **WHEN** Renovate propose une mise a jour `major`, `pin`, `pinDigest` ou `digest`
- **THEN** la nouvelle regle d'automerge `minor` et `patch` ne s'applique pas et une mise a jour majeure reste soumise a validation manuelle

### Requirement: Tests obligatoires avant fusion sur main
La protection GitHub applicable a `main` SHALL exiger la reussite du controle du job `test-and-coverage` avant toute fusion de PR, y compris les PR Renovate, sans exemption permettant de contourner ce controle. L'automerge SHALL NOT etre mis en service tant que cette exigence n'a pas ete verifiee sur GitHub.

#### Scenario: Controle en attente ou absent
- **WHEN** le controle `test-and-coverage` d'une PR est en attente ou absent
- **THEN** la PR ne peut pas etre fusionnee sur `main`

#### Scenario: Controle echoue
- **WHEN** le controle `test-and-coverage` d'une PR echoue
- **THEN** la PR ne peut pas etre fusionnee sur `main`, y compris automatiquement par Renovate

#### Scenario: Controle reussi
- **WHEN** le controle `test-and-coverage` reussit et que toutes les autres protections applicables sont satisfaites
- **THEN** une PR eligible a l'automerge peut etre fusionnee

#### Scenario: Protection impossible a verifier
- **WHEN** l'acces ou les permissions ne permettent pas de verifier ou d'etablir le controle obligatoire et l'absence de contournement
- **THEN** l'activation effective de l'automerge reste bloquee et l'intervention d'un administrateur est demandee