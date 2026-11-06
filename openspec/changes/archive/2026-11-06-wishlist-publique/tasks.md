## 1. Modèle de données

- [x] 1.1 `Wishlist.jetonPublic` (nullable) avec index unique séparé, et `Souhait.prisParInvite` (booléen nullable, nul = faux)
- [x] 1.2 `WishlistRepository.findByJetonPublic`, requêtes `@Modifying` de prise et de libération portant le nom et l'indicateur d'invité
- [x] 1.3 Mettre à jour `docs/database.puml`
- [x] 1.4 Vérifier le démarrage sur une copie de la base de dev SQLite existante (ajout des deux colonnes et de l'index)

## 2. Services

- [x] 2.1 `WishlistService` : activer (jeton `SecureRandom` 128 bits base64url), régénérer, désactiver, réservés au propriétaire
- [x] 2.2 Résolution d'une wishlist par jeton (404 identique pour inconnu et révoqué) et détermination du profil du visiteur (propriétaire, lecteur partagé, autre compte, invité)
- [x] 2.3 `SouhaitService` : prise et libération par jeton, nom d'invité nettoyé (non vide, 100 caractères, sans caractères de contrôle), login automatique pour un compte connecté
- [x] 2.4 `confirmationRequise` tenant compte de l'indicateur d'invité
- [x] 2.5 Vue publique : objet de vue sans propriétaire, mention « (invité) » dans le nom affiché
- [x] 2.6 Tests de service : cycle du jeton, jeton révoqué, nom invalide, prise concurrente, règle de confirmation (compte, invité, invité au login identique), articles pris conservés après retour en privé

## 3. Web et sécurité

- [x] 3.1 `SecurityConfig` : `/p/**` en accès libre, CSRF maintenu
- [x] 3.2 Contrôleur public `/p/{jeton}` : consultation, prise, libération avec page de confirmation, redirections du propriétaire et des lecteurs partagés
- [x] 3.3 En-têtes `Referrer-Policy`, `X-Robots-Tag` et `Cache-Control` sur `/p/**`
- [x] 3.4 Gabarit public (titre, tuiles, en-tête minimal, popup `<dialog>`, nom d'invité en texte)
- [x] 3.5 Onglet Réglages : interrupteur avec avertissement, lien affiché, bouton Régénérer ; routes réservées au propriétaire
- [x] 3.6 Onglet Articles : bouton Partager seulement si publique, `navigator.share` avec repli copie, popup et préremplissage `localStorage` dans `app.js`
- [x] 3.7 Tests web : accès sans connexion, 404, redirections, absence de login du propriétaire dans le HTML, en-têtes, CSRF, 403 de gestion pour un lecteur et un administrateur, bouton Partager conditionnel

## 4. Livraison

- [x] 4.1 Mettre à jour `README.md` (wishlist publique, limite d'aveuglement, règle Cloudflare recommandée sur `/p/*`)
- [x] 4.2 Mettre à jour `CHANGELOG.md` et incrémenter la version dans `pom.xml` et le changelog
- [x] 4.3 Exécuter la suite de tests avec Maven de `C:\USINE_LOGICIELLE\apache-maven\bin`
