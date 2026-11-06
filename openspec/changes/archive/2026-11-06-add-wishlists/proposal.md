## Why

L'application gère les achats de cadeaux (côté offrant) mais pas l'expression des envies (côté bénéficiaire). Une wishlist partageable permet à chacun de dire ce qu'il souhaite, et aux proches de se coordonner sans doublon, sans que le bénéficiaire apprenne ce qui a été pris.

## What Changes

- Nouvelle section « Wishlists » (page distincte des listes d'achat, entrée dans la barre de navigation).
- Un utilisateur crée, renomme et supprime ses wishlists ; le nom est obligatoire et unique par propriétaire.
- Un propriétaire ajoute, modifie et supprime des articles : nom obligatoire, URL et prix facultatifs, avec récupération automatique du titre depuis l'URL (protection SSRF existante).
- Le propriétaire partage une wishlist avec d'autres utilisateurs via une liste déroulante et peut retirer un partage. Seul le propriétaire voit et gère les partages.
- Un utilisateur voit ses wishlists et celles qui lui sont partagées.
- Un lecteur coche un article comme « pris » : le nom du preneur (login) est enregistré et visible de tous les lecteurs, jamais du propriétaire. N'importe quel lecteur peut décocher ; si le décocheur n'est pas le preneur (ou n'est pas identifiable), un avertissement important est affiché avant de libérer l'article.
- Le propriétaire est aveugle à l'état « pris », au nom du preneur et aux avis, y compris côté serveur (aucune donnée dans le HTML ni les réponses HTMX). L'administrateur n'a aucun accès particulier.
- Avis aux lecteurs connectés lorsque le propriétaire modifie ou supprime un article pris, ou supprime une wishlist contenant des articles pris. Un avis se ferme par « OK » (suppression définitive) et expire au bout de 90 jours. Compteur d'avis dans la barre de navigation.
- Le retrait d'un partage supprime les avis du lecteur retiré pour cette wishlist ; ses articles pris restent « pris par ».
- Conception compatible avec une future wishlist publique (lecteur invité sans compte) : le preneur est un nom en texte, sans clé vers `utilisateur`.

Hors périmètre : wishlist publique et invités, suppression de comptes, lien automatique avec les listes d'achat.

## Capabilities

### New Capabilities
- `wishlists`: wishlists, articles, partage, état « pris » nominatif masqué au propriétaire, avis de modification et de suppression.

### Modified Capabilities
<!-- Aucune : les listes d'achat et leur partage ne changent pas. -->

## Impact

- Domaine : entités `Wishlist`, `Souhait`, `WishlistPartage`, `Avis` (tables créées par Hibernate, SQLite et PostgreSQL) ; mise à jour de `docs/database.puml`.
- Services : accès wishlist (propriétaire / lecteur), articles, partages, avis, réutilisation de `UrlTitleService`.
- Web : contrôleurs MVC + HTMX, templates Thymeleaf, barre de navigation et `LayoutAdvice` (compteur d'avis).
- Sécurité : droits vérifiés côté serveur, CSRF inchangé, URL limitées à http/https.
- Documentation et livraison : `README.md`, `CHANGELOG.md`, version du `pom.xml`, tests JUnit 5 + Mockito.
