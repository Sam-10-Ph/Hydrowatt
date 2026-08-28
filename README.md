# HydroWatt — Script de soutenance (slide par slide)

Durée cible : 10 à 12 minutes de parole, pour un créneau de 10-15 min avec questions.
Chaque section indique un minutage indicatif — à ajuster selon ton débit.

---

## Slide 1 — Titre (30 sec)

> "Bonjour, je m'appelle Samuel, étudiant en ING2/MIAGE2 à l'IAI Gabon. Je vais vous présenter HydroWatt, une plateforme de suivi de l'énergie hydroélectrique gabonaise, réalisée dans le cadre du TP individuel n°13, encadré par M. ABOH ABOH Yannick Thierry."

Ne t'attarde pas — c'est une slide de transition, le jury la lit pendant que tu parles.

---

## Slide 2 — Contexte & objectifs (45 sec)

> "Le Gabon exploite plusieurs barrages hydroélectriques, notamment Kinguélé et Grand Poubara, dont la production dépend directement du niveau d'eau retenu. L'objectif du projet est de concevoir une application en trois niveaux permettant de centraliser les barrages, leurs relevés de niveau d'eau et leur production journalière, avec une authentification sécurisée. Le défi technique demandé était de mettre en évidence la corrélation entre le niveau d'eau et la production — j'y reviendrai."

Insiste sur le mot "corrélation" — c'est le fil rouge que le jury doit retenir dès le début.

---

## Slide 3 — La problématique (1 min)

> "Avant de rentrer dans la technique, je veux poser le vrai problème que ce projet adresse. Le Gabon connaît des délestages électriques fréquents, qui touchent aussi bien les ménages que les entreprises, parfois plusieurs jours d'affilée. Ce que beaucoup de gens ignorent, c'est que l'eau potable dépend elle aussi de l'électricité : les stations de pompage et de traitement d'eau ne peuvent plus fonctionner en cas de coupure. Une panne électrique devient alors une panne d'eau."
>
> "HydroWatt ne résout pas les coupures à lui seul, mais il donne aux opérateurs une base de données fiable pour suivre la production en temps réel et anticiper les baisses — plutôt que de les subir."

**Si le jury demande "en quoi c'est concret ?"** : *"Sans outil de suivi centralisé, on découvre une baisse de production après coup. Avec HydroWatt, la corrélation niveau d'eau / production est visible immédiatement."*

---

## Slide 4 — Architecture technique (1 min)

> "L'application suit une architecture en trois niveaux. Le client JavaFX, que l'utilisateur manipule, ne parle jamais directement à la base de données. Il passe systématiquement par l'API Django REST Framework, qui gère l'authentification, les opérations CRUD, les filtres et le calcul de corrélation. C'est cette API, et elle seule, qui accède à PostgreSQL."
>
> "Trois piliers de sécurité soutiennent cette architecture : les mots de passe sont hachés, jamais stockés en clair ; l'authentification utilise des jetons JWT à durée de vie limitée ; et une double authentification protège la connexion."

**Si on demande "pourquoi cette séparation ?"** : *"Ça isole les responsabilités : le client ne connaît que l'API, jamais les détails de la base. Si demain je change de base de données, le client n'a rien à changer."*

---

## Slide 5 — Modèle de données (1 min 30 à 2 min)

