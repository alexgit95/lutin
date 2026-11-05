## Context

Le dashboard est une application MVC Thymeleaf/HTMX. `DashboardService` charge actuellement les cadeaux par pages de 50, triés par personne puis par nom; les comptes par statut sont calculés séparément de la pagination et du filtre par tag. Pour un lecteur, les cadeaux masqués sont projetés sans destinataire ni statut et ne sont pas inclus dans les comptes. Le changement de statut remplace aujourd’hui le contenu complet du dashboard, ce qui ferait perdre les lots déjà chargés et la position de défilement.

## Goals / Non-Goals

**Goals:**

- Regrouper les cadeaux visibles par destinataire et les parcourir sans contrôles de pagination visibles.
- Charger les cadeaux progressivement par lots afin de maintenir une consommation mémoire bornée.
- Garder une barre de workflow compacte visible pendant le défilement, avec des comptes globaux indépendants du filtre par tag.
- Donner un retour animé lors d’un changement de statut sans déplacer durablement le cadeau hors de son groupe.
- Préserver les règles de lecture seule et de masquage des cadeaux partagés.

**Non-Goals:**

- Modifier le workflow métier, son ordre ou les transitions autorisées.
- Filtrer les compteurs par tag ou par page chargée.
- Charger tous les cadeaux en mémoire au premier affichage.
- Modifier les entités, le schéma de base de données, l’API publique ou ajouter une dépendance JavaScript.

## Decisions

1. **Conserver les lots serveur, supprimer seulement la pagination visible.** Le premier rendu charge un lot; un marqueur observé en bas de la liste demande le suivant, en conservant la liste et le filtre de tag actifs. Le service reste paginé côté base. Les résultats restent triés par destinataire puis par nom. L’identifiant du destinataire sert à distinguer deux personnes homonymes et à éviter de répéter un en-tête lorsque le même groupe traverse la frontière entre deux lots. Une erreur de chargement laisse les cadeaux déjà rendus en place et permet de réessayer.

2. **Garder les compteurs globaux.** La barre affiche les nombres de toute la liste, même lorsqu’un filtre par tag est actif ou que seuls certains lots sont chargés. Elle réutilise la règle existante: le propriétaire compte ses cadeaux masqués; un lecteur ne voit pas ces cadeaux dans les comptes. La barre reste en lecture seule pour les lecteurs.

3. **Isoler les cadeaux masqués dans les groupes.** Un cadeau masqué vu par un lecteur ne fournit toujours ni identifiant de destinataire ni statut. Il ne peut donc pas être regroupé avec sa personne réelle; il est rendu dans un groupe neutre, sans ajouter de métadonnée révélatrice au HTML ou aux réponses de chargement.

4. **Mettre à jour uniquement les fragments touchés par un changement de statut.** La réponse HTMX remplace la ligne du cadeau et rafraîchit les compteurs de la barre, sans remplacer la liste déjà chargée ni réinitialiser le défilement. Les compteurs du fragment sont recalculés côté serveur afin de rester fiables.

5. **Animer une représentation temporaire du cadeau.** Lors d’une mise à jour acceptée, une représentation visuelle part de la ligne du cadeau vers l’étape cible de la barre; les compteurs source et cible changent avec une animation courte. La ligne réelle reste dans son groupe de destinataire. Une mise à jour sans JavaScript conserve le comportement de formulaire actuel sans animation.

6. **Respecter l’accessibilité et l’espace d’affichage.** La barre compacte reste accessible au clavier, annonce les nouveaux comptes aux technologies d’assistance sans annonces répétitives et ne masque pas les derniers cadeaux. La préférence `prefers-reduced-motion` supprime le déplacement et les mouvements de compteurs tout en affichant immédiatement les valeurs finales. La mise en page tient compte de la zone sûre en bas des écrans mobiles.

## Risks / Trade-offs

- [Risque] Un groupe peut s’étendre sur plusieurs lots. → Continuer le groupe existant en utilisant l’identifiant du destinataire, sans dupliquer son en-tête.
- [Risque] Le chargement continu peut échouer ou être déclenché plusieurs fois à proximité de la fin de liste. → N’autoriser qu’une requête de lot à la fois, conserver les éléments rendus en cas d’erreur et exposer une action de nouvelle tentative.
- [Risque] La réponse complète du dashboard réinitialiserait les cadeaux chargés et la position de défilement. → Limiter les mises à jour HTMX à la ligne modifiée et à la barre du workflow.
- [Risque] La barre fixe peut recouvrir du contenu ou distraire. → Réserver l’espace correspondant, limiter la durée et l’amplitude des animations, et respecter la réduction des animations.
- [Risque] Les regroupements pourraient divulguer le destinataire d’un cadeau masqué à un lecteur. → Ne jamais utiliser un identifiant ou un nom de personne pour ces cadeaux côté lecteur; les rendre dans un groupe neutre.

## Migration Plan

Aucune migration de données ou de base n’est nécessaire. Déployer ensemble les changements du contrôleur/service, des fragments Thymeleaf, des styles et du JavaScript. Un retour à la version précédente ne requiert qu’un redéploiement; les données persistées restent compatibles.

## Open Questions

Aucune décision produit bloquante ne reste ouverte. La durée et la courbe de l’animation pourront être ajustées lors de l’implémentation, avec une préférence initiale pour un mouvement bref et discret.
