## 1. Protection GitHub prealable

- [ ] 1.1 Identifier le controle exact emis par le job `test-and-coverage`, les protections et rulesets effectifs de `main`, leurs exemptions et les droits de Renovate, sans modifier les reglages.
- [x] 1.2 Verifier que la reussite de ce controle est exigee avant toute fusion et qu'aucune exemption ne permet de le contourner ; conserver les protections et revues existantes.
- [x] 1.3 Si la protection est absente ou inaccessible, demander l'intervention ou l'autorisation appropriee, etablir la protection avec les permissions necessaires et la reverifier ; laisser cette tache ouverte et ne pas activer l'automerge si elle reste non verifiee.
- [ ] 1.4 Verifier avec les moyens et autorisations disponibles qu'un controle absent, en attente ou echoue bloque la fusion ; consigner explicitement tout essai reel non realise, sans provoquer une fusion ou echec de CI non autorise.

## 2. Politique Renovate

- [x] 2.1 Apres validation de la protection, ajouter dans `renovate.json` l'automerge par PR pour `minor` et `patch` uniquement, sans ajouter de regle pour `pin`, `pinDigest` ou `digest`, et sans modifier les workflows.
- [ ] 2.2 Valider la syntaxe et la configuration resolue, y compris les groupes et presets, et verifier que les mises a jour majeures ne sont pas eligible a l'automerge.
- [ ] 2.3 Sur une PR Renovate eligible et avec les autorisations necessaires, verifier l'application de la regle et l'attente de `test-and-coverage` ; consigner les limitations ou une recette reelle non effectuee.

## 3. Documentation et validation

- [x] 3.1 Documenter la politique de fusion, le check obligatoire, le maintien des protections et la limite des tests Maven qui ne construisent pas l'image Docker dans la PR ; mettre a jour le changelog et la version du projet selon les conventions applicables.
- [x] 3.2 Valider strictement les artefacts OpenSpec et verifier que le workflow et la politique de deploiement restent inchanges ; documenter les prerequis GitHub non satisfaits.

## Blocage - 2026-11-23

- Le client GitHub `gh` n'est pas disponible et le navigateur integre n'est pas connecte a GitHub : la consultation des protections du depot `alexgit95/lutin` renvoie une page 404.
- Les premieres captures montraient l'absence de ruleset de depot et de regle classique. L'administrateur a ensuite configure une regle `main`, visible comme appliquee a une branche : PR obligatoire sans approbation obligatoire, controle GitHub Actions `test-and-coverage` requis, branche a jour et contournement desactive.
- La capture des parametres generaux montre que `Allow auto-merge` est active pour le depot.
- La capture de l'installation Renovate montre que `alexgit95/lutin` fait partie des depots selectionnes et que l'application dispose des droits de lecture et ecriture sur le code et les pull requests.
- Les eventuelles regles heritees et le comportement effectif sur une PR restent a verifier. Aucun essai de fusion n'a ete effectue.
- La syntaxe JSON et la regle `minor`/`patch` sont verifiees localement. Le validateur officiel Renovate n'est pas installe et le registre npm est inaccessible (DNS) : la configuration resolue et les presets restent a valider.