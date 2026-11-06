## Context

`DashboardService` construit la vue d'une liste pour le propriétaire ou un lecteur. Pour un lecteur, `VueCadeau.depuis` réduit un cadeau masqué à son nom. Les cadeaux sont chargés par `CadeauRepository.findByListeId` (et `findByListeIdAndTagsId` pour un filtre par tag), triés par `personne.nom, personne.id, nom, id`, puis regroupés par `personneId`. Un cadeau masqué d'un lecteur a `personneId` nul : il forme un groupe neutre à sa place dans le tri, donc plusieurs sections « Cadeaux masqués » s'intercalent entre les personnes. Le filtre par tag, lui, s'applique aux tags réels du cadeau.

## Goals / Non-Goals

**Goals:**
- Un cadeau masqué n'expose rien d'autre que son existence à un lecteur.
- Sa place dans la liste ne dépend ni du nom du cadeau ni de la personne destinataire.
- Aucun moyen de deviner ses tags par le filtre.

**Non-Goals:**
- Masquer les montants des totaux (accepté par le propriétaire).
- Empêcher la déduction du prix quand il n'y a qu'un seul cadeau masqué.
- Lier un lecteur à une personne destinataire.

## Decisions

### Projection sans nom
`VueCadeau.depuis(c, lecteur)` renvoie, pour un cadeau masqué et un lecteur, un `VueCadeau` dont tous les champs sont nuls (nom compris), `masque` vrai et aucune liste de tags. Le gabarit affiche le libellé fixe « Cadeau masqué ». Le nom n'est jamais copié dans l'objet de vue, donc il ne peut pas fuiter dans le HTML.

### Requête et tri propres au lecteur
Un propriétaire garde `findByListeId` et son tri. Un lecteur utilise une requête distincte qui ordonne d'abord les cadeaux visibles, puis les masqués :

```
order by c.masque, 
         case when c.masque = true then '' else c.personne.nom end,
         case when c.masque = true then 0  else c.personne.id  end,
         case when c.masque = true then '' else c.nom end,
         c.id
```

Pour un cadeau masqué, les clés de tri sont constantes : l'ordre ne dépend que de l'identifiant. Le regroupement par `personneId` donne alors un seul groupe neutre, en fin de liste, y compris à travers les lots de 50 du chargement continu.
Alternative écartée : deux requêtes (visibles puis masqués) fusionnées en mémoire, qui compliquerait la pagination par lots.

### Filtre par tag
Pour un lecteur, la requête avec tag ajoute `c.masque = false` : un cadeau masqué est traité comme sans tag et n'apparaît jamais sous un filtre. Sans filtre, il apparaît dans le groupe masqué. Le propriétaire n'est pas concerné.

### Totaux et répartition
Inchangés : `sommeDebourse`, `sommeResteAPayer` et `sommeBudgets` incluent les masqués ; `compterParStatut(listeId, false)` les exclut pour un lecteur. Le compteur « Cadeaux » reste le total de la page de résultats.

### Gabarit
`liste-fragments.html` : la ligne masquée d'un lecteur affiche « Cadeau masqué » seul, sans le nom et sans l'attribut `data-statut`.

## Risks / Trade-offs

- [Déduction du prix avec un seul cadeau masqué : statut et montants changent ensemble] → limite connue, documentée dans le README.
- [Oubli d'une des deux requêtes lecteur (avec ou sans tag)] → tests dédiés qui vérifient ordre et absence de tags, ainsi que l'absence du nom dans le HTML.
- [Tri conditionnel en JPQL moins lisible] → requête commentée et couverte par un test d'ordre indépendant des noms.
