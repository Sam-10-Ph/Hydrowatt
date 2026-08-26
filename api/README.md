# HydroWatt — API Django REST

## Prérequis

- Python 3.11+
- PostgreSQL 14+ (ou SQLite pour un test rapide sans installation, voir plus bas)

## Installation

```bash
cd api
python3 -m venv venv
source venv/bin/activate        # Windows : venv\Scripts\activate
pip install -r requirements.txt
```

## Configuration de la base de données

Par défaut, l'API utilise **PostgreSQL** avec les variables d'environnement
suivantes (valeurs par défaut entre parenthèses) :

| Variable       | Défaut      |
|----------------|-------------|
| `DB_NAME`      | `hydrowatt` |
| `DB_USER`      | `hydrowatt` |
| `DB_PASSWORD`  | `hydrowatt` |
| `DB_HOST`      | `localhost` |
| `DB_PORT`      | `5432`      |

Créer la base et l'utilisateur PostgreSQL (exemple) :

```bash
sudo -u postgres psql -c "CREATE USER hydrowatt WITH PASSWORD 'hydrowatt';"
sudo -u postgres psql -c "CREATE DATABASE hydrowatt OWNER hydrowatt;"
```

**Test rapide sans PostgreSQL** (SQLite) : `export USE_POSTGRES=false`
avant les commandes ci-dessous.

## Migrations et compte de démonstration

```bash
python manage.py makemigrations
python manage.py migrate
python manage.py createsuperuser   # compte admin de démonstration exigé par les livrables
```

## Lancement

```bash
python manage.py runserver 0.0.0.0:8000
```

L'API est alors disponible sur `http://127.0.0.1:8000/api/`.
L'admin Django (pratique pour peupler des données de démo) est sur
`http://127.0.0.1:8000/admin/`.

## Endpoints principaux

### Authentification

| Méthode | URL                             | Description                                   |
|---------|----------------------------------|-----------------------------------------------|
| POST    | `/api/auth/login/`              | Étape 1 : identifiant + mot de passe          |
| POST    | `/api/auth/2fa/verify/`         | Étape 2 : `pre_auth_token` + code TOTP        |
| POST    | `/api/auth/forgot-password/`    | Envoie un token de reset par email (console)  |
| POST    | `/api/auth/reset-password/`     | `token` + `new_password`                      |
| POST    | `/api/auth/refresh/`            | Rafraîchit l'access token JWT                 |
| GET     | `/api/auth/me/`                 | Profil de l'utilisateur connecté              |
| CRUD    | `/api/utilisateurs/`            | Réservé aux administrateurs (`is_staff`)      |

**Note 2FA** : à la création du compte, `is_2fa_enabled=True` par
défaut. Le secret TOTP est généré au premier login et peut être
retrouvé pour configurer une app comme Google Authenticator via le
shell Django :

```python
python manage.py shell
>>> from comptes.models import Utilisateur
>>> u = Utilisateur.objects.get(username="admin")
>>> u.get_or_create_otp_secret()   # affiche/produit le secret Base32
```

### Métier (énergie)

| Méthode    | URL                                          | Description                          |
|------------|-----------------------------------------------|---------------------------------------|
| CRUD       | `/api/barrages/`                             | Barrages                              |
| GET        | `/api/barrages/{id}/correlation/`            | Corrélation niveau d'eau / production |
| CRUD       | `/api/releves-niveau/`                       | Relevés de niveau d'eau               |
| CRUD       | `/api/productions/`                          | Production journalière                |

Filtres disponibles (query params) :
- `/api/releves-niveau/?barrage=1&date_debut=2026-01-01&date_fin=2026-01-31&niveau_min=5`
- `/api/productions/?barrage=1&date_debut=2026-01-01&production_min=100`
- `/api/barrages/?search=Kinguélé`

Toutes les routes (hors `/api/auth/login/`, `/2fa/verify/`,
`/forgot-password/`, `/reset-password/`) exigent un en-tête
`Authorization: Bearer <access_token>`.

## Tester avec Postman/Swagger

Les endpoints DRF sont navigables directement dans un navigateur
(interface « Browsable API ») une fois le serveur lancé, ou via
Postman en suivant le tableau ci-dessus.
