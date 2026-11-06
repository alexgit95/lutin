# Wishlists

## Purpose
Permettre à chacun de publier ses envies, de les partager et de coordonner les achats des proches sans que le propriétaire voie ce qui est pris.

## Requirements

### Requirement: Gestion des wishlists
Le système SHALL permettre à un utilisateur connecté de créer, renommer et supprimer ses wishlists depuis une page dédiée distincte des listes d'achat. Le nom SHALL être obligatoire et unique par propriétaire (comparaison sans tenir compte de la casse ni des espaces en bordure).

#### Scenario: Création
- **WHEN** l'utilisateur saisit un nom et valide
- **THEN** la wishlist est créée et apparaît dans « Mes wishlists »

#### Scenario: Nom vide
- **WHEN** l'utilisateur valide un nom vide
- **THEN** la création est refusée avec un message d'erreur

#### Scenario: Nom déjà utilisé
- **WHEN** l'utilisateur crée ou renomme une wishlist avec le nom d'une de ses autres wishlists
- **THEN** l'opération est refusée avec un message d'erreur et rien n'est modifié

#### Scenario: Même nom chez deux propriétaires
- **WHEN** deux propriétaires différents nomment une wishlist de la même façon
- **THEN** les deux opérations sont acceptées

#### Scenario: Suppression
- **WHEN** le propriétaire supprime sa wishlist
- **THEN** la wishlist, ses articles et ses partages sont supprimés

### Requirement: Gestion des articles
Le système SHALL permettre au propriétaire d'ajouter, modifier et supprimer les articles de sa wishlist. Le nom SHALL être obligatoire ; l'URL et le prix SHALL être facultatifs. Une URL fournie SHALL commencer par `http://` ou `https://`, un prix fourni SHALL être positif ou nul. Les articles SHALL être paginés et triés indépendamment de l'état « pris ».

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

#### Scenario: Modification et suppression
- **WHEN** le propriétaire modifie ou supprime un article
- **THEN** la modification ou la suppression est appliquée

### Requirement: Partage d'une wishlist
Le système SHALL permettre au propriétaire de partager une wishlist avec un autre utilisateur choisi dans une liste déroulante des logins existants, et de retirer ce partage. Seul le propriétaire SHALL voir et gérer les partages. L'administrateur n'a aucun accès particulier aux wishlists d'autrui.

#### Scenario: Partage
- **WHEN** le propriétaire choisit un login et valide
- **THEN** cet utilisateur voit la wishlist dans « Partagées avec moi »

#### Scenario: Partage impossible
- **WHEN** le propriétaire partage avec lui-même ou avec un utilisateur déjà destinataire
- **THEN** l'opération est refusée avec un message d'erreur

#### Scenario: Retrait
- **WHEN** le propriétaire retire le partage
- **THEN** l'utilisateur perd l'accès à la wishlist, ses avis en attente pour cette wishlist sont supprimés et ses articles pris restent « pris par » son nom

#### Scenario: Vue des partages réservée
- **WHEN** un lecteur ou un administrateur non propriétaire demande les partages d'une wishlist
- **THEN** la requête est refusée (403)

#### Scenario: Accès refusé
- **WHEN** un utilisateur ni propriétaire ni destinataire (administrateur compris) ouvre une wishlist
- **THEN** la requête est refusée (403)

### Requirement: Onglets et rendu de la wishlist
Le système SHALL présenter au propriétaire sa wishlist en trois onglets (Articles, Partage, Réglages), la page Articles n'affichant que les articles. Les articles SHALL être rendus en tuiles. Pour un lecteur, une tuile d'article pris SHALL se distinguer visuellement d'une tuile libre et nommer le preneur ; le propriétaire SHALL recevoir un rendu identique pour tous les articles.

#### Scenario: Réglages dans un onglet dédié
- **WHEN** le propriétaire ouvre l'onglet Articles
- **THEN** la page ne contient ni le formulaire de renommage ni la suppression de la wishlist, accessibles dans l'onglet Réglages

#### Scenario: Tuile prise vue par un lecteur
- **WHEN** un lecteur consulte un article pris
- **THEN** la tuile est grisée avec un ruban « Pris par » suivi du nom du preneur

#### Scenario: Rendu propriétaire
- **WHEN** le propriétaire consulte une wishlist contenant des articles pris
- **THEN** toutes les tuiles ont le même rendu, sans ruban ni action de prise

### Requirement: Consultation des wishlists
Le système SHALL présenter à l'utilisateur ses wishlists et, séparément, celles qui lui sont partagées. Un lecteur SHALL ne pouvoir modifier ni la wishlist ni ses articles, uniquement cocher ou décocher « pris ».

#### Scenario: Deux sections
- **WHEN** l'utilisateur ouvre la page des wishlists
- **THEN** il voit « Mes wishlists » et « Partagées avec moi »

#### Scenario: Lecteur sans droit d'édition
- **WHEN** un lecteur tente de modifier ou supprimer un article ou la wishlist
- **THEN** la requête est refusée (403) et rien n'est modifié

