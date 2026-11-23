# Paramétrage GitHub

Modifications effectuées le 23 novembre 2026 sur le dépôt `alexgit95/lutin` pour préparer l'automerge Renovate.

## Protection de la branche `main`

Dans **Settings > Branches**, une règle classique s'applique à `main` :

- Une pull request est obligatoire avant fusion ; aucune approbation n'est exigée par cette règle.
- Le contrôle GitHub Actions `test-and-coverage` doit réussir et la branche de la pull request doit être à jour avec `main`.
- L'option **Do not allow bypassing the above settings** interdit le contournement de cette règle.

Cette protection s'applique également aux pull requests qui ne viennent pas de Renovate et empêche les modifications directes de `main` dans le cadre de la règle. Avant sa création, les écrans du dépôt ne montraient aucun ruleset local ni aucune règle classique de protection. Les éventuelles règles héritées n'ont pas été vérifiées.

## Automerge GitHub

Dans **Settings > General > Pull Requests**, l'option **Allow auto-merge** a été activée. GitHub peut ainsi fusionner une pull request configurée pour l'automerge lorsque les exigences applicables sont satisfaites.

## Renovate et vérifications

- L'installation Renovate avait déjà accès à `alexgit95/lutin` et aux permissions de lecture et écriture sur le code et les pull requests. Ces permissions ont été vérifiées, mais pas modifiées.
- La configuration du dépôt prévoit l'automerge des mises à jour `minor` et `patch` ; aucune nouvelle règle d'automerge n'est ajoutée pour `pin`, `pinDigest` ou `digest`, et les mises à jour majeures restent manuelles. Elle prend effet une fois fusionnée dans la branche par défaut.
- Sur la pull request de configuration, le contrôle `test-and-coverage` a été vu comme obligatoire et réussi. Le comportement d'une pull request Renovate avec un contrôle absent, en attente ou échoué, puis sa fusion automatique après réussite, reste à vérifier en conditions réelles.
- Le validateur officiel Renovate et la configuration résolue des presets n'ont pas été vérifiés localement, le registre npm étant inaccessible au moment de la modification.
- Le workflow de déploiement n'a pas été modifié. Les tests de la pull request ne construisent pas l'image Docker ; le job de construction et publication s'exécute sur un push, notamment après fusion sur `main`.