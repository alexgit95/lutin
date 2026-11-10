## Context

Lutin utilise Thymeleaf et des formulaires serveur sans chaine de build front. Le manifeste ne declare pas de cible de partage et le service worker transmet les requetes au reseau sans cache hors connexion. `SouhaitController.nouveau` construit actuellement un formulaire vide et exige deja la propriete de la wishlist. `SouhaitService.completerNom` recupere un titre uniquement lors de l'enregistrement si le nom manque ; `UrlTitleService` fournit la recuperation HTTP avec validations SSRF, limites de taille, delais et controle des redirections.

En developpement, Spring Security utilise une connexion avec session. En production, Cloudflare Access fournit l'identite a chaque requete ; le contexte de securite Spring n'est pas conserve en session. La selection doit rester reservee aux wishlists personnelles et ne manipuler aucune donnee de prise des articles.

## Goals / Non-Goals

**Goals:**

- Recevoir un partage de lien dans Lutin installe sur Android avec Chrome.
- Toujours presenter un choix explicite de wishlist, meme lorsqu'il n'en existe qu'une.
- Pre-remplir le formulaire existant sans creer d'article avant son enregistrement explicite.
- Conserver le partage pendant la connexion et pendant la creation d'une premiere wishlist.
- Reutiliser les droits et validations metier existants, sans schema de donnees supplementaire.

**Non-Goals:**

- iOS, applications natives, reception de fichiers, partage de liens internes a une application, enrichissement produit (image, prix), detection de doublons et mode hors connexion.
- Garantie de presence dans tous les menus de toutes les applications Android : le navigateur, l'installation et le contenu effectivement partage conditionnent la prise en charge.
- Refonte de l'authentification, des politiques Cloudflare, du service worker ou du parcours manuel d'ajout.

## Decisions

### 1. Reception GET declaree dans le manifeste

Ajouter un `share_target` dont l'action est `/partage`, la methode `GET` et les parametres `url`, `title` et `text`. Le partage ouvre uniquement un ecran : il ne constitue pas une creation de ressource metier. GET permet de traverser l'authentification plus simplement qu'un POST externe depourvu de jeton CSRF. Tous les POST internes restent proteges par CSRF.

Conserver le manifeste, son scope, ses icones et son point de depart existants. Ne pas employer `navigator.share()`, qui concerne le partage sortant et non sa reception. Un traitement POST dans le service worker est ecarte : aucun fichier n'est accepte et cela ajouterait une persistance locale et une gestion de reprise inutiles.

### 2. Normalisation centralisee et bornee

Isoler le traitement des donnees partagees dans une petite abstraction testable, sans entite JPA. Les valeurs sont non fiables. Verifier leurs tailles avant recherche d'URL : URL au plus 2048 caracteres, titre brut au plus 2048, texte au plus 8192. Une entree excessive est refusee explicitement, sans recherche reseau ni troncature de l'URL.

Une URL explicite non vide est prioritaire et doit etre valide ; elle n'est pas remplacee silencieusement par un lien du texte si elle est invalide. En l'absence d'URL explicite, rechercher le premier lien HTTP(S) valide du texte avec des regles testees pour les espaces, sauts de ligne et delimitations usuelles. Utiliser les API URI et la validation existante pour le controle final. Ne pas reconstruire ni supprimer les parametres, fragments ou caracteres encodes du lien retenu.

Un partage sans URL exploitable affiche une erreur recuperable avec une action vers les wishlists. Ne pas traiter tout le texte partage comme un nom. Nettoyer le titre des caracteres de controle, reduire les espaces, puis le limiter a la longueur du nom admise par les regles metier actuelles (100 caracteres), sans modifier ces regles.

### 3. Contexte transitoire dans les navigations

Apres normalisation, ne propager que l'URL et le titre utiles, correctement encodes, dans les liens internes vers le choix de wishlist puis vers le formulaire. Ne pas conserver tout le texte brut. Chaque point d'entree revalide les donnees ; un champ cache ou un parametre ne donne aucun droit. Les chemins de retour sont fixes et internes, jamais des destinations libres fournies par le client.

Ne pas ajouter une session de brouillons ni un stockage en base ou dans le navigateur. Les GET sont rejouables et ne creent pas d'article. Le prix reste vide. Plusieurs partages ouverts dans plusieurs onglets ne doivent pas se remplacer via un brouillon global.

Compromis : l'URL partagee peut apparaitre dans l'historique et les journaux d'acces de l'infrastructure. Ne pas ajouter de journalisation applicative des donnees partagees ; servir les pages de ce parcours avec `Cache-Control: no-store` et `Referrer-Policy: no-referrer` pour limiter les traces et les fuites par referent. Cela n'efface pas les historiques ni les journaux externes existants.

