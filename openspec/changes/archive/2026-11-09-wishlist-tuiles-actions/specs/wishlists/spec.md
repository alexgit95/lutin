## MODIFIED Requirements

### Requirement: Gestion des articles
Le système SHALL permettre au propriétaire d'ajouter, modifier et supprimer les articles de sa wishlist. La suppression SHALL se faire depuis l'écran de modification de l'article, avec confirmation, et non depuis la tuile ; elle SHALL être absente de l'écran d'ajout. Le nom SHALL être obligatoire ; l'URL et le prix SHALL être facultatifs. Une URL fournie SHALL commencer par `http://` ou `https://`, un prix fourni SHALL être positif ou nul. Les articles SHALL être paginés et triés indépendamment de l'état « pris ».

#### Scenario: Ajout minimal
- **WHEN** le propriétaire ajoute un article avec un nom seul
- **THEN** l'article est créé sans URL ni prix

#### Scenario: Nom manquant
- **WHEN** le propriétaire valide un article sans nom
- **THEN** l'ajout est refusé avec un message d'erreur

#### Scenario: URL invalide
- **WHEN** le propriétaire saisit une URL qui n'est pas en http ou https
- **THEN** l'enregistrement est refusé avec un message d'erreur

#### Scenario: Titre automatique
- **WHEN** le propriétaire saisit une URL valide et demande le titre
- **THEN** le nom est prérempli avec le titre de la page, via la récupération protégée contre la SSRF

#### Scenario: Modification
- **WHEN** le propriétaire modifie un article
- **THEN** la modification est appliquée

#### Scenario: Suppression depuis l'écran de modification
- **WHEN** le propriétaire ouvre l'écran de modification d'un article et confirme la suppression
- **THEN** l'article est supprimé

#### Scenario: Pas de suppression sur la tuile
- **WHEN** le propriétaire consulte l'onglet Articles
- **THEN** aucune tuile ne porte de bouton de suppression

#### Scenario: Pas de suppression à l'ajout
- **WHEN** le propriétaire ouvre l'écran d'ajout d'un article
- **THEN** aucun bouton de suppression n'est affiché

### Requirement: Onglets et rendu de la wishlist
Le système SHALL présenter au propriétaire sa wishlist en trois onglets (Articles, Partage, Réglages), la page Articles n'affichant que les articles. Les articles SHALL être rendus en tuiles. Pour un lecteur, une tuile d'article pris SHALL se distinguer visuellement d'une tuile libre et nommer le preneur ; le propriétaire SHALL recevoir un rendu identique pour tous les articles. Une tuile d'article qui a une URL SHALL ouvrir cette URL dans un nouvel onglet au clic, sans bouton « Voir », et SHALL porter un bouton « Copier » qui copie l'URL dans le presse-papiers. Une tuile sans URL SHALL n'être pas cliquable et SHALL ne pas porter de bouton « Copier ». Ce comportement SHALL valoir pour le propriétaire, les lecteurs et la page publique.

#### Scenario: Réglages dans un onglet dédié
- **WHEN** le propriétaire ouvre l'onglet Articles
- **THEN** la page ne contient ni le formulaire de renommage ni la suppression de la wishlist, accessibles dans l'onglet Réglages

#### Scenario: Tuile prise vue par un lecteur
- **WHEN** un lecteur consulte un article pris
- **THEN** la tuile est grisée avec un ruban « Pris par » suivi du nom du preneur

#### Scenario: Rendu propriétaire
- **WHEN** le propriétaire consulte une wishlist contenant des articles pris
- **THEN** toutes les tuiles ont le même rendu, sans ruban ni action de prise

#### Scenario: Clic sur un article avec URL
- **WHEN** un utilisateur clique sur la tuile d'un article qui a une URL
- **THEN** l'URL s'ouvre dans un nouvel onglet avec `rel="noopener noreferrer"`

#### Scenario: Plus de bouton Voir
- **WHEN** une tuile est affichée au propriétaire, à un lecteur ou sur la page publique
- **THEN** aucun bouton « Voir » n'est présent

#### Scenario: Bouton Copier
- **WHEN** un utilisateur clique « Copier » sur un article qui a une URL
- **THEN** l'URL de l'article est copiée dans le presse-papiers et « Lien copié » est affiché

#### Scenario: Article sans URL
- **WHEN** un article n'a pas d'URL
- **THEN** sa tuile n'est pas cliquable et ne porte pas de bouton « Copier »

#### Scenario: Boutons de la tuile
- **WHEN** le propriétaire clique « Modifier » ou un lecteur clique « Je le prends »
- **THEN** l'action du bouton s'exécute et le lien de l'article ne s'ouvre pas
