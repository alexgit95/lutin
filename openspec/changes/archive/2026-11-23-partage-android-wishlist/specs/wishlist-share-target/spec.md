## ADDED Requirements

### Requirement: Cible de partage Android

Le systeme SHALL declarer dans le manifeste web une cible `share_target` vers `/partage`, utilisant GET et acceptant les parametres `url`, `title` et `text`. Le parcours SHALL etre utilisable en ligne avec la PWA installee en HTTPS sur Android et un navigateur compatible ; Chrome Android SHALL etre la cible de recette. La reception du partage SHALL NOT creer d'article ou de wishlist.

#### Scenario: Partage depuis une application compatible
- **WHEN** l'utilisateur partage un lien web depuis une application Android qui le transmet au menu de partage, avec la PWA Lutin installee dans Chrome et son manifeste a jour
- **THEN** Lutin est propose comme destination et son choix ouvre le parcours de reception avec les donnees transmises

#### Scenario: GET sans mutation metier
- **WHEN** une requete GET de partage est ouverte ou rechargee
- **THEN** aucun article et aucune wishlist ne sont crees

### Requirement: Interpretation bornee des donnees partagees

Le systeme SHALL traiter les donnees recues comme non fiables, refuser une URL brute de plus de 2048 caracteres, un titre brut de plus de 2048 caracteres ou un texte de plus de 8192 caracteres, puis retenir l'URL explicite non vide en priorite. Si elle est absente, il SHALL extraire le premier lien HTTP(S) valide du texte. Il SHALL accepter seulement une URL HTTP(S) avec un hote et sans identifiants, conserver les parametres et fragments du lien retenu et SHALL NOT tronquer une URL pour la rendre acceptable. Sans URL exploitable, il SHALL afficher un message et une action vers les wishlists sans recuperation reseau.

#### Scenario: URL explicite prioritaire
- **WHEN** le partage contient une URL explicite valide et un autre lien dans le texte
- **THEN** l'URL explicite est utilisee, sans modification de ses parametres ou de son fragment

#### Scenario: URL uniquement dans le texte
- **WHEN** le champ URL est absent et le texte contient un lien HTTPS valide au milieu d'un message, avec des espaces ou sauts de ligne
- **THEN** ce lien est extrait pour le parcours d'ajout

#### Scenario: Plusieurs liens dans le texte
- **WHEN** le champ URL est absent et le texte contient plusieurs liens HTTP(S) valides
- **THEN** seul le premier lien valide dans l'ordre du texte est retenu

#### Scenario: URL explicite invalide
- **WHEN** une URL explicite non vide est invalide, utilise un autre schema que HTTP(S) ou contient des identifiants
- **THEN** le partage est refuse avec un message, sans remplacement silencieux par un lien du texte et sans appel reseau

#### Scenario: Aucun lien exploitable
- **WHEN** le partage contient seulement une image, du texte sans lien ou un lien interne non HTTP(S)
- **THEN** aucun formulaire pre-rempli n'est ouvert et un message permet de rejoindre les wishlists

#### Scenario: Entree trop longue
- **WHEN** un des champs depasse sa limite d'entree
- **THEN** le partage est refuse explicitement sans appel reseau ni creation et sans troncature de l'URL

### Requirement: Authentification et reprise du partage

Le systeme SHALL reserver le choix des wishlists et le formulaire a un utilisateur authentifie et autorise. Pour un utilisateur deja inscrit dont l'authentification doit etre renouvelee, le parcours SHALL conserver l'URL et le titre partages apres connexion locale ou Cloudflare Access. Aucun acces anonyme aux wishlists ni exemption CSRF SHALL etre introduit pour assurer cette reprise. Le premier onboarding Cloudflare SHALL rester inchange et ne garantit pas la reprise automatique du partage.

#### Scenario: Utilisateur deja authentifie
- **WHEN** un utilisateur authentifie et autorise ouvre un partage valide
- **THEN** il accede au choix de ses wishlists avec le contexte partage

#### Scenario: Connexion locale necessaire
- **WHEN** un utilisateur deja inscrit ouvre un partage sans session locale et se connecte avec succes
- **THEN** il retrouve le choix des wishlists avec l'URL et le titre du partage initial

