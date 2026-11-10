## Why

Sur une tuile d'article, « Voir » et « Supprimer » encombrent l'écran : on ouvre le lien en cliquant sur un bouton à part, et la suppression est à un clic de l'erreur. On veut qu'un clic sur l'article ouvre son lien, pouvoir copier ce lien en un geste, et ne supprimer que depuis l'écran de modification.

## What Changes

- Un clic sur une tuile d'article qui a une URL ouvre cette URL dans un **nouvel onglet** (`target="_blank"`, `rel="noopener noreferrer"`). Une tuile sans URL n'est pas cliquable. Les boutons de la tuile restent cliquables.
- Le bouton **« Voir » est retiré**.
- Un bouton **« Copier »** copie l'URL de l'article dans le presse-papiers et confirme par « Lien copié ». Il n'apparaît que pour un article qui a une URL.
- Le bouton **« Modifier » est conservé** (propriétaire).
- Le bouton **« Supprimer » quitte la tuile** : la suppression se fait depuis l'écran « Modifier l'article », avec la même confirmation. Elle n'apparaît pas sur « Nouvel article ». Le comportement serveur (suppression, avis aux lecteurs d'un article pris) ne change pas.
- Le clic sur l'article et « Copier » valent pour le propriétaire, les lecteurs d'une wishlist partagée et la page publique `/p/{jeton}`. « Je le prends » reste inchangé.

## Capabilities

### New Capabilities
<!-- Aucune -->

### Modified Capabilities
- `wishlists`: la gestion des articles (suppression depuis l'écran de modification) et le rendu des tuiles (clic sur l'article, bouton Copier, plus de bouton « Voir »).

## Impact

- Code : gabarits `wishlist.html`, `wishlist-lecteur.html`, `wishlist-publique.html` et `souhait-form.html`, script `app.js` (réutilise `copierLien`), `app.css` (tuile cliquable). Aucun contrôleur, service ni schéma de données ne change.
- Sécurité : liens sortants avec `rel="noopener noreferrer"` ; une URL d'article n'est jamais appelée par le serveur à cette occasion.
- Tests : web (présence et absence des boutons selon le rôle, suppression depuis le formulaire, confirmation, absence de copie sans URL).
- Documentation : `README.md`, `CHANGELOG.md`, version du `pom.xml`.
