## ADDED Requirements

### Requirement: Wishlist publique
Le système SHALL permettre au propriétaire de rendre sa wishlist publique, ce qui génère un jeton secret aléatoire et un lien `/p/{jeton}`. Le propriétaire SHALL pouvoir régénérer le lien, ce qui invalide l'ancien, et rendre la wishlist privée, ce qui invalide tous les liens. Une wishlist publique SHALL afficher un bouton Partager sur l'onglet Articles du propriétaire, et seulement dans ce cas. Le propriétaire SHALL être averti, au moment d'activer le lien, qu'il verra l'état « pris » s'il ouvre le lien sans être connecté.

#### Scenario: Activation
- **WHEN** le propriétaire active l'interrupteur « Rendre publique »
- **THEN** un lien est affiché et le bouton Partager apparaît sur l'onglet Articles

#### Scenario: Wishlist privée
- **WHEN** la wishlist n'est pas publique
- **THEN** aucun bouton Partager ni lien n'est affiché

#### Scenario: Régénération
- **WHEN** le propriétaire régénère le lien
- **THEN** l'ancien lien renvoie 404 et le nouveau donne accès à la wishlist

#### Scenario: Retour en privé
- **WHEN** le propriétaire rend la wishlist privée
- **THEN** tous les liens renvoient 404 et les articles pris restent « pris par »

#### Scenario: Gestion réservée
- **WHEN** un lecteur ou un administrateur non propriétaire tente d'activer, régénérer ou désactiver le lien
- **THEN** la requête est refusée (403)

#### Scenario: Partage
- **WHEN** le propriétaire utilise le bouton Partager
- **THEN** le partage natif du système est proposé s'il existe, sinon le lien est copié

### Requirement: Accès par lien public
Le système SHALL donner accès à une wishlist publique, sans connexion, à toute personne disposant du lien. La vue publique SHALL afficher uniquement le titre de la wishlist et ses articles, sans identifiant du propriétaire, sans avis et sans actions d'édition. Un jeton inconnu ou révoqué SHALL renvoyer 404. Le propriétaire connecté et les lecteurs partagés connectés SHALL être redirigés vers leur vue habituelle de la wishlist. Les pages publiques SHALL porter les en-têtes `Referrer-Policy: no-referrer`, `X-Robots-Tag: noindex, nofollow` et `Cache-Control: no-store`, et rester protégées par le CSRF.

#### Scenario: Visiteur non connecté
- **WHEN** un visiteur non connecté ouvre un lien valide
- **THEN** il voit le titre de la wishlist et ses articles, sans login du propriétaire

#### Scenario: Jeton invalide
- **WHEN** un visiteur ouvre un lien dont le jeton est inconnu ou révoqué
- **THEN** la réponse est 404, identique dans les deux cas

#### Scenario: Propriétaire connecté
- **WHEN** le propriétaire connecté ouvre le lien de sa wishlist
- **THEN** il est redirigé vers sa vue aveugle, sans état « pris » ni preneur

#### Scenario: Lecteur partagé connecté
- **WHEN** un lecteur partagé connecté ouvre le lien
- **THEN** il est redirigé vers sa vue habituelle de la wishlist

#### Scenario: En-têtes de protection
- **WHEN** une page publique est servie
- **THEN** elle porte `Referrer-Policy: no-referrer`, `X-Robots-Tag: noindex, nofollow` et `Cache-Control: no-store`

#### Scenario: CSRF
- **WHEN** un visiteur envoie une prise ou une libération sans jeton CSRF valide
- **THEN** la requête est refusée (403) et rien n'est modifié

#### Scenario: Aucune édition
- **WHEN** un visiteur tente de modifier ou supprimer un article, la wishlist ou ses partages par la route publique
- **THEN** aucune route ne le permet

