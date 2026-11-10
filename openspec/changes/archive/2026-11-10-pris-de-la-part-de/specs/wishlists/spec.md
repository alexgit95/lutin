## ADDED Requirements

### Requirement: Prise pour un tiers
Le système SHALL permettre à un lecteur partagé connecté de prendre un article libre « de la part de » un tiers, depuis la vue de la wishlist partagée, via un bouton « De la part de… » placé à côté de « Je le prends ». Ce bouton SHALL ouvrir la même popup de saisie de nom que la wishlist publique, sans préremplissage. Le nom saisi SHALL être obligatoire, nettoyé des caractères de contrôle et limité à 100 caractères. Le système SHALL enregistrer le nom du tiers comme preneur et le login du lecteur connecté comme auteur de la prise, et SHALL afficher « Pris par <nom> (via <login>) ». Le bouton SHALL être absent de la wishlist publique et de la vue du propriétaire. Un article déjà pris SHALL NOT pouvoir être pris pour un tiers.

#### Scenario: Prise pour un tiers
- **WHEN** un lecteur partagé clique « De la part de… » sur un article libre, saisit « Bob » et valide
- **THEN** l'article est « pris par » Bob via le login du lecteur et les lecteurs voient « Pris par Bob (via alice) »

#### Scenario: Nom absent
- **WHEN** le lecteur valide la popup avec un nom vide ou composé uniquement de blancs
- **THEN** la prise est refusée avec le message « Votre nom est obligatoire » et l'article reste libre

#### Scenario: Nom trop long
- **WHEN** le lecteur saisit un nom de plus de 100 caractères
- **THEN** la prise est refusée avec un message d'erreur et l'article reste libre

#### Scenario: Popup sans préremplissage
- **WHEN** le lecteur ouvre la popup « De la part de… »
- **THEN** le champ du nom est vide, même si un nom d'invité a été mémorisé dans le navigateur

#### Scenario: Article déjà pris
- **WHEN** un lecteur tente de prendre pour un tiers un article déjà pris
- **THEN** la prise est refusée et le message indique par qui l'article est pris

#### Scenario: Prise concurrente
- **WHEN** un lecteur prend un article pour un tiers au même moment qu'un autre lecteur le prend
- **THEN** un seul réussit et l'autre reçoit un message indiquant par qui l'article est pris

#### Scenario: Propriétaire et non-lecteur exclus
- **WHEN** le propriétaire ou un utilisateur sans accès à la wishlist tente une prise pour un tiers
- **THEN** la requête est refusée (403) et rien n'est modifié

#### Scenario: Pas de bouton hors vue lecteur partagé
- **WHEN** la wishlist est consultée par son propriétaire ou par le lien public
- **THEN** aucun bouton « De la part de… » n'est présent

#### Scenario: Avis
- **WHEN** le propriétaire modifie ou supprime un article pris pour un tiers
- **THEN** l'avis envoyé aux lecteurs nomme le tiers comme preneur, sans mention « via »

## MODIFIED Requirements

### Requirement: Décochage avec avertissement
Le système SHALL permettre à tout lecteur ou visiteur de décocher un article pris. Le système SHALL afficher un avertissement important nommant le preneur et exiger une confirmation avant de libérer l'article, sauf si le visiteur est connecté, que son login est le nom du preneur et que le preneur n'est pas un invité, ou si l'article a été pris pour un tiers (prise « de la part de »). Un article pris pour un tiers SHALL pouvoir être libéré sans avertissement par tout lecteur ou visiteur, y compris un invité du lien public.

#### Scenario: Décochage de son propre article
- **WHEN** un utilisateur connecté décoche un article dont le preneur est son login et n'est pas un invité
- **THEN** l'article est libéré sans avertissement

#### Scenario: Décochage de l'article d'un autre
- **WHEN** un lecteur décoche un article pris par un autre nom
- **THEN** un avertissement important nomme le preneur et l'article n'est libéré qu'après confirmation

#### Scenario: Décochage par un invité
- **WHEN** un visiteur non connecté décoche un article pris
- **THEN** l'avertissement est affiché avant toute libération

#### Scenario: Preneur invité portant un login existant
- **WHEN** un utilisateur connecté décoche un article pris par un invité dont le nom est identique à son login
- **THEN** l'avertissement est affiché avant la libération

#### Scenario: Décochage d'un article pris pour un tiers
- **WHEN** n'importe quel lecteur ou visiteur, invité compris, décoche un article pris pour un tiers
- **THEN** l'article est libéré sans avertissement et son nom de tiers comme son auteur sont effacés

#### Scenario: Tiers portant le login du lecteur
- **WHEN** un utilisateur connecté décoche un article pris pour un tiers dont le nom est identique à son login
- **THEN** l'article est libéré sans avertissement

#### Scenario: Purge des avis de modification
- **WHEN** un article est libéré
- **THEN** les avis de modification en attente concernant cet article sont supprimés
