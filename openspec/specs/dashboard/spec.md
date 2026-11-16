# Dashboard

## Purpose
Expose purchase-list spending indicators and gift status at a glance, including on mobile screens.

## Requirements

### Requirement: Indicateurs budgétaires
Le dashboard d'une liste SHALL afficher le budget global (somme des budgets des personnes), le budget déboursé (cadeaux hors tiers au statut « Commandé » ou plus, avec prix) et le reste à payer (cadeaux hors tiers au statut « À acheter », avec prix).

#### Scenario: Calcul
- **WHEN** une liste contient un cadeau à 20 € « Commandé », un à 30 € « À acheter » et un à 50 € pour compte de tiers « Commandé »
- **THEN** le déboursé est 20 € et le reste à payer est 30 €

#### Scenario: Prix absent
- **WHEN** un cadeau n'a pas de prix
- **THEN** il n'est compté dans aucun montant

### Requirement: Vue des statuts
Le dashboard SHALL afficher tous les cadeaux avec leur statut et une répartition par statut.

#### Scenario: Affichage
- **WHEN** l'utilisateur ouvre le dashboard
- **THEN** chaque cadeau est listé avec son destinataire, son prix et son statut

### Requirement: Changement de statut depuis le dashboard
Le dashboard SHALL permettre au propriétaire de changer le statut d'un cadeau sans quitter la page.

#### Scenario: Mise à jour en place
- **WHEN** le propriétaire choisit un nouveau statut sur une ligne
- **THEN** le statut est enregistré et les indicateurs sont recalculés

