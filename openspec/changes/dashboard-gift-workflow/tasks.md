## 1. Chargement et projection des cadeaux

- [x] 1.1 Adapter la projection du dashboard pour exposer une clé stable de destinataire aux propriétaires et lecteurs autorisés, tout en gardant les champs du cadeau masqué absents de la projection lecteur.
- [x] 1.2 Ajouter le chargement du lot suivant avec contrôle d'accès serveur, maintien du filtre par tag, ordre stable et indication de fin de résultats; conserver la pagination JPA bornée.
- [x] 1.3 Ajouter les tests du service pour les groupes homonymes, les lots, la frontière d'un groupe, les filtres et les projections lecteur des cadeaux masqués.

## 2. Présentation du dashboard

- [x] 2.1 Remplacer les contrôles de pagination visibles par un marqueur de chargement continu qui ajoute les cadeaux au fil du défilement sans retirer les éléments déjà affichés.
- [x] 2.2 Afficher les cadeaux sous des groupes de destinataires distincts, poursuivre les groupes traversant une frontière de lot et utiliser un groupe neutre pour les cadeaux masqués présentés aux lecteurs.
- [x] 2.3 Ajouter une barre compacte fixée au bas de l'écran avec les cinq comptes par statut, réserver l'espace nécessaire pour ne pas masquer le contenu et préserver l'usage mobile et clavier.
- [x] 2.4 Afficher les erreurs de chargement d'un lot avec une possibilité de nouvelle tentative, sans perdre les cadeaux déjà chargés.

## 3. Mise à jour et animations du workflow

- [x] 3.1 Adapter la réponse HTMX d'un changement de statut pour ne remplacer que la ligne du cadeau et les comptes de workflow calculés côté serveur, sans réinitialiser les lots chargés ni la position de défilement.
- [x] 3.2 Animer une représentation temporaire du cadeau vers son statut cible et l'évolution des compteurs uniquement après confirmation du serveur; garder la ligne dans son groupe.
- [x] 3.3 Respecter `prefers-reduced-motion`, exposer les nouveaux comptes de façon accessible et conserver un comportement fonctionnel sans animation.
- [x] 3.4 Ajouter ou mettre à jour les tests web pour le chargement continu, les comptes globaux hors filtre, les autorisations lecteur/propriétaire et l'absence de fuite des champs masqués.

## 4. Documentation et validation

- [x] 4.1 Mettre à jour le README, ajouter l'entrée correspondante au CHANGELOG et incrémenter la version du pom.xml.
- [x] 4.2 Exécuter les tests avec le Maven situé dans `C:\USINE_LOGICIELLE\apache-maven\bin` et corriger toute régression liée au changement.
- [x] 4.3 Vérifier le chargement continu à travers plusieurs lots, le maintien d'un groupe entre deux lots, le changement de statut avec barre persistante et l'affichage à réduction des animations activée.
