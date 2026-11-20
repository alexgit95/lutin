## Context

Le cadeau a un destinataire et un indicateur `pourTiers` sans identité associée. La date de remboursement actuelle concerne exclusivement les virements depuis l'épargne. Le nom du tiers et l'état du remboursement reçu ne doivent pas apparaître dans les vues partagées.

## Goals / Non-Goals

**Goals:** Attribuer une avance à une personne de la liste, suivre le reste à recevoir par tiers et enregistrer le règlement intégral de chaque cadeau.

**Non-Goals:** Paiements partiels, règlement groupé, initiation de virements, compte utilisateur pour le tiers ou modification du calcul des budgets.

## Decisions

1. **Conserver l'indicateur et ajouter deux champs facultatifs sur le cadeau.** Une relation vers la personne débitrice identifie le tiers ; une date distincte indique que le prix entier a été reçu. Réutiliser la date du virement d'épargne mélangerait deux flux, et un journal de paiements n'est pas nécessaire pour un remboursement en une fois. Le booléen conserve le sens des cadeaux historiques sans tiers identifié.
2. **Valider l'attribution et les mutations côté serveur.** Toute attribution doit viser une personne de la même liste. Une nouvelle attribution est obligatoire pour un nouvel achat pour tiers, mais un cadeau historique peut encore être modifié sans saisir de tiers tant qu'il reste dans ce mode. Désactiver le mode tiers retire l'attribution. Pour un cadeau réglé, annuler d'abord le règlement avant de modifier le prix, le tiers ou l'éligibilité, ou de supprimer le cadeau. Bloquer la suppression d'une personne encore référencée comme tiers, y compris si elle est destinataire d'un autre cadeau : une suppression en cascade perdrait le suivi.
3. **Séparer les sous-vues « Épargne » et « Tiers ».** Conserver la route, les actions groupées et les alertes actuelles côté épargne. Côté tiers, sommer les prix non réglés par personne en base, avec un détail paginé (non réglés d'abord) et une section séparée pour les cadeaux historiques non attribués. Utiliser les formulaires Thymeleaf/HTMX existants ; actualiser la ligne et le total après une action réussie. Ne pas exposer les nouveaux champs aux lecteurs, y compris dans les fragments.

## Risks / Trade-offs

- Perte ou mutation silencieuse d'un remboursement reçu : verrouiller les mutations qui en changent le sens jusqu'à annulation explicite. Le montant reste dérivé du prix, sans copie susceptible de diverger.
- Ne pas confondre retrait du débiteur et suppression du destinataire : appliquer les garde-fous avant toute suppression directe ou en cascade d'un cadeau réglé.
- Sauvegardes historiques : les nouveaux champs doivent accepter leur absence dans les anciens exports. Les nouvelles sauvegardes doivent restituer l'attribution et la date, sans référence hors de la liste.

## Migration Plan

Ajouter des colonnes nullables par le mécanisme de schéma Hibernate existant, sans inventer d'attribution ni de date. Vérifier la mise à niveau sur SQLite et PostgreSQL et tester la restauration d'un ancien export. Sauvegarder la base avant déploiement ; pour un retour arrière avec restauration, utiliser une sauvegarde antérieure au déploiement, car l'ancienne version ne suit pas les nouveaux champs.

## Open Questions

Aucune décision bloquante pour cette version.