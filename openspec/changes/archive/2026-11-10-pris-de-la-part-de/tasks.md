## 1. Modèle et dépôt

- [x] 1.1 Ajouter `prisViaLogin` (`@Column(name = "pris_via_login", length = 50)`, nullable) à `Souhait` et adapter `nomPreneurAffiche()` pour renvoyer « nom (via login) » quand il est renseigné
- [x] 1.2 Ajouter le paramètre `via` (nullable) à `SouhaitRepository.prendre` et remettre `prisViaLogin` à nul dans `SouhaitRepository.liberer`
- [x] 1.3 Mettre à jour les appels existants de `prendre` (`SouhaitService.prendreSansControle`) avec `via = null`

## 2. Services

- [x] 2.1 Extraire la validation du nom (`WishlistPubliqueService.nomInvite`) dans une méthode statique de `SouhaitService` et la faire appeler par `WishlistPubliqueService`
- [x] 2.2 Ajouter `SouhaitService.prendrePour(wishlistId, souhaitId, login, nomSaisi)` : contrôle `acces.lecteur`, nom validé, prise atomique avec `invite=false` et `via=login`, message d'erreur identique à la prise concurrente
- [x] 2.3 Étendre `confirmationRequise` d'un indicateur « pris pour un tiers » (faux requis) et le renseigner dans `libererSansControle` depuis `prisViaLogin`

## 3. Web et interface

- [x] 3.1 Ajouter `POST /wishlists/{id}/souhaits/{sid}/pris-pour` (`nom`, `page`) dans `SouhaitController`, erreurs métier en flash, redirection vers la page courante
- [x] 3.2 Ajouter dans `wishlist-lecteur.html` le bouton « De la part de… » (`data-prendre-pour-url`) à côté de « Je le prends » et le `<dialog id="dialogue-pour">` avec formulaire POST (CSRF via `th:action`)
- [x] 3.3 Étendre `app.js` : ouverture du dialogue `dialogue-pour` sans lecture ni écriture de `localStorage` ; généraliser la fermeture à la boîte de dialogue contenant le bouton
- [x] 3.4 Vérifier le CSS de `tuile-pied` (déjà en `flex-wrap`, aucun changement nécessaire)

## 4. Transfert et documentation

- [x] 4.1 Ajouter la colonne `pris_via_login` (texte facultatif, 50) à la table `souhait` de `Tables.java` ; passer `Tables.VERSION` à 2 (un export antérieur est refusé à l'import)
- [x] 4.2 Mettre à jour `docs/database.puml` (colonne `pris_via_login`)

## 5. Tests

- [x] 5.1 Tests de service : prise pour un tiers (affichage « Bob (via alice) »), nom vide / trop long / nettoyage, article déjà pris, propriétaire et non-lecteur refusés (403), prise concurrente
- [x] 5.2 Tests de libération : prise pour un tiers libérée sans confirmation par un lecteur, un compte connecté et un invité du lien public ; effacement du nom et du via ; règles existantes inchangées (`confirmationRequise`)
- [x] 5.3 Tests web (`WishlistsWebTest`) : route `pris-pour`, CSRF requis, bouton présent sur la vue lecteur et absent de la vue publique et propriétaire, vue propriétaire toujours aveugle
- [x] 5.4 Tests de transfert : export/import avec `pris_via_login`, refus d'un fichier sans cette colonne ; mettre à jour `ModeleDonneesTest` et `TablesConformiteTest` si nécessaire
- [x] 5.5 Vérifier que les avis (`MODIFIE`, `SUPPRIME`) nomment le tiers sans « via »
- [x] 5.6 Lancer la suite complète avec Maven (`C:\USINE_LOGICIELLE\apache-maven\bin`)

## 6. Documentation et version

- [x] 6.1 Mettre à jour `README.md` (prise « de la part de », libération libre)
- [x] 6.2 Ajouter l'entrée 0.13.0 dans `CHANGELOG.md` ([Added], [Changed]) et passer la version à 0.13.0 dans `pom.xml`
