## 1. Normalisation du partage

- [x] 1.1 Ajouter une abstraction transitoire de donnees partagees et leur normalisation centralisee, avec limites URL 2048, titre brut 2048 et texte 8192 avant traitement ; ne pas creer d'entite ni modifier le schema.
- [x] 1.2 Implementer la priorite de l'URL explicite, l'extraction du premier lien HTTP(S) valide du texte en son absence et le refus explicite d'une URL invalide ; conserver parametres, fragments et encodage du lien sans le tronquer.
- [x] 1.3 Normaliser les titres (controles, espaces, limite metier de 100 caracteres) et ajouter les tests JUnit 5/Mockito des entrees absentes, vides, multiples, malveillantes, delimitees, encodees et aux limites de taille ; verifier l'absence d'appels reseau dans cette couche.

## 2. Reception et choix de wishlist

- [x] 2.1 Ajouter le point d'entree MVC GET `/partage`, protege par l'authentification existante, et une vue Thymeleaf de choix explicite des seules wishlists personnelles, meme s'il n'en existe qu'une.
- [x] 2.2 Afficher un etat recuperable pour un partage sans URL exploitable et transmettre uniquement le contexte utile et encode dans les navigations internes, sans session globale de brouillon ni journalisation du contenu.
- [x] 2.3 Proposer la creation d'une premiere wishlist en l'absence de destination, via un POST CSRF reutilisant `WishlistService.creer`, avec conservation du contexte apres erreur puis retour au choix apres succes.
- [x] 2.4 Ajouter les tests web couvrant zero, une et plusieurs wishlists, l'exclusion des wishlists recues en partage, l'absence de mutation lors des GET, la conservation apres creation et erreur de nom, et le refus des POST sans CSRF.

## 3. Pre-remplissage du formulaire existant

- [x] 3.1 Etendre localement l'ouverture du formulaire d'ajout pour recevoir et revalider le contexte partage, verifier la propriete et pre-remplir URL et titre avec prix vide, tout en conservant l'ajout manuel sans parametres.
- [x] 3.2 Si le titre manque, reutiliser `UrlTitleService` apres verification des droits et hors transaction, nettoyer et borner le resultat ; en cas d'echec afficher un nom vide sans bloquer la saisie manuelle.
- [x] 3.3 Conserver les valeurs corrigees lors d'une erreur de soumission et l'annulation vers la wishlist ; traiter une wishlist supprimee entre selection et ouverture par un retour possible au choix avec le contexte partage.
- [x] 3.4 Ajouter ou completer les tests Java du pre-remplissage, de l'absence de recherche avec titre fourni et avant selection, du titre absent ou indisponible, de la limite a 100 caracteres, des valeurs corrigees, de l'annulation, de l'enregistrement explicite et du parcours manuel.

## 4. Securite et reprise d'authentification

- [x] 4.1 Ajouter les tests de refus 403 pour une destination non possedee, administrateur compris, a l'ouverture et a l'enregistrement ; verifier qu'aucune recuperation reseau ne precede l'autorisation et qu'aucune donnee de prise n'est exposee.
- [x] 4.2 Verifier la reprise de l'URL GET et de ses parametres apres connexion locale pour un compte inscrit ; corriger uniquement le retour de connexion si un test met en evidence une perte de contexte.
- [x] 4.3 Appliquer `Cache-Control: no-store` et `Referrer-Policy: no-referrer` aux pages du parcours ; tester le rendu echappe, les destinations internes fixes, les requetes non authentifiees et l'independance de deux partages ouverts dans deux onglets.
- [x] 4.4 Completer les tests de recuperation du titre pour verifier la reutilisation des protections SSRF, dont les adresses interdites et redirections, sans acces reseau externe reel ni modification des regles generales de securite.

## 5. Declaration PWA et documentation

