## ADDED Requirements

### Requirement: Import de personnes depuis une autre liste
Le système SHALL permettre au propriétaire d'une liste d'importer, dans l'onglet Personnes, les personnes et leur budget d'une autre liste dont il est également propriétaire, après un aperçu. Seules les personnes SHALL être copiées, jamais les cadeaux. Une personne dont le nom existe déjà dans la liste cible, sans tenir compte de la casse ni des espaces en bordure, SHALL être signalée « déjà présente » dans l'aperçu et SHALL NOT être importée. Les autres personnes SHALL être cochées par défaut. Le système SHALL contrôler côté serveur les droits sur les deux listes, l'appartenance des personnes cochées à la liste source et les doublons au moment de l'import, et SHALL relire les noms et budgets en base. Une liste dont l'utilisateur n'est que lecteur SHALL NOT servir de source.

#### Scenario: Panneau d'import
- **WHEN** le propriétaire possède au moins une autre liste et ouvre l'onglet Personnes
- **THEN** un panneau propose ses autres listes avec leur nombre de personnes

#### Scenario: Aucune autre liste
- **WHEN** le propriétaire ne possède qu'une seule liste
- **THEN** le panneau d'import n'est pas affiché

#### Scenario: Aperçu
- **WHEN** le propriétaire choisit une liste source et demande l'aperçu
- **THEN** il voit les personnes de la source avec leur budget, cochées par défaut, et le total du budget importable

#### Scenario: Personne déjà présente
- **WHEN** la liste cible contient déjà une personne de même nom (casse et espaces en bordure ignorés)
- **THEN** l'aperçu la grise avec la mention « déjà présente », sa case est inactive et elle n'est jamais importée

#### Scenario: Import
- **WHEN** le propriétaire valide l'aperçu
- **THEN** les personnes cochées sont créées dans la liste cible avec le nom et le budget de la source, et un compte rendu indique les personnes importées, le budget total ajouté et les personnes ignorées

#### Scenario: Copie indépendante
- **WHEN** le budget d'une personne importée est modifié dans l'une des listes
- **THEN** l'autre liste n'est pas modifiée

#### Scenario: Cadeaux non copiés
- **WHEN** des personnes sont importées
- **THEN** aucun cadeau de la liste source n'est copié

#### Scenario: Personne ajoutée entre l'aperçu et l'import
- **WHEN** une personne de même nom est ajoutée à la cible après l'aperçu puis que l'import est validé
- **THEN** elle n'est pas dupliquée et figure parmi les personnes ignorées

#### Scenario: Identifiant forgé
- **WHEN** la requête d'import contient l'identifiant d'une personne qui n'appartient pas à la liste source
- **THEN** cette personne est ignorée et rien n'est copié depuis une autre liste

#### Scenario: Source non possédée
- **WHEN** l'utilisateur demande un aperçu ou un import depuis une liste dont il n'est pas propriétaire, y compris une liste partagée avec lui
- **THEN** la requête est refusée (403) et rien n'est copié

#### Scenario: Cible non possédée
- **WHEN** un lecteur de la liste cible ou un autre utilisateur tente un aperçu ou un import
- **THEN** la requête est refusée (403)

#### Scenario: Source identique à la cible
- **WHEN** la source choisie est la liste cible
- **THEN** la demande est refusée avec un message d'erreur

#### Scenario: Rien à importer
- **WHEN** la source n'a aucune personne, ou que toutes sont déjà présentes
- **THEN** l'aperçu l'indique et l'import ne crée aucune personne