### Requirement: Article pris
Le système SHALL permettre à tout lecteur d'une wishlist de marquer un article comme « pris ». Le nom du preneur (son login) SHALL être enregistré en texte et affiché à tous les lecteurs. Un article déjà pris SHALL NOT pouvoir être pris une seconde fois.

#### Scenario: Prise
- **WHEN** un lecteur coche un article libre
- **THEN** l'article est « pris par » son login et les lecteurs voient ce nom

#### Scenario: Prise concurrente
- **WHEN** deux lecteurs cochent le même article en même temps
- **THEN** un seul réussit et l'autre reçoit un message indiquant par qui l'article est pris

#### Scenario: Propriétaire exclu
- **WHEN** le propriétaire tente de cocher ou décocher un article
- **THEN** la requête est refusée (403)

### Requirement: Décochage avec avertissement
Le système SHALL permettre à tout lecteur de décocher un article pris. Lorsque le lecteur n'est pas le preneur, ou n'est pas identifiable comme tel (invité), le système SHALL afficher un avertissement important nommant le preneur et exiger une confirmation avant de libérer l'article.

#### Scenario: Décochage de son propre article
- **WHEN** le lecteur décoche un article dont le nom du preneur est son login
- **THEN** l'article est libéré sans avertissement

#### Scenario: Décochage de l'article d'un autre
- **WHEN** un lecteur décoche un article pris par un autre nom
- **THEN** un avertissement important nomme le preneur et l'article n'est libéré qu'après confirmation

#### Scenario: Décochage par un invité
- **WHEN** un lecteur non identifié décoche un article pris
- **THEN** l'avertissement est affiché avant toute libération

#### Scenario: Purge des avis de modification
- **WHEN** un article est libéré
- **THEN** les avis de modification en attente concernant cet article sont supprimés

### Requirement: Aveuglement du propriétaire
Le système SHALL ne jamais exposer au propriétaire l'état « pris », le nom du preneur, ni les avis liés à sa wishlist, ni dans le HTML, ni dans les réponses partielles HTMX, ni par l'ordre de tri ou un compteur.

#### Scenario: Vue propriétaire
- **WHEN** le propriétaire consulte sa wishlist
- **THEN** aucun article n'indique s'il est pris et aucune donnée du preneur ne figure dans la réponse

#### Scenario: Tri indépendant
- **WHEN** un article est pris puis libéré
- **THEN** l'ordre d'affichage de la vue propriétaire n'est pas modifié

### Requirement: Avis aux lecteurs
Le système SHALL créer un avis pour chaque lecteur connecté lorsque le propriétaire modifie ou supprime un article pris, ou supprime une wishlist contenant des articles pris. Aucun avis ne SHALL être créé pour un article libre. Un seul avis de modification SHALL exister par article et par lecteur. Un avis SHALL être supprimé définitivement quand le lecteur le ferme, et SHALL expirer 90 jours après sa création. Un avis SHALL n'être reçu que par les utilisateurs connectés ayant un partage.

#### Scenario: Modification d'un article pris
- **WHEN** le propriétaire modifie un article pris
- **THEN** chaque lecteur connecté reçoit un avis « modifié » mentionnant l'article et son preneur, l'article restant « pris par »

#### Scenario: Modifications répétées
- **WHEN** le propriétaire modifie plusieurs fois le même article pris
- **THEN** chaque lecteur n'a qu'un seul avis de modification pour cet article

#### Scenario: Suppression d'un article pris
- **WHEN** le propriétaire supprime un article pris
- **THEN** les avis de modification de cet article sont remplacés par un avis « supprimé » pour chaque lecteur connecté

#### Scenario: Suppression d'un article libre
- **WHEN** le propriétaire modifie ou supprime un article libre
- **THEN** aucun avis n'est créé

#### Scenario: Suppression de la wishlist
- **WHEN** le propriétaire supprime une wishlist contenant au moins un article pris
- **THEN** chaque lecteur connecté reçoit un unique avis `WISHLIST_SUPPRIMEE` mentionnant le nombre d'articles pris, qui remplace les avis d'articles en attente de cette wishlist

#### Scenario: Wishlist sans article pris
- **WHEN** le propriétaire supprime une wishlist sans article pris
- **THEN** aucun avis n'est créé

#### Scenario: Fermeture
- **WHEN** un lecteur ferme un avis
- **THEN** l'avis est supprimé définitivement

#### Scenario: Expiration
- **WHEN** un avis a plus de 90 jours
- **THEN** il n'est plus affiché ni compté et est purgé

### Requirement: Visibilité des avis
Le système SHALL afficher les avis du lecteur sur la page des wishlists et sur la page de la wishlist concernée, et SHALL afficher dans la barre de navigation le nombre d'avis non expirés du lecteur.

#### Scenario: Compteur
- **WHEN** un lecteur a des avis non expirés
- **THEN** la barre de navigation affiche leur nombre sur chaque page

#### Scenario: Aucun avis
- **WHEN** un lecteur n'a aucun avis
- **THEN** aucun compteur n'est affiché