> "Cette slide montre comment les données sont organisées en base. J'ai volontairement séparé deux modules indépendants : à gauche tout ce qui concerne les comptes utilisateurs, à droite tout ce qui concerne le métier — les barrages et leurs mesures. Ces deux mondes ne se touchent jamais directement en base."
>
> "Chaque rectangle est une table. La barre de titre colorée donne son nom ; chaque ligne avec un point est une colonne, donc une information stockée ; le texte en italique tout en bas, ce sont des actions que le code peut faire avec cette donnée, pas des colonnes."
>
> "À gauche, `Utilisateur` hérite d'`AbstractUser`, un modèle fourni par Django — en pointillés parce qu'il n'existe pas vraiment en base, c'est un socle tout fait qui gère déjà le mot de passe haché et les droits admin. J'ai juste ajouté l'email, le secret de double authentification et son statut. La flèche '1 — 0..N' en dessous veut dire qu'un utilisateur peut avoir zéro, un, ou plusieurs tokens de réinitialisation de mot de passe."
>
> "À droite, tout part de `Barrage` : nom, localisation, capacité en mégawatts. De ce barrage partent deux relations identiques, un-à-plusieurs : ses relevés de niveau d'eau, et ses productions journalières — un enregistrement par jour. La contrainte que vous voyez en italique empêche d'avoir deux relevés pour le même barrage le même jour."
>
> "C'est ce modèle qui alimente tout le reste : l'API expose ces tables en CRUD, et c'est la relation Barrage vers Relevés et Productions qui permet de construire le graphique de corrélation que je vais montrer plus loin."

**Questions probables :**
- *"Pourquoi 2 modules séparés ?"* → "Authentification et métier sont deux responsabilités différentes ; les séparer rend le code plus facile à maintenir et à tester."
- *"Pourquoi hériter d'AbstractUser ?"* → "Django gère déjà le hachage du mot de passe et les permissions de façon sécurisée — pas besoin de réinventer ça."
- *"Pourquoi pas de relation entre comptes et energie ?"* → "Rien dans le cahier des charges ne le demande : un utilisateur gère des barrages, il n'appartient pas à un barrage."

---

## Slide 6 — Authentification en 2 étapes (1 min)

> "La connexion se fait en quatre étapes. D'abord l'identifiant et le mot de passe, vérifiés contre le hash stocké en base. Si la double authentification est activée, l'API renvoie un jeton de pré-authentification valable seulement 5 minutes, plutôt que de connecter directement l'utilisateur. L'utilisateur saisit alors un code à 6 chiffres, comme avec Google Authenticator. Une fois validé, l'API délivre les jetons JWT définitifs : un access token valable 2 heures, et un refresh token valable 7 jours qui permet de se reconnecter sans ressaisir le mot de passe."
>
> "Pour la réinitialisation de mot de passe, un token à usage unique est envoyé par email et expire automatiquement après 30 minutes."

**Si on demande "pourquoi un jeton intermédiaire et pas connecter direct après le mot de passe ?"** : *"Parce que tant que le code TOTP n'est pas validé, l'utilisateur ne doit avoir aucun accès — le jeton de pré-authentification ne sert qu'à relier les deux étapes, il ne donne aucun droit sur l'API."*

---

## Slide 7 — Fonctionnalités principales (45 sec)

> "Les quatre modules du client couvrent l'ensemble du cahier des charges : gestion des barrages avec recherche par nom ou localisation ; relevés de niveau d'eau avec filtres par barrage, période et niveau ; production journalière avec les mêmes types de filtres ; et un module d'administration des comptes utilisateurs, réservé aux administrateurs — il n'y a aucune inscription publique sur cette plateforme."

Rapide — cette slide résume, elle n'a pas besoin de détails supplémentaires à l'oral.

---

## Slide 8 — Défi technique : corrélation (1 min)

> "C'est le défi technique demandé par le sujet. L'onglet Corrélation du client superpose, jour par jour, le niveau d'eau et l'énergie produite d'un barrage sélectionné. Sur ce graphique d'exemple, on voit clairement que la production suit la même tendance que le niveau d'eau : quand le niveau monte, la production monte aussi, avec un léger décalage."
>
> "Techniquement, un endpoint dédié sur l'API fusionne les deux séries de données par date, et le client les affiche avec un LineChart JavaFX à deux courbes."

**Si on demande "et si les dates ne correspondent pas exactement entre les deux tables ?"** : *"L'API aligne les deux séries sur l'ensemble des dates disponibles ; s'il manque une valeur d'un côté, elle apparaît simplement comme absente sur le graphique plutôt que de fausser la corrélation."*

