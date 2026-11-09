## 1. Gabarits

- [x] 1.1 `wishlist.html` : titre de la tuile en lien étiré vers l'URL (nouvel onglet, `rel="noopener noreferrer"`) quand elle existe, retrait de « Voir » et de « Supprimer », ajout de « Copier » (`data-copier`, `data-lien`) quand l'URL existe, « Modifier » conservé
- [x] 1.2 `wishlist-lecteur.html` et `wishlist-publique.html` : même lien étiré et même bouton « Copier », retrait de « Voir », « Je le prends » inchangé
- [x] 1.3 `souhait-form.html` : en modification seulement, second formulaire de suppression hors du formulaire d'enregistrement, avec confirmation, vers la route existante ; rien sur l'écran d'ajout
- [x] 1.4 `app.css` : tuile en `position: relative`, pseudo-élément du lien couvrant la tuile, boutons au-dessus ; style du bouton de suppression de l'écran de modification
- [x] 1.5 `app.js` : vérifier que `[data-copier]` couvre le nouveau bouton et que le message reste « Lien copié »

## 2. Tests

- [x] 2.1 Tests web propriétaire : lien étiré vers l'URL avec `target="_blank"` et `rel="noopener noreferrer"`, « Copier » avec l'URL, absence de « Voir » et de « Supprimer » sur la tuile, « Modifier » présent
- [x] 2.2 Tests web article sans URL : ni lien ni « Copier »
- [x] 2.3 Tests web lecteur et page publique : lien étiré et « Copier », absence de « Voir », « Je le prends » présent
- [x] 2.4 Tests web écran de modification : bouton de suppression présent avec confirmation, absent sur « Nouvel article » ; la suppression fonctionne et supprime les avis comme avant
- [x] 2.5 Vérifier que les tests existants des wishlists passent, en adaptant ceux qui cherchaient « Voir » ou « Supprimer » sur la tuile

## 3. Livraison

- [x] 3.1 Mettre à jour `README.md` (tuiles, clic, copie, suppression depuis la modification)
- [x] 3.2 Mettre à jour `CHANGELOG.md` et incrémenter la version dans `pom.xml` et le changelog (mentionner qu'aucune variable d'environnement ni donnée n'est touchée)
- [x] 3.3 Exécuter la suite de tests avec Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
