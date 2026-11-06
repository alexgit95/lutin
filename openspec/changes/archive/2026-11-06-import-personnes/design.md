## Context

`Personne` ne porte que `nom`, `budget` et sa `liste`. `PersonneService.creer` valide déjà le nom (100 caractères) et le budget (positif, arrondi à 2 décimales). `AccesListeService.proprietaire` refuse l'accès à une liste dont on n'est pas propriétaire. Aucune contrainte d'unicité n'existe sur le nom d'une personne : deux personnes homonymes sont permises dans une liste.

## Goals / Non-Goals

**Goals:**
- Copier des personnes et leur budget d'une liste vers une autre, avec un aperçu.
- Ne jamais créer de doublon de nom à l'import.
- Garantir côté serveur que source et cible appartiennent à l'utilisateur.

**Non-Goals:**
- Copier les cadeaux, les tags ou les partages.
- Synchroniser les deux listes après l'import (la copie est indépendante).
- Importer depuis une liste partagée.
- Mettre à jour le budget d'une personne déjà présente.

## Decisions

### Deux étapes, sans état serveur
`GET /listes/{id}/personnes/import?source={sourceId}` affiche l'aperçu ; `POST /listes/{id}/personnes/import` effectue l'import. Aucune donnée n'est conservée entre les deux : le formulaire de l'aperçu n'envoie que `sourceId` et les identifiants cochés. Le panneau de l'onglet envoie l'identifiant de la source par un formulaire `GET` classique, sans HTMX.

### Aperçu
`PersonneService.apercuImport(listeId, sourceId, login)` vérifie que l'utilisateur est propriétaire des deux listes et qu'elles sont différentes, puis renvoie la liste des personnes de la source (tri par nom) avec un drapeau `dejaPresente` calculé contre les noms de la cible. Les noms sont comparés après `trim` et mise en minuscules. Le total du budget des personnes importables est calculé côté serveur.

### Import
`PersonneService.importer(listeId, sourceId, login, ids)` :
1. contrôle les droits sur la cible et la source ; refuse si source et cible sont identiques ;
2. relit les personnes de la source (jamais les valeurs du navigateur) et ne garde que celles dont l'identifiant est dans `ids` ; un identifiant d'une autre liste est ignoré ;
3. relit les noms de la cible **à cet instant** pour écarter les doublons créés depuis l'aperçu ;
4. crée les copies (`new Personne(nom, budget, cible)`) dans la même transaction ;
5. renvoie un compte rendu : importées, ignorées (déjà présentes), budget total ajouté.
Un import sans aucune personne retenue ne crée rien et renvoie un message.

### Comparaison des noms
La clé de comparaison est `nom.trim().toLowerCase(Locale.ROOT)`. Deux homonymes de la source sont comparés uniquement aux noms de la cible avant l'import : aucune ne l'est si la cible n'en contient pas, donc les deux sont importées.

### Droits
Le contrôle réutilise `AccesListeService.proprietaire` pour chaque liste. Les menus ne proposent que les listes dont l'utilisateur est propriétaire (`ListeAchatService.mesListes`), mais le serveur contrôle de nouveau, car un identifiant peut être forgé. Un refus renvoie 403 comme ailleurs. Un lecteur ne peut pas ouvrir l'aperçu ni importer.

### Interface
`personnes.html` ajoute le panneau « Importer depuis une autre liste » (menu des autres listes avec le nombre de personnes, bouton Prévisualiser), absent si l'utilisateur n'a pas d'autre liste. Un gabarit d'aperçu affiche les cases (cochées par défaut, inactives pour une personne déjà présente), le total, et les boutons Importer et Annuler ; le bouton est désactivé quand rien n'est importable. Le compte rendu passe par le message flash habituel de l'onglet.

### Volume
Les personnes d'une liste se comptent en dizaines : une lecture complète de la source est raisonnable. Le nombre de cases d'un aperçu n'est donc pas paginé.

## Risks / Trade-offs

- [Import répété : doublons si le nom a changé entre deux listes] → la règle ne compare que les noms ; un nom modifié donne une nouvelle personne.
- [Identifiants forgés ou source non possédée] → contrôles serveur sur les droits et l'appartenance, tests dédiés.
- [Personne ajoutée à la cible entre l'aperçu et l'import] → doublons recalculés à l'import.
- [Budget global de la cible qui augmente] → le total importé est affiché dans l'aperçu et le compte rendu.