#### Scenario: Reconnexion Cloudflare Access
- **WHEN** un utilisateur deja inscrit ouvre un partage en production avec une authentification Access expiree puis se reconnecte avec succes
- **THEN** le parcours reprend avec l'URL et le titre initiaux, sans divulgation de wishlist avant authentification

#### Scenario: Premier onboarding
- **WHEN** l'identite Cloudflare n'a pas encore de compte Lutin
- **THEN** le parcours d'inscription existant s'applique et aucune wishlist n'est exposee avant sa completion

### Requirement: Choix explicite parmi les wishlists personnelles

Le systeme SHALL presenter uniquement les wishlists dont l'utilisateur est proprietaire et SHALL exiger leur selection explicite, meme s'il n'en existe qu'une. Il SHALL recontroler la propriete a l'ouverture du formulaire et a l'enregistrement. Les wishlists partagees en lecture et les wishlists des autres utilisateurs SHALL NOT etre des destinations d'ajout, y compris pour un administrateur non proprietaire.

#### Scenario: Plusieurs wishlists
- **WHEN** l'utilisateur recoit un partage valide et possede plusieurs wishlists
- **THEN** il choisit la destination et le formulaire de cette wishlist s'ouvre en conservant le partage

#### Scenario: Une seule wishlist
- **WHEN** l'utilisateur possede une seule wishlist
- **THEN** le choix de cette wishlist est affiche sans ajout ni selection automatique

#### Scenario: Wishlist seulement partagee
- **WHEN** une wishlist est partagee avec l'utilisateur sans qu'il en soit proprietaire
- **THEN** elle n'apparait pas dans les destinations possibles

#### Scenario: Identifiant de wishlist falsifie
- **WHEN** un utilisateur, administrateur compris, demande le formulaire ou l'enregistrement pour une wishlist dont il n'est pas proprietaire
- **THEN** l'acces est refuse avec 403 et aucun article n'est cree

#### Scenario: Wishlist supprimee entre reception et choix
- **WHEN** la wishlist choisie a ete supprimee depuis l'affichage du choix
- **THEN** aucun article n'est cree et l'utilisateur peut revenir au choix avec les donnees partagees conservees

### Requirement: Premiere wishlist sans perte du partage

Si l'utilisateur ne possede aucune wishlist, le systeme SHALL proposer d'en creer une en conservant le partage. La creation SHALL reutiliser les regles existantes de nom et de propriete et un POST protege par CSRF. Apres creation, le systeme SHALL revenir au choix explicite de la wishlist ; une erreur de creation SHALL conserver le contexte du partage.

#### Scenario: Aucune wishlist personnelle
- **WHEN** un utilisateur sans wishlist personnelle recoit un partage valide
- **THEN** il peut creer sa premiere wishlist sans recopier l'URL ni le titre

#### Scenario: Creation reussie
- **WHEN** l'utilisateur cree sa premiere wishlist depuis ce parcours
- **THEN** elle apparait dans le choix de destination avec le partage conserve et aucun article n'est encore cree

#### Scenario: Erreur de creation
- **WHEN** l'utilisateur soumet un nom de wishlist invalide
- **THEN** l'erreur est affichee et le formulaire conserve l'URL et le titre du partage

### Requirement: Formulaire pre-rempli et titre facultatif

Apres choix de la wishlist, le systeme SHALL reutiliser le formulaire d'ajout d'article avec URL pre-remplie, prix vide et champs modifiables. Il SHALL privilegier le titre partage, nettoye des caracteres de controle et des espaces superflus et limite a 100 caracteres conformement aux regles metier du nom. En l'absence de titre exploitable, il SHALL tenter de recuperer le titre de la page via le service reseau existant et ses protections SSRF, apres controle des droits et hors transaction de base de donnees. Un echec SHALL laisser le nom vide sans bloquer l'affichage du formulaire. Aucune recuperation reseau SHALL avoir lieu pour le simple choix de wishlist ou lorsqu'un titre partage exploitable existe.