### 4. Selection des seules wishlists personnelles

La page `/partage` utilise le service de consultation des wishlists du proprietaire, sans afficher celles simplement partagees avec lui. La selection redirige vers le formulaire d'ajout existant en transmettant le contexte normalise. `AccesWishlistService.proprietaire` reste l'autorite lors de l'ouverture du formulaire et de l'enregistrement. Une wishlist supprimee entre-temps ne provoque aucune creation et permet de revenir au choix avec le contexte partage.

Si aucune wishlist n'existe, presenter sur la page de selection une creation de wishlist avec son contexte en champs caches. Un POST interne dedie peut reutiliser `WishlistService.creer` puis rediriger vers le choix conserve ; ne pas accepter de retour externe arbitraire. Les erreurs de nom conservent le partage. Cette solution evite de modifier tous les ecrans de creation existants.

### 5. Titre fourni d'abord, recuperation reseau en second

Au moment d'ouvrir le formulaire apres selection, utiliser le titre partage nettoye s'il est present. Sinon, tenter une seule recuperation via `UrlTitleService`, hors transaction de base de donnees et apres verification du droit de propriete. Nettoyer et borner aussi le titre recupere. Aucun acces HTTP n'a lieu pour remplir la simple page de selection ni quand un titre exploitable est deja fourni.

Si la recuperation echoue, afficher immediatement apres cette tentative bornee le formulaire avec URL remplie et nom vide, modifiable. Garder les validations et le comportement de complementation deja presents a la soumission ; un nom toujours vide ne permet pas de creer un article sans nom. Ne pas recuperer de titre en boucle ni remplacer un nom corrige lors du reaffichage d'une erreur de validation.

### 6. Reprise d'authentification sans acces anonyme

L'action GET est protegee comme les autres pages utilisateur. En mode local, verifier la reprise de la requete avec ses parametres apres connexion via le mecanisme Spring existant ; n'adapter que ce retour si necessaire. En production, verifier sur l'environnement reel que Cloudflare Access revient a l'URL cible avec sa query string. Ne pas rendre les wishlists publiques et ne pas introduire d'exception CSRF pour contourner une difficulte de connexion.

La conservation apres connexion d'un utilisateur deja inscrit fait partie des criteres d'acceptation. Le parcours de premier onboarding Cloudflare n'est pas refondu : un utilisateur sans compte termine d'abord son inscription puis peut relancer son partage. Si la politique Cloudflare existante ne permet pas la reprise pour un compte inscrit, documenter le blocage et demander une decision explicite avant toute extension de perimetre infrastructure.

## Risks / Trade-offs

- [Prise en charge variable et manifeste installe ancien] -> Recette sur Android Chrome en HTTPS, verification de l'installation et de sa mise a jour ; documenter qu'une reinstallation peut etre necessaire pour la recette.
- [L'application source envoie un texte ou un lien non web] -> Normalisation testee et erreur claire sans creation ni appel reseau.
- [Titre absent, anti-bot ou delai reseau] -> Priorite au titre transmis, reutilisation du service borne et saisie manuelle toujours possible ; le titre n'est pas garanti.
- [Perte du contexte a travers Access] -> Test sur environnement protege avant validation de la fonctionnalite ; aucune promesse fondee uniquement sur MockMvc.
- [Exposition des parametres dans l'historique ou les logs externes] -> Limiter les donnees propagees, ne pas les journaliser dans l'application et appliquer les en-tetes de confidentialite ; pas de stockage de brouillon supplementaire.
- [SSRF et contenu malveillant] -> Validation HTTP(S), refus des identifiants dans l'URL, rendu Thymeleaf echappe et reutilisation des protections reseau existantes, y compris pour chaque redirection.

## Migration Plan

1. Implementer et tester le parcours sans changement de base de donnees ou de pipeline ; les routes doivent etre disponibles avant l'utilisation du manifeste actualise.
2. Mettre a jour le README, le changelog et la version du projet selon les conventions du depot.
3. Deployer avec le mecanisme existant et tester la PWA installee sur Android en HTTPS, avec connexion active puis expiree.
4. En cas de retour arriere, retirer la declaration du manifeste et le parcours associe ; aucune migration de donnees n'est necessaire. Les installations peuvent conserver temporairement une ancienne declaration, ce qui doit etre signale lors du retour arriere.

## Open Questions

- La configuration Cloudflare Access de l'environnement cible conserve-t-elle effectivement l'URL et tous ses parametres lors d'une reconnexion ? La recette sur appareil est un critere de livraison, pas une hypothese validee par la seule conception.