### Requirement: Interface adaptée au mobile
L'interface SHALL s'inspirer de la maquette (cartes d'indicateurs colorées, navigation, pastilles de statut) et rester utilisable sur écran de téléphone. Le dashboard SHALL s'adapter à la largeur disponible sans défilement horizontal, tout en conservant l'accès aux informations et actions des cadeaux.

#### Scenario: Écran étroit
- **WHEN** le dashboard est affiché sur un écran de téléphone
- **THEN** la navigation et les cartes restent lisibles sans défilement horizontal

#### Scenario: Cadeaux sur écran étroit
- **WHEN** le dashboard contient des cadeaux avec des noms ou des données longs sur un écran de téléphone
- **THEN** le contenu reste lisible et les actions accessibles sans défilement horizontal

### Requirement: Colonnes de cadeaux alignées
Le dashboard SHALL aligner les colonnes correspondantes des groupes de cadeaux dans une même présentation, indépendamment de la longueur des noms et des données de chaque groupe. Le destinataire SHALL être identifié par l'en-tête du groupe, sans colonne « Pour » redondante. Les noms de cadeaux longs SHALL pouvoir être affichés en entier sur plusieurs lignes, y compris lorsqu'ils contiennent un mot sans espace.

#### Scenario: Groupes de tailles différentes
- **WHEN** plusieurs personnes ont des cadeaux dont les noms et les autres données ont des longueurs différentes
- **THEN** les colonnes correspondantes sont alignées entre les groupes et les actions restent dans leur colonne

#### Scenario: Titre long
- **WHEN** un cadeau possède un nom trop long pour sa colonne, avec ou sans espaces
- **THEN** son nom passe sur plusieurs lignes sans chevaucher les autres colonnes ni être tronqué

#### Scenario: Destinataire dans l'en-tête
- **WHEN** un propriétaire ou un lecteur consulte les cadeaux visibles regroupés par personne
- **THEN** le nom du destinataire apparaît dans l'en-tête de son groupe, sans colonne « Pour » dans le tableau

#### Scenario: Chargement et modification en place
- **WHEN** un lot supplémentaire est chargé ou qu'un statut est modifié par HTMX
- **THEN** les lignes ajoutées ou mises à jour respectent le même alignement et le même retour à la ligne

### Requirement: Menu des actions de cadeau
Le dashboard SHALL regrouper les actions de modification et de suppression de chaque cadeau dans un bouton de menu compact réservé au propriétaire. Le menu SHALL être accessible au clavier, utilisable sur mobile et se fermer au clic extérieur, à l'ouverture d'un autre menu ou avec Échap. La suppression SHALL conserver sa confirmation et ses contrôles d'accès.

#### Scenario: Actions du propriétaire
- **WHEN** le propriétaire ouvre le menu d'un cadeau
- **THEN** les actions « Modifier » et « Supprimer » sont disponibles pour ce cadeau

#### Scenario: Suppression confirmée
- **WHEN** le propriétaire sélectionne « Supprimer »
- **THEN** une confirmation est demandée avant l'envoi de la suppression

#### Scenario: Menu près des bords de l'écran
- **WHEN** le propriétaire ouvre le menu près du haut ou du bas d'un écran de téléphone
- **THEN** le menu s'ouvre dans l'espace disponible et les deux actions restent visibles

#### Scenario: Lecture seule
- **WHEN** un lecteur consulte le dashboard ou charge un lot de cadeaux
- **THEN** aucun menu de modification ou de suppression ne lui est transmis

#### Scenario: Mise à jour dynamique
- **WHEN** un lot de cadeaux est chargé ou qu'une ligne est remplacée après un changement de statut
- **THEN** le menu des actions de chaque cadeau du propriétaire reste utilisable

### Requirement: Barre de workflow adaptée au mobile
La barre persistante SHALL présenter les cinq libellés de statut et leurs compteurs lisiblement sur écran étroit, sans défilement horizontal. Elle SHALL rester visible pendant le défilement sans masquer les cadeaux ni leurs actions, y compris avec une zone de sécurité en bas de l'écran.

#### Scenario: Cinq statuts sur téléphone
- **WHEN** le dashboard est affiché sur un téléphone étroit
- **THEN** les cinq libellés et compteurs restent visibles et lisibles, sans chevauchement ni défilement horizontal

#### Scenario: Dernier cadeau accessible
- **WHEN** l'utilisateur atteint le bas de la liste sur téléphone
- **THEN** le dernier cadeau et ses actions restent entièrement visibles au-dessus de la barre

#### Scenario: Compteurs après modification
- **WHEN** le propriétaire change un statut sur téléphone et que le serveur confirme la modification
- **THEN** la barre actualisée reste adaptée à l'écran et affiche les compteurs confirmés

### Requirement: Cadeaux regroupés par personne
Le dashboard SHALL présenter les cadeaux visibles regroupés par personne destinataire. Deux personnes distinctes portant le même nom SHALL rester dans des groupes distincts. Les cadeaux masqués consultés par un lecteur SHALL apparaître dans un unique groupe neutre, placé après les autres groupes, sans exposer ni nom de cadeau, ni destinataire, ni statut.

#### Scenario: Cadeaux d'une personne
- **WHEN** le propriétaire consulte une liste contenant plusieurs cadeaux pour une personne
- **THEN** ces cadeaux apparaissent sous un même groupe portant le nom de cette personne

#### Scenario: Personnes homonymes
- **WHEN** deux personnes distinctes de la liste portent le même nom
- **THEN** les cadeaux de chacune restent dans leur groupe respectif

#### Scenario: Cadeau masqué vu par un lecteur
- **WHEN** un lecteur consulte un cadeau masqué
- **THEN** il apparaît comme une ligne « Cadeau masqué » dans le groupe neutre, sans nom de cadeau, nom de destinataire ni statut dans le groupe, le HTML ou la réponse de chargement

#### Scenario: Groupe masqué en dernière position
- **WHEN** un lecteur parcourt une liste dont les cadeaux sont chargés en plusieurs lots
- **THEN** les lignes masquées ne se trouvent qu'après tous les cadeaux visibles et ne réapparaissent pas entre deux groupes de personnes

### Requirement: Chargement continu des cadeaux
Le dashboard SHALL permettre de parcourir tous les cadeaux sans contrôles de pagination visibles, en chargeant les résultats progressivement par lots lorsque l'utilisateur approche de la fin des cadeaux déjà affichés.

#### Scenario: Chargement du lot suivant
- **WHEN** le marqueur de fin approche de la zone visible et qu'il reste des cadeaux
- **THEN** le lot suivant est ajouté à la liste sans remplacer les cadeaux déjà affichés ni déplacer la position de défilement

#### Scenario: Groupe à la frontière d'un lot
- **WHEN** les cadeaux d'une même personne s'étendent sur deux lots consécutifs
- **THEN** les cadeaux du lot suivant continuent le groupe existant sans répéter son en-tête

#### Scenario: Filtre par tag actif
- **WHEN** l'utilisateur charge un lot suivant avec un filtre par tag actif
- **THEN** seuls les cadeaux correspondant à ce filtre sont ajoutés et le filtre reste actif

#### Scenario: Fin de liste
- **WHEN** tous les cadeaux correspondant à la liste et au filtre sont affichés
- **THEN** aucun lot supplémentaire n'est demandé et aucun contrôle de pagination n'est affiché

### Requirement: Barre de workflow persistante
Le dashboard SHALL afficher une barre compacte persistante contenant le nombre de cadeaux pour chacun des statuts « À acheter », « Commandé », « Livré », « Emballé » et « Remis ». Ces nombres SHALL représenter toute la liste, indépendamment du filtre par tag et des lots chargés, et SHALL rester visibles pendant le défilement sans masquer les cadeaux.

#### Scenario: Compteurs indépendants du filtre
- **WHEN** l'utilisateur active un filtre par tag
- **THEN** la barre conserve les nombres de statuts calculés sur toute la liste, sans être réduits aux cadeaux filtrés ou chargés

#### Scenario: Comptes du propriétaire
- **WHEN** le propriétaire consulte la barre de workflow
- **THEN** les nombres incluent les cadeaux masqués et excluent aucun statut de la liste

#### Scenario: Comptes du lecteur
- **WHEN** un lecteur consulte la barre de workflow
- **THEN** les nombres excluent les cadeaux masqués et aucune information de statut de ces cadeaux ne lui est transmise

### Requirement: Animation du changement de statut
Après l'enregistrement réussi d'un changement de statut par le propriétaire, le dashboard SHALL animer une représentation temporaire du cadeau de sa ligne vers le statut cible et animer la diminution du compte source ainsi que l'augmentation du compte cible. La ligne persistante du cadeau SHALL rester dans son groupe de personne.

#### Scenario: Changement enregistré
- **WHEN** le propriétaire choisit un nouveau statut et que le serveur confirme l'enregistrement
- **THEN** la représentation du cadeau rejoint visuellement l'étape cible, les deux compteurs affichent leurs nouvelles valeurs et le cadeau reste dans son groupe

#### Scenario: Échec de mise à jour
- **WHEN** le serveur ne confirme pas le changement de statut
- **THEN** aucune animation de succès n'est jouée et les compteurs affichent les valeurs confirmées par le serveur

#### Scenario: Réduction des animations
- **WHEN** l'utilisateur a activé la préférence de réduction des animations de son navigateur
- **THEN** les valeurs des compteurs sont mises à jour sans déplacement animé du cadeau ni mouvement du nombre

#### Scenario: Lecteur en lecture seule
- **WHEN** un lecteur consulte la liste
- **THEN** il peut voir les comptes de workflow autorisés mais ne dispose d'aucune commande de changement de statut ni animation de déplacement

### Requirement: Dashboard sans suivi de remboursement
Le dashboard SHALL afficher tous les cadeaux de la liste, regroupés par personne, avec leur statut modifiable, le filtre par tag et la barre de workflow, et SHALL ne contenir aucune donnée ni action de remboursement : ni carte, ni alerte, ni pastille, ni filtre « À rembourser ». Le paramètre `aRembourser` SHALL être ignoré. Le suivi du remboursement SHALL se faire dans l'onglet Remboursement.

#### Scenario: Cadeaux et statuts conservés
- **WHEN** le propriétaire consulte le dashboard
- **THEN** tous les cadeaux, leur statut modifiable, le filtre par tag et la barre de workflow en bas sont présents

#### Scenario: Aucune donnée de remboursement
- **WHEN** le propriétaire consulte le dashboard d'une liste contenant des cadeaux remboursés et des alertes
- **THEN** la page et les lots de chargement continu ne contiennent ni carte, ni alerte, ni pastille, ni filtre de remboursement

#### Scenario: Paramètre ignoré
- **WHEN** l'adresse du dashboard porte le paramètre `aRembourser`
- **THEN** la liste n'est pas filtrée

#### Scenario: Changement de statut sans mise à jour du remboursement
- **WHEN** le propriétaire change le statut d'un cadeau depuis le dashboard
- **THEN** seules la ligne, la barre de workflow et le badge de l'onglet Remboursement sont mis à jour