#### Scenario: Titre transmis
- **WHEN** le partage contient un titre exploitable et l'utilisateur choisit une wishlist
- **THEN** le nom et l'URL sont pre-remplis, le prix est vide et aucune requete de recuperation du titre n'est envoyee

#### Scenario: Titre nettoye et borne
- **WHEN** le titre accepte contient des caracteres de controle, des espaces multiples ou depasse 100 caracteres apres nettoyage
- **THEN** le nom pre-rempli est nettoye et limite a 100 caracteres

#### Scenario: Titre absent et recuperable
- **WHEN** aucun titre exploitable n'est transmis et la recuperation protegee retourne un titre
- **THEN** le nom du formulaire est pre-rempli avec ce titre nettoye et borne

#### Scenario: Titre indisponible
- **WHEN** la recuperation du titre echoue, depasse les delais existants ou ne retourne aucun titre
- **THEN** le formulaire s'affiche avec l'URL conservee et un nom vide que l'utilisateur peut saisir

#### Scenario: Pas de recherche avant selection
- **WHEN** l'utilisateur consulte seulement l'ecran de choix de wishlist
- **THEN** aucun appel de recuperation du titre de la page partagee n'est effectue

### Requirement: Enregistrement explicite et reutilisation des validations

Le systeme SHALL enregistrer l'article uniquement apres soumission explicite du formulaire existant, avec les controles de propriete, CSRF, nom, URL et prix existants. Un reaffichage apres erreur SHALL conserver les valeurs corrigees par l'utilisateur sans les remplacer par les valeurs partagees. L'annulation SHALL revenir a la wishlist selectionnee sans creation. Le parcours manuel d'ajout SHALL rester fonctionnel sans contexte partage.

#### Scenario: Validation apres correction
- **WHEN** l'utilisateur modifie les valeurs pre-remplies puis enregistre un formulaire valide
- **THEN** un article est cree dans la wishlist selectionnee avec les valeurs soumises et l'utilisateur rejoint cette wishlist

#### Scenario: Erreur de formulaire
- **WHEN** le formulaire soumis contient une valeur invalide
- **THEN** l'erreur est affichee avec les valeurs saisies conservees et aucun article n'est cree

#### Scenario: Annulation
- **WHEN** l'utilisateur annule le formulaire issu du partage
- **THEN** il revient a la wishlist selectionnee sans creation d'article

#### Scenario: CSRF absent
- **WHEN** un POST de creation de wishlist ou d'article est soumis sans jeton CSRF valide
- **THEN** la requete est refusee et rien n'est cree

#### Scenario: Ajout manuel preserve
- **WHEN** l'utilisateur ouvre le formulaire d'ajout normal sans donnees partagees
- **THEN** le formulaire reste initialement vide et le parcours habituel est conserve

### Requirement: Protection du contenu et des acces reseau

Le systeme SHALL echapper le titre et les autres donnees partagees lors du rendu HTML, utiliser des chemins internes fixes pour la navigation et appliquer les protections SSRF existantes a chaque recuperation de titre et redirection reseau. Les pages du parcours de reception et de pre-remplissage SHALL porter `Cache-Control: no-store` et `Referrer-Policy: no-referrer`. Le systeme SHALL NOT ajouter de journalisation applicative du contenu partage ni de stockage hors connexion de ce contenu.

#### Scenario: Titre contenant du HTML
- **WHEN** un titre partage contient du balisage ou du script
- **THEN** il est rendu comme texte sans execution

#### Scenario: Destination reseau interdite
- **WHEN** la recuperation du titre vise une adresse interdite par le service existant, directement ou apres redirection
- **THEN** la destination interdite n'est pas contactee et le formulaire reste utilisable avec un nom vide

#### Scenario: Navigation confidentielle
- **WHEN** une page du parcours de partage est rendue
- **THEN** les en-tetes interdisent sa mise en cache et l'envoi d'un referent et aucune destination de retour externe n'est utilisee

#### Scenario: Partages independants
- **WHEN** l'utilisateur ouvre deux liens partages dans deux onglets puis choisit une wishlist dans chacun
- **THEN** chaque formulaire conserve son propre contexte sans remplacer le contenu de l'autre