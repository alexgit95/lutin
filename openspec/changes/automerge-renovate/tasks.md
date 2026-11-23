## 1. Protection GitHub prealable

- [ ] 1.1 Identifier le controle exact emis par le job `test-and-coverage`, les protections et rulesets effectifs de `main`, leurs exemptions et les droits de Renovate, sans modifier les reglages.
- [ ] 1.2 Verifier que la reussite de ce controle est exigee avant toute fusion et qu'aucune exemption ne permet de le contourner ; conserver les protections et revues existantes.
- [ ] 1.3 Si la protection est absente ou inaccessible, demander l'intervention ou l'autorisation appropriee, etablir la protection avec les permissions necessaires et la reverifier ; laisser cette tache ouverte et ne pas activer l'automerge si elle reste non verifiee.
- [ ] 1.4 Verifier avec les moyens et autorisations disponibles qu'un controle absent, en attente ou echoue bloque la fusion ; consigner explicitement tout essai reel non realise, sans provoquer une fusion ou echec de CI non autorise.

## 2. Politique Renovate

- [ ] 2.1 Apres validation de la protection, ajouter dans `renovate.json` l'automerge par PR pour `minor` et `patch` uniquement, sans ajouter de regle pour `pin`, `pinDigest` ou `digest`, et sans modifier les workflows.
- [ ] 2.2 Valider la syntaxe et la configuration resolue, y compris les groupes et presets, et verifier que les mises a jour majeures ne sont pas eligible a l'automerge.
- [ ] 2.3 Sur une PR Renovate eligible et avec les autorisations necessaires, verifier l'application de la regle et l'attente de `test-and-coverage` ; consigner les limitations ou une recette reelle non effectuee.

## 3. Documentation et validation

- [ ] 3.1 Documenter la politique de fusion, le check obligatoire, le maintien des protections et la limite des tests Maven qui ne construisent pas l'image Docker dans la PR ; mettre a jour le changelog et la version du projet selon les conventions applicables.
- [ ] 3.2 Valider strictement les artefacts OpenSpec et verifier que le workflow et la politique de deploiement restent inchanges ; documenter les prerequis GitHub non satisfaits.