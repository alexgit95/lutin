## Why

Les mises a jour de dependances mineures et correctives necessitent aujourd'hui une fusion manuelle. Leur fusion automatique apres les tests reduit cette intervention tout en conservant une barriere de verification avant integration.

## What Changes

- Activer l'automerge des PR Renovate classees `minor` et `patch` ; ne pas ajouter de regle d'automerge pour `pin`, `pinDigest` ou `digest`.
- Maintenir une validation manuelle des mises a jour majeures.
- Exiger la reussite du controle GitHub Actions `test-and-coverage` sur `main` avant toute fusion ; verifier la configuration GitHub effective et son application a Renovate.
- Ne pas modifier le workflow de deploiement ni contourner les protections existantes.

## Capabilities

### New Capabilities

- `renovate-automerge`: politique de fusion automatique des dependances et controle de tests obligatoire avant fusion.

### Modified Capabilities

Aucune.

## Impact

- Configuration Renovate du depot et protection de la branche `main` dans GitHub, sous reserve d'acces et d'autorisation d'administration.
- Verification du controle et du comportement reel de Renovate sur une PR eligible ; aucun changement du workflow GitHub Actions prevu.
- Documentation du fonctionnement et des limites, notamment le fait que les tests Maven ne construisent pas l'image Docker dans la PR.