### Requirement: Prise par un visiteur du lien
Le système SHALL permettre à un visiteur du lien public de prendre un article. Un visiteur non connecté SHALL saisir son nom dans une popup avant validation ; le nom SHALL être obligatoire, nettoyé des caractères de contrôle et limité à 100 caractères. Il SHALL être enregistré avec l'indicateur d'invité et affiché « nom (invité) ». Un utilisateur connecté, ni propriétaire ni lecteur partagé, SHALL prendre l'article sous son login, sans popup et sans indicateur d'invité. Le nom saisi SHALL être affiché comme du texte, jamais comme du HTML.

#### Scenario: Prise par un invité
- **WHEN** un visiteur non connecté clique « Je le prends », saisit « Tante Marie » et valide
- **THEN** l'article est « pris par Tante Marie (invité) » pour tous les visiteurs et lecteurs

#### Scenario: Nom manquant
- **WHEN** un visiteur valide la popup sans nom
- **THEN** la prise est refusée avec un message d'erreur et l'article reste libre

#### Scenario: Nom trop long ou avec du HTML
- **WHEN** un visiteur saisit un nom de plus de 100 caractères ou contenant du HTML
- **THEN** le nom trop long est refusé et le HTML est affiché tel quel, sans être interprété

#### Scenario: Prise par un compte connecté
- **WHEN** un utilisateur connecté, ni propriétaire ni lecteur partagé, prend un article par le lien
- **THEN** l'article est « pris par » son login, sans popup ni mention « invité »

#### Scenario: Prise concurrente
- **WHEN** deux visiteurs prennent le même article en même temps
- **THEN** un seul réussit et l'autre reçoit un message indiquant par qui l'article est pris

#### Scenario: Mémorisation du nom
- **WHEN** un visiteur a déjà saisi son nom dans ce navigateur
- **THEN** la popup est préremplie, sans que le serveur ne conserve ce nom

## MODIFIED Requirements

### Requirement: Article pris
Le système SHALL permettre à tout lecteur d'une wishlist de marquer un article comme « pris ». Le nom du preneur SHALL être enregistré en texte, sans lien vers un compte : le login pour un utilisateur connecté, le nom saisi pour un invité. Un nom d'invité SHALL être marqué comme tel et affiché avec la mention « (invité) ». Le nom SHALL être affiché à tous les lecteurs. Un article déjà pris SHALL NOT pouvoir être pris une seconde fois.

#### Scenario: Prise
- **WHEN** un lecteur connecté coche un article libre
- **THEN** l'article est « pris par » son login et les lecteurs voient ce nom

#### Scenario: Prise par un invité
- **WHEN** un invité prend un article libre
- **THEN** l'article est « pris par » son nom avec la mention « (invité) »

#### Scenario: Prise concurrente
- **WHEN** deux lecteurs cochent le même article en même temps
- **THEN** un seul réussit et l'autre reçoit un message indiquant par qui l'article est pris

#### Scenario: Propriétaire exclu
- **WHEN** le propriétaire tente de cocher ou décocher un article
- **THEN** la requête est refusée (403)

### Requirement: Décochage avec avertissement
Le système SHALL permettre à tout lecteur ou visiteur de décocher un article pris. Le système SHALL afficher un avertissement important nommant le preneur et exiger une confirmation avant de libérer l'article, sauf si le visiteur est connecté, que son login est le nom du preneur et que le preneur n'est pas un invité.

#### Scenario: Décochage de son propre article
- **WHEN** un utilisateur connecté décoche un article dont le preneur est son login et n'est pas un invité
- **THEN** l'article est libéré sans avertissement

#### Scenario: Décochage de l'article d'un autre
- **WHEN** un lecteur décoche un article pris par un autre nom
- **THEN** un avertissement important nomme le preneur et l'article n'est libéré qu'après confirmation

#### Scenario: Décochage par un invité
- **WHEN** un visiteur non connecté décoche un article pris
- **THEN** l'avertissement est affiché avant toute libération

#### Scenario: Preneur invité portant un login existant
- **WHEN** un utilisateur connecté décoche un article pris par un invité dont le nom est identique à son login
- **THEN** l'avertissement est affiché avant la libération

#### Scenario: Purge des avis de modification
- **WHEN** un article est libéré
- **THEN** les avis de modification en attente concernant cet article sont supprimés
