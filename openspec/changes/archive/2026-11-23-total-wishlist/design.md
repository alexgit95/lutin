## Context

Les vues proprietaire et lecteur utilisent des DTO distincts ; la page publique reutilise la vue lecteur. Les souhaits sont pagines par lots de 50, leur prix est un `BigDecimal` facultatif et la reservation ne doit jamais etre divulguee au proprietaire. Les deux bases prises en charge sont SQLite et PostgreSQL.

## Goals / Non-Goals

**Goals:**

- Afficher le meme total global et le nombre de prix manquants sur les trois vues, a chaque page.
- Preserver l'aveuglement du proprietaire, les droits actuels et la pagination.

**Non-Goals:**

- Budget cible, conversion de devises, total des seuls articles disponibles, changement du schema ou du parcours de reservation.

## Decisions

### Agregation par wishlist apres controle d'acces

Ajouter au repository une agregation bornee par identifiant de wishlist, appelee apres resolution du droit proprietaire, lecteur ou du jeton public. Calculer somme des prix non nuls et nombre de prix nuls sur tous les articles, sans predicat sur l'etat pris. Retourner des valeurs numeriques dans les deux DTO de vue ; ne pas exposer les entites ou informations de prise au proprietaire. Une wishlist vide ou sans prix a un total de zero ; un prix nul est distingue d'un prix renseigne a zero.

Une somme de la page serait incorrecte et charger toute la collection en memoire contreviendrait a la pagination. Les tests devront verifier explicitement la precision des centimes et le comportement de l'agregat sur SQLite et PostgreSQL ; choisir une implementation compatible avec les deux, sans arrondi silencieux ni hypothese non verifiee sur les types SQLite.

### Presentation et actualisation

Afficher le montant en haut du contenu de chaque vue avec le format monetaire existant ; afficher le nombre de prix manquants lorsqu'il est non nul. Indiquer clairement que le montant est base sur les prix renseignes. Les formulaires existants redirigent vers la wishlist, ou l'agregat est recalcule ; aucune persistance du total ni mise a jour client supplementaire.

## Risks / Trade-offs

- [Precision et types d'agregation SQLite/PostgreSQL] -> Tester des montants decimaux et plusieurs articles dans les deux moteurs ; ne pas accepter de resultat arrondi ou de conversion fragile.
- [Fuite indirecte de reservation] -> Ne jamais filtrer par prise et verifier que prendre ou liberer ne change ni le total ni le compteur dans la vue proprietaire.
- [Lecture concurremment modifiee] -> Calculer a chaque consultation ; les modifications concurrentes peuvent apparaitre au prochain rechargement.

## Migration Plan

Deployer avec le mecanisme existant sans migration de schema. En cas de retour arriere, retirer l'affichage et les calculs ; les donnees des souhaits restent inchangees.