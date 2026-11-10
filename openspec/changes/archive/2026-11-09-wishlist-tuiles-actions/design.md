## Context

Les tuiles d'articles sont rendues par trois gabarits proches : `wishlist.html` (propriétaire : Voir, Modifier, Supprimer), `wishlist-lecteur.html` et `wishlist-publique.html` (Voir et « Je le prends »). Le projet a déjà `copierLien` (lit `data-lien`, utilise l'API presse-papiers, retombe sur une boîte `prompt`, confirme par `confirmerSurBouton`) et un gestionnaire de clic délégué sur `[data-copier]`. La suppression d'un article est `POST /wishlists/{id}/souhaits/{sid}/supprimer` ; elle gère déjà les avis aux lecteurs et la confirmation côté navigateur.

## Goals / Non-Goals

**Goals:**
- Un clic sur la tuile ouvre l'URL de l'article dans un nouvel onglet.
- Copier l'URL en un clic, partout où une tuile est affichée.
- Supprimer un article uniquement depuis son écran de modification.

**Non-Goals:**
- Aucun changement de route, de service, de droit ou de schéma de données.
- Pas de nouvelle action de prise ou de partage.
- Pas de copie de l'URL côté serveur ni de récupération du titre.

## Decisions

### Lien étiré sur le titre
Le titre de la tuile devient un lien `<a target="_blank" rel="noopener noreferrer">` dont un pseudo-élément `::after` couvre toute la tuile (tuile en `position: relative`). Les boutons de pied de tuile passent au-dessus (`position: relative` et `z-index`). Cela évite d'imbriquer des boutons ou des formulaires dans un lien. Sans URL, le titre reste un simple texte et la tuile n'est pas cliquable.
Alternative écartée : envelopper la tuile dans un `<a>`, invalide en HTML avec des boutons et des formulaires à l'intérieur.

### Bouton Copier
Un bouton `type="button"` avec `data-copier` et `data-lien` portant l'URL de l'article, visible seulement si l'URL existe. Il réutilise `copierLien` et son message « Lien copié ». L'API presse-papiers exige un contexte sécurisé (HTTPS en prod, localhost en dev) ; sinon le repli actuel affiche l'URL dans une boîte de dialogue.

### Suppression dans l'écran de modification
`souhait-form.html` affiche, en mode modification seulement, un second formulaire POST vers la route de suppression existante, placé hors du formulaire d'enregistrement (pas de formulaires imbriqués), avec `onsubmit="return confirm(...)"` et le style « danger » actuel. La page « Nouvel article » ne le montre pas. Le serveur et ses avis ne changent pas.

### Rendu commun
Le fragment de tuile n'est pas factorisé dans ce changement : les trois gabarits reçoivent la même modification localisée pour limiter le risque. Une factorisation éventuelle est un travail distinct.

## Risks / Trade-offs

- [Clic sur « Modifier » ou « Copier » interprété comme clic sur la tuile] → boutons au-dessus du lien étiré, test d'absence de navigation du côté du navigateur impossible en MockMvc : vérification par la structure du HTML et revue visuelle.
- [Suppression plus lente] → souhaité : une étape de plus évite les suppressions accidentelles.
- [Copie impossible hors contexte sécurisé] → repli par boîte de dialogue, déjà en place pour le partage.
- [Liens sortants] → `rel="noopener noreferrer"` pour ne pas exposer la page d'origine.
