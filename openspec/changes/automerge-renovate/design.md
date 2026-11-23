## Context

`renovate.json` etend `config:recommended`. Le workflow GitHub existant execute `test-and-coverage` sur les pull requests vers `main` ; le job de publication Docker ne s'execute que lors d'un push. L'etat des protections GitHub, les permissions et les exemptions ne sont pas connus depuis le depot.

## Goals / Non-Goals

**Goals:**

- Automatiser les PR Renovate `minor` et `patch` apres reussite du controle requis.
- Exiger `test-and-coverage` sur `main` avant toute fusion, sans exception pour Renovate.

**Non-Goals:**

- Activer une nouvelle regle d'automerge pour `pin`, `pinDigest` ou `digest`, automatiser les majeures, modifier les workflows ou changer la strategie de deploiement.
- Pretendre que les tests Maven valident l'image Docker ou un deploiement.

## Decisions

### PR Renovate limitees aux mises a jour demandees

Ajouter une `packageRules` avec `matchUpdateTypes` limite a `minor` et `patch` et `automerge: true`, en conservant les presets existants. Garder l'automerge par PR, sans `ignoreTests` et sans regle nouvelle pour `pin`, `pinDigest` ou `digest`. Verifier la configuration resolue, y compris les groupes et les regles heritees, afin que les majeures restent manuelles. Les mises a jour mineures ou correctives peuvent neanmoins casser une compatibilite, notamment en version 0.x ; les tests ne sont pas une garantie absolue.

### Controle obligatoire cote GitHub

Avant toute activation effective de l'automerge, verifier dans les protections ou rulesets applicables a `main` que la fusion exige la reussite du check exact emis par le job `test-and-coverage`, que la regle s'applique aux PR et qu'aucun acteur, y compris Renovate, ne la contourne. Exiger une branche a jour avant fusion si le reglage existant le permet, pour eviter de fusionner un resultat teste contre une ancienne base. Identifier d'eventuelles regles plus strictes et les conserver. Verifier aussi que les autorisations et conditions d'auto-merge sont compatibles sans supprimer une revue obligatoire ni elargir les droits.

Le reglage Renovate seul ne garantit pas l'attente de tests quand la plateforme gere l'automerge. Ne pas mettre en service la configuration tant que la protection et son controle exact ne sont pas verifies ; si les droits manquent ou si les parametres ne peuvent pas etre constates, consigner le blocage et demander l'intervention d'un administrateur. Toute modification de protection est une action externe distincte qui requiert l'autorisation appropriee ; ne pas modifier silencieusement les regles existantes.

## Risks / Trade-offs

- [Check requis absent ou contournable] -> Verification effective du check, des exemptions et des etats en attente/echec avant activation.
- [CI verte mais image Docker ou comportement de production non valide] -> Documenter cette limite ; aucun changement de pipeline dans ce perimetre.
- [Mise a jour classee mineure ou corrective mais incompatible] -> Maintenir les controles existants et la possibilite de revenir en arriere ; les tests ne couvrent pas tous les risques.
- [Permissions, regles de revue ou indisponibilite d'une PR] -> Ne pas contourner les protections ; laisser les verifications reelles non validees et signaler le blocage.

## Migration Plan

Verifier d'abord les protections et l'autorisation d'administration, puis configurer Renovate et controler une PR eligible. En cas d'incident, desactiver l'automerge dans la configuration Renovate sans supprimer la protection GitHub. Les essais externes ou actions sur une PR reelle ne sont executes qu'avec les autorisations necessaires.

## Open Questions

- Quels rulesets, protections, exceptions et droits s'appliquent actuellement a `main` ? Cette question doit etre resolue avant activation.