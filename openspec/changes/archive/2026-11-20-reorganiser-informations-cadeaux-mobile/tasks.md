## 1. Réorganisation mobile

- [x] 1.1 Ajouter des hooks explicites aux cellules du nom, des tags, du prix et du statut pour permettre leur placement mobile sans dépendre de l'ordre des cellules.
- [x] 1.2 Réorganiser la grille au breakpoint mobile : nom, tags, puis prix, statut prioritaire et actions compactes sur une rangée ; ne pas réserver d'espace si aucun tag n'est présent.
- [x] 1.3 Préserver la disposition desktop, les libellés, les commandes du propriétaire et le statut en lecture seule du lecteur.

## 2. Tests et documentation

- [x] 2.1 Ajouter ou adapter les tests web pour vérifier la structure des cellules et préserver l'absence d'actions pour le lecteur.
- [x] 2.2 Mettre à jour le README, incrémenter la version Maven et décrire la modification dans le changelog.
- [x] 2.3 Exécuter les tests ciblés avec le binaire Maven imposé par le projet.
- [x] 2.4 Vérifier dans un navigateur les largeurs 320, 375 et 768 px ainsi qu'un écran desktop : tags sous le nom, statut plus large, absence de rangée vide sans tag, aucun défilement horizontal et aucune régression de la vue lecteur.
- [x] 2.5 Valider les artefacts OpenSpec en mode strict.