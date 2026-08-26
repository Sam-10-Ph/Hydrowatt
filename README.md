# HydroWatt — Sujet n°13 (ING2/MIAGE2, IAI Gabon)

Application de suivi de l'énergie hydroélectrique gabonaise (barrages,
relevés de niveau d'eau, production journalière), développée selon le
cahier des charges commun du TP : architecture 3 niveaux **client
JavaFX / API Django REST Framework / PostgreSQL**.

## Structure du dépôt

```
hydrowatt/
├── api/      → API Django REST (authentification, CRUD, filtres)
└── client/   → Client JavaFX (Maven)
```

Chaque sous-dossier possède son propre README avec les instructions
d'installation et de lancement détaillées.

## Démarrage rapide

1. Lancer l'API (voir `api/README.md`) — écoute par défaut sur
   `http://127.0.0.1:8000`
2. Lancer le client JavaFX (voir `client/README.md`) — se connecte à
   l'API via `ApiClient.BASE_URL`
3. Se connecter avec le compte administrateur de démonstration créé à
   l'étape d'installation de l'API (voir README de l'API)

## Fonctionnalités couvertes

- Authentification complète : login (mot de passe haché), mot de passe
  oublié (token à usage unique par email, expiration 30 min), reset,
  double authentification (TOTP)
- Aucune inscription publique : les comptes sont gérés uniquement par
  un administrateur via le module Utilisateurs (CRUD)
- CRUD complet des entités métier : Barrage, Relevé de niveau d'eau,
  Production journalière
- Recherche et filtres combinables (par barrage, période, niveau
  d'eau, production minimale)
- Défi technique du sujet : graphique de corrélation niveau d'eau /
  production par barrage (onglet « Corrélation » du client)

## Notes de conception (pour la soutenance)

- Le client JavaFX ne communique **jamais** directement avec
  PostgreSQL : tous les échanges passent par l'API REST en JSON
  (contrainte imposée par le cahier des charges)
- Les mots de passe ne sont jamais stockés ni transmis en clair côté
  serveur (hachage Django par défaut, PBKDF2)
- Les jetons JWT (access 2h / refresh 7j) sont conservés en mémoire
  côté client (`Session`), jamais persistés sur disque
- Le token de réinitialisation de mot de passe est à usage unique et
  expire après 30 minutes
