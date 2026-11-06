## Context

L'onglet Personnes (`personnes.html`, `PersonneController.lister`) liste les personnes avec leur budget et trois liens d'action par ligne. Le dashboard calcule déjà « Déjà déboursé » (`CadeauRepository.sommeDebourse`) et « Reste à payer » (`sommeResteAPayer`) pour toute la liste, avec les règles : cadeaux pour un tiers et sans prix exclus, « À acheter » exclu du déboursé. Les cadeaux masqués comptent dans ces totaux. L'onglet est réservé au propriétaire.

## Goals / Non-Goals

**Goals:**
- Ranger les actions d'une personne derrière un sous-menu.
- Afficher par personne le dépensé, le reste à payer et le budget, avec une barre lisible et accessible.
- Cohérence stricte avec les totaux du dashboard.

**Non-Goals:**
- Récapitulatif global en tête de l'onglet (le dashboard l'affiche déjà).
- Tri par consommé ou filtres.
- Toute modification du dashboard ou du schéma.

## Decisions

### Une seule requête agrégée
`CadeauRepository` gagne une requête groupée par personne qui renvoie, pour une liste, `(personneId, depense, resteAPayer)` :

```
select c.personne.id,
       coalesce(sum(case when c.statut <> A_ACHETER then c.prix else 0 end), 0),
       coalesce(sum(case when c.statut =  A_ACHETER then c.prix else 0 end), 0)
from Cadeau c
where c.liste.id = :listeId and c.pourTiers = false and c.prix is not null
group by c.personne.id
```

Une requête par liste, pas par ligne. Les personnes sans cadeau n'ont pas de ligne : le service les complète avec zéro. Les règles sont celles de `sommeDebourse` et `sommeResteAPayer`, ce qui garantit l'égalité des sommes avec le dashboard.
Alternative écartée : une requête par personne, qui multiplierait les lectures avec le nombre de personnes.

### Objet de vue
`PersonneService.lister(listeId, login)` (droit propriétaire) renvoie une liste de `VuePersonneBudget(id, nom, budget, depense, resteAPayer)` triée par nom, avec des accesseurs calculés : total prévu (`depense + resteAPayer`), échelle de la barre (le plus grand du budget et du total prévu), pourcentages des segments et du repère du budget, montant de dépassement (`max(0, prévu - budget)`) et indicateur « sans barre » pour un budget nul. Les pourcentages sont calculés côté serveur pour que le gabarit reste sans logique arithmétique.

### Rendu de la barre
Trois segments dans un conteneur à largeur 100 % : dépensé (teinte pleine), reste à payer (teinte claire), le reste en fond. L'échelle est le budget, ou le total prévu lorsqu'il le dépasse ; dans ce cas un repère vertical marque la position du budget, la barre passe en rouge et « dépassé de X € » s'affiche. Pour un budget à 0, pas de barre : seul le texte est affiché. Le texte de chaque ligne reprend les trois montants, et la barre porte un `role="img"` avec un `aria-label` décrivant les mêmes montants, afin que l'information ne repose pas sur la couleur.

### Sous-menu natif
Chaque ligne contient un `<details class="menu-actions">` dont le `<summary>` est le bouton `⋯`, avec les liens « + Cadeau » et « Modifier » et le formulaire de suppression (confirmation conservée). Un court script de `app.js` referme le menu ouvert au clic en dehors, à l'ouverture d'un autre et à la touche Échap. Sans JavaScript, le menu reste utilisable au clic.
Alternative écartée : l'attribut `popover`, moins bien supporté sur les navigateurs mobiles visés.

### Montants
Le formatage réutilise le fragment `montant` existant. Aucun calcul monétaire n'est fait en virgule flottante : tout reste en `BigDecimal`.

## Risks / Trade-offs

- [Écart avec le dashboard si les règles divergent un jour] → un test compare la somme par personne aux deux totaux du dashboard.
- [Pourcentages arrondis] → affichage des montants exacts à côté, la barre n'étant qu'une aide visuelle.
- [Menu ouvert qui masque la ligne suivante] → le menu se superpose au contenu et se referme au clic ailleurs et sur Échap.
- [Budget nul ou dépassement difficiles à lire] → cas traités explicitement dans le rendu et couverts par des tests.