---

## Slide 9 — Démonstration (1 min 30, variable selon live demo)

> "Voici le tableau de bord du client JavaFX en fonctionnement. On y retrouve les cinq onglets dont je viens de parler. Le parcours complet démontré est : connexion, double authentification, création ou modification d'un barrage, filtrage des relevés, puis affichage du graphique de corrélation."

Si tu fais une démo live plutôt qu'une capture statique, garde ce texte comme fil conducteur et adapte le timing — une démo live peut prendre 2-3 min, prévois de compresser une autre slide en conséquence (par exemple la slide 7).

---

## Slide 10 — Stack technique (30 sec)

> "Côté client : JavaFX avec Maven, HttpClient pour les appels réseau et Jackson pour le JSON. Côté API : Django et Django REST Framework, avec simplejwt pour les jetons, django-filter pour les filtres combinables, et pyotp pour la double authentification. Côté données et outils : PostgreSQL, pgAdmin pour l'administration, Git et GitHub pour le versionnement, le tout développé sur un ordinateur sous Windows 11."

Slide de référence — la lire posément une fois suffit, pas besoin de commenter chaque techno.

---

## Slide 11 — Difficultés rencontrées (45 sec)

> "Trois difficultés ont marqué le développement. D'abord, un mot de passe PostgreSQL oublié, résolu en réinitialisant temporairement l'authentification locale via le fichier pg_hba.conf, avant de revenir à une configuration sécurisée. Ensuite, des dépendances Python absentes parce que je travaillais hors de l'environnement virtuel — j'ai pris l'habitude de toujours créer et activer un venv dédié. Enfin, un conflit Git lors de la synchronisation avec un dépôt distant non vide, résolu par une fusion manuelle."

Cette slide humanise ta soutenance — n'hésite pas à la dire avec un peu d'autodérision, ça passe bien auprès d'un jury.

---

## Slide 12 — Conclusion & perspectives (45 sec)

> "En résumé, l'architecture en trois niveaux est fonctionnelle et testée de bout en bout, l'authentification est robuste avec hachage, double authentification et JWT, le CRUD et les filtres sont opérationnels sur les trois entités métier, et le défi de corrélation demandé par le sujet est relevé."
>
> "Comme perspectives, on pourrait ajouter des alertes automatiques sur des seuils de niveau critiques, l'export des rapports de production, l'intégration d'autres sources d'énergie comme le solaire de Plaine Ayémé ou les centrales thermiques, et un déploiement conteneurisé avec Docker."
>
> "Merci de votre attention, je suis à votre disposition pour vos questions."

---

## Récapitulatif des timings

| Slide | Titre | Temps indicatif |
|---|---|---|
| 1 | Titre | 30 sec |
| 2 | Contexte & objectifs | 45 sec |
| 3 | La problématique | 1 min |
| 4 | Architecture technique | 1 min |
| 5 | Modèle de données | 1 min 30 – 2 min |
| 6 | Authentification | 1 min |
| 7 | Fonctionnalités principales | 45 sec |
| 8 | Défi technique (corrélation) | 1 min |
| 9 | Démonstration | 1 min 30 |
| 10 | Stack technique | 30 sec |
| 11 | Difficultés rencontrées | 45 sec |
| 12 | Conclusion & perspectives | 45 sec |
| **Total** | | **~11-12 min** |

Ça laisse 3 à 4 minutes de marge pour les questions sur un créneau de 15 minutes.

## Conseils généraux pour l'oral

- Ne lis jamais une slide mot pour mot au jury — regarde-le, pas l'écran.
- Sur les slides denses (5, 6, 8), ralentis et pointe du doigt ce dont tu parles.
- Sur les slides de résumé (7, 10), accélère — le jury lit en même temps que toi.
- Prépare une réponse courte si on te demande de relancer le serveur ou de montrer du code en live : sache où se trouvent `manage.py runserver` et le projet Maven du client.