- [x] 5.1 Declarer `share_target` GET vers `/partage` avec `url`, `title` et `text` dans le manifeste, sans modifier le scope, le point de depart ou le service worker ; ajouter un test du manifeste servi avec ses autres attributs preserves.
- [x] 5.2 Mettre a jour le README avec le parcours Android, l'installation HTTPS via Chrome, les limites des applications sources et du titre, l'absence de mode hors connexion, le premier onboarding et les consignes de mise a jour ou reinstallation pour la recette.
- [x] 5.3 Incrementer la version dans le POM et le changelog selon les conventions existantes ; decrire l'impact fonctionnel et technique dans les rubriques Keep a Changelog appropriees, sans toucher aux pipelines.

## 6. Verification et recette Android

- [x] 6.1 Executer les tests cibles puis la suite de non-regression avec exclusivement `C:/USINE_LOGICIELLE/apache-maven/bin/mvn` (ou son chemin Bash equivalent) ; verifier les parcours wishlist existants et les deux modes d'authentification sans lancer de chaine de build front.
- [x] 6.2 Verifier le rendu mobile et desktop du choix, de l'etat vide, des erreurs et du formulaire ; utiliser des liens et titres longs pour detecter les debordements et verifier que toutes les actions restent accessibles.
- [ ] 6.3 Sur Android Chrome avec la PWA installee et a jour en HTTPS, verifier le menu natif depuis un navigateur et une autre application partageant un lien, puis choix explicite, URL/titre, modifications, annulation et enregistrement dans la bonne wishlist ; consigner appareil et versions testes.
- [ ] 6.4 Sur l'environnement protege par Cloudflare Access, renouveler une authentification expiree pour un compte inscrit et verifier la conservation de l'URL (parametres et fragment inclus dans la valeur partagee) et du titre ; si elle echoue, consigner le blocage et demander une decision avant toute modification d'infrastructure. Ne pas declarer cette recette validee a partir des seuls tests serveur.
- [x] 6.5 Executer `openspec validate partage-android-wishlist --strict` et consigner les resultats automatises et manuels ; laisser explicitement non validee toute recette appareil ou Cloudflare non executee.

## Resultats de verification - 2026-11-10

- Maven du depot uniquement : tests cibles de normalisation, reception, formulaire, authentification locale, Cloudflare, titres et manifeste, puis suite complete : **429 tests, 34 suites, 0 echec, 0 erreur, 0 ignore**. Les tests web `PartageWebTest` et `PwaTest` ont ete relances avec succes apres le correctif de mise en page.
- `mvn -DskipTests package -q` : JAR executable `target/lutin-0.15.0.jar` construit avec succes. Aucun changement de dependance, de schema ou de pipeline.
- Navigateur local sur `http://localhost:8182`, profil dev, base de recette distincte `target/lutin-partage-recette.db` : inscription/connexion, etat sans wishlist, creation avec conservation du partage, choix explicite et ouverture du formulaire verifies. URL avec `%2F`, `+`, parametres et fragment conservee exactement ; titre partage et prix vide verifies.
- Verification Playwright par snapshots et geometrie DOM : etat vide a 360/1280 px ; choix, formulaire et erreur a 320/360/1280 px avec nom de wishlist sans espaces, URL proche de 2048 caracteres et titre trop long. Un debordement du nom de wishlist a ete corrige par des regles CSS limitees au nouvel ecran ; le JAR reconstruit sert le correctif et aucune largeur de page ou action ne deborde ensuite. Les captures d'ecran n'ont pas ete disponibles dans le navigateur integre : controle automatise de mise en page, pas de validation visuelle par capture.
- Validation stricte OpenSpec reussie et `git diff --check` sans erreur.
- Diagnostic d'environnement : VS Code signale une resolution impossible du parent Maven depuis Maven Central (DNS), alors que le Maven impose compile, teste et construit correctement. Aucun reglage utilisateur modifie.
- **Non executes, non valides** : tache 6.3 (menu natif sur un vrai Android avec PWA installee en HTTPS) et tache 6.4 (reconnexion reelle via Cloudflare Access). Les tests serveur Cloudflare ne valident pas la conservation de la query string par la page de connexion externe. Aucune modification de politique Cloudflare n'a ete faite.