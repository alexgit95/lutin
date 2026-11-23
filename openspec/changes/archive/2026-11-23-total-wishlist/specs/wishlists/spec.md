## ADDED Requirements

### Requirement: Total des prix de la wishlist
Le systeme SHALL afficher en haut de la wishlist, pour le proprietaire, le lecteur partage et le visiteur public, la somme en euros des prix renseignes de tous ses articles, toutes pages confondues. Le total SHALL inclure les articles pris et libres et SHALL conserver la precision au centime. Le systeme SHALL indiquer que le total porte sur les prix renseignes et afficher le nombre d'articles sans prix lorsqu'il est non nul. Un prix renseigne a zero SHALL NOT etre considere manquant. Une wishlist vide ou sans prix renseigne SHALL afficher un total de zero.

#### Scenario: Total identique sur chaque vue et chaque page
- **WHEN** une wishlist contient des articles sur plusieurs pages, dont certains sont pris
- **THEN** les vues proprietaire, lecteur partage et publique affichent sur chaque page le total de tous les prix renseignes, y compris ceux des articles pris

#### Scenario: Prix manquants et prix nul
- **WHEN** une wishlist contient des articles a 12,35 euros, 0 euro et sans prix
- **THEN** le total des prix renseignes est de 12,35 euros et un seul article est indique sans prix

#### Scenario: Wishlist vide ou sans prix
- **WHEN** une wishlist est vide ou ne contient que des articles sans prix
- **THEN** le total est de 0 euro et, pour les articles sans prix, leur nombre est indique

#### Scenario: Modification des articles
- **WHEN** le proprietaire ajoute, modifie ou supprime un article puis consulte la wishlist
- **THEN** le total et le nombre de prix manquants refletent les donnees actuelles de toute la wishlist

#### Scenario: Reservation invisible au proprietaire
- **WHEN** un lecteur prend ou libere un article sans autre modification
- **THEN** le total et le nombre de prix manquants restent inchanges et aucune information de reservation n'est exposee au proprietaire

#### Scenario: Acces non autorise
- **WHEN** un utilisateur sans acces ou un visiteur avec un jeton public invalide demande une wishlist
- **THEN** aucun total ni nombre de prix manquants de cette wishlist ne lui est communique