# HydroWatt — Client JavaFX

## Prérequis

- JDK 17+
- Maven 3.8+
- L'API Django doit être lancée (voir `../api/README.md`)

## Configuration

L'URL de l'API est définie dans
`src/main/java/ga/iai/hydrowatt/service/ApiClient.java` :

```java
public static String BASE_URL = "http://127.0.0.1:8000/api";
```

Adapter cette valeur si l'API tourne sur une autre machine/port
(par exemple l'IP de la VM Debian si le client tourne sous Windows).

## Lancement (mode développement)

```bash
cd client
mvn clean javafx:run
```

## Génération d'un exécutable

```bash
mvn clean package
```

Le jar est généré dans `target/`. Pour l'exécuter avec les modules
JavaFX (non inclus dans le JDK depuis Java 11) :

```bash
java --module-path /chemin/vers/javafx-sdk-21/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar target/hydrowatt-client-1.0.0.jar
```

## Parcours utilisateur

1. **Connexion** : identifiant + mot de passe (créés uniquement par un
   administrateur, aucune inscription publique)
2. **Double authentification** : code TOTP à 6 chiffres si la 2FA est
   activée sur le compte
3. **Tableau de bord** avec 5 onglets :
   - **Barrages** : CRUD + recherche (nom, localisation)
   - **Relevés de niveau d'eau** : CRUD + filtres (barrage, période,
     niveau min)
   - **Production journalière** : CRUD + filtres (barrage, période,
     production min)
   - **Corrélation niveau / production** : graphique mettant en
     évidence la corrélation niveau d'eau ↔ production pour un
     barrage donné (défi technique du sujet)
   - **Utilisateurs (admin)** : CRUD des comptes, visible uniquement
     si l'utilisateur connecté est administrateur

## Architecture du code

```
src/main/java/ga/iai/hydrowatt/
├── HydroWattApp.java        → point d'entrée JavaFX
├── model/                   → DTOs (Barrage, ReleveNiveauEau, ProductionJournaliere, Utilisateur)
├── service/
│   ├── ApiClient.java       → HttpClient + Jackson, seul point d'accès réseau
│   ├── Session.java         → jetons JWT en mémoire (jamais persistés)
│   └── ApiException.java
├── controller/               → contrôleurs FXML (Login, 2FA, Forgot/Reset, Dashboard)
└── util/SceneManager.java   → navigation entre écrans
```

Le client ne se connecte **jamais** directement à PostgreSQL : toutes
les données transitent par l'API Django REST Framework en JSON.
