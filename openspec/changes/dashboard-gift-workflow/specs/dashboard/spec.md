## ADDED Requirements

### Requirement: Cadeaux regroupés par personne
Le dashboard SHALL présenter les cadeaux visibles regroupés par personne destinataire. Deux personnes distinctes portant le même nom SHALL rester dans des groupes distincts. Un cadeau masqué consulté par un lecteur SHALL apparaître dans un groupe neutre sans exposer son destinataire.

#### Scenario: Cadeaux d'une personne
- **WHEN** le propriétaire consulte une liste contenant plusieurs cadeaux pour une personne
- **THEN** ces cadeaux apparaissent sous un même groupe portant le nom de cette personne

#### Scenario: Personnes homonymes
- **WHEN** deux personnes distinctes de la liste portent le même nom
- **THEN** les cadeaux de chacune restent dans leur groupe respectif

#### Scenario: Cadeau masqué vu par un lecteur
- **WHEN** un lecteur consulte un cadeau masqué
- **THEN** le cadeau reste visible par son nom mais aucun nom de destinataire ni statut ne figure dans son groupe, le HTML ou la réponse de chargement

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
