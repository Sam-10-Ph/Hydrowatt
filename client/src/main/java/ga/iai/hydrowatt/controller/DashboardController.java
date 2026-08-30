package ga.iai.hydrowatt.controller;

import com.fasterxml.jackson.databind.JsonNode;
import ga.iai.hydrowatt.model.Barrage;
import ga.iai.hydrowatt.model.ProductionJournaliere;
import ga.iai.hydrowatt.model.ReleveNiveauEau;
import ga.iai.hydrowatt.model.Utilisateur;
import ga.iai.hydrowatt.service.ApiClient;
import ga.iai.hydrowatt.service.ApiException;
import ga.iai.hydrowatt.service.JsonMapper;
import ga.iai.hydrowatt.service.Session;
import ga.iai.hydrowatt.util.SceneManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class DashboardController {

    @FXML private Label userLabel;
    @FXML private Tab usersTab;
    @FXML private TabPane tabPane;

    // Barrages
    @FXML private TextField barrageSearchField;
    @FXML private TableView<Barrage> barrageTable;
    @FXML private TableColumn<Barrage, String> colBarrageNom;
    @FXML private TableColumn<Barrage, String> colBarrageLoc;
    @FXML private TableColumn<Barrage, String> colBarrageCapacite;

    // Relevés
    @FXML private ComboBox<Barrage> releveBarrageFilter;
    @FXML private DatePicker releveDateDebut;
    @FXML private DatePicker releveDateFin;
    @FXML private TextField releveNiveauMinField;
    @FXML private TableView<ReleveNiveauEau> releveTable;
    @FXML private TableColumn<ReleveNiveauEau, String> colReleveBarrage;
    @FXML private TableColumn<ReleveNiveauEau, String> colReleveDate;
    @FXML private TableColumn<ReleveNiveauEau, String> colReleveNiveau;

    // Production
    @FXML private ComboBox<Barrage> productionBarrageFilter;
    @FXML private DatePicker productionDateDebut;
    @FXML private DatePicker productionDateFin;
    @FXML private TextField productionMinField;
    @FXML private TableView<ProductionJournaliere> productionTable;
    @FXML private TableColumn<ProductionJournaliere, String> colProductionBarrage;
    @FXML private TableColumn<ProductionJournaliere, String> colProductionDate;
    @FXML private TableColumn<ProductionJournaliere, String> colProductionEnergie;

    // Corrélation
    @FXML private ComboBox<Barrage> correlationBarrageChoice;
    @FXML private LineChart<Number, Number> correlationChart;
    @FXML private NumberAxis correlationXAxis;
    @FXML private NumberAxis correlationYAxis;

    // Utilisateurs
    @FXML private TableView<Utilisateur> userTable;
    @FXML private TableColumn<Utilisateur, String> colUserUsername;
    @FXML private TableColumn<Utilisateur, String> colUserEmail;
    @FXML private TableColumn<Utilisateur, String> colUserStaff;
    @FXML private TableColumn<Utilisateur, String> colUserActive;

    private final ObservableList<Barrage> barrages = FXCollections.observableArrayList();
    private final ObservableList<ReleveNiveauEau> releves = FXCollections.observableArrayList();
    private final ObservableList<ProductionJournaliere> productions = FXCollections.observableArrayList();
    private final ObservableList<Utilisateur> utilisateurs = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        userLabel.setText("Connecté : " + Session.getUsername());

        colBarrageNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colBarrageLoc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLocalisation()));
        colBarrageCapacite.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getCapaciteInstalleeMw())));
        barrageTable.setItems(barrages);

        colReleveBarrage.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBarrageNom()));
        colReleveDate.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getDate())));
        colReleveNiveau.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getNiveauM())));
        releveTable.setItems(releves);

        colProductionBarrage.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBarrageNom()));
        colProductionDate.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getDate())));
        colProductionEnergie.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getEnergieProduiteMwh())));
        productionTable.setItems(productions);

        colUserUsername.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colUserEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colUserStaff.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isStaff() ? "Oui" : "Non"));
        colUserActive.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "Oui" : "Non"));
        userTable.setItems(utilisateurs);

        // Le module Utilisateurs (CRUD comptes) est réservé à l'administrateur.
        usersTab.setDisable(!Session.isAdmin());

        loadBarrages(null);
        loadReleves();
        loadProductions();
        if (Session.isAdmin()) loadUsers();
    }

    private void runAsync(Runnable body, Runnable onDone) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                body.run();
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(onDone));
        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            // Les tâches enveloppent souvent la vraie cause dans un RuntimeException
            // (voir les blocs "throw new RuntimeException(e)" plus bas) : on la déballe
            // pour retrouver le message exact renvoyé par l'API.
            Throwable cause = ex;
            while (cause != null && !(cause instanceof ApiException) && cause.getCause() != null) {
                cause = cause.getCause();
            }
            String msg = cause instanceof ApiException apiEx
                    ? "Erreur API (" + apiEx.getStatusCode() + ") : " + apiEx.getMessage()
                    : "Erreur réseau : " + (cause != null ? cause.toString() : String.valueOf(ex));
            showFullErrorAlert(msg);
        }));
        new Thread(task).start();
    }

    /** Affiche une erreur dans une boîte redimensionnable qui n'est jamais tronquée. */
    private void showFullErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        TextArea area = new TextArea(message);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefWidth(480);
        area.setPrefHeight(160);
        alert.getDialogPane().setContent(area);
        alert.setResizable(true);
        alert.showAndWait();
    }

    // -----------------------------------------------------------
    // Barrages
    // -----------------------------------------------------------

    private void loadBarrages(String search) {
        runAsync(() -> {
            try {
                String path = "/barrages/?page_size=200" + (search != null && !search.isBlank() ? "&search=" + search : "");
                JsonNode node = ApiClient.get(path);
                List<Barrage> list = JsonMapper.get().convertValue(
                        node.has("results") ? node.get("results") : node,
                        JsonMapper.get().getTypeFactory().constructCollectionType(List.class, Barrage.class));
                Platform.runLater(() -> {
                    barrages.setAll(list);
                    ObservableList<Barrage> copy = FXCollections.observableArrayList(list);
                    releveBarrageFilter.setItems(copy);
                    productionBarrageFilter.setItems(FXCollections.observableArrayList(list));
                    correlationBarrageChoice.setItems(FXCollections.observableArrayList(list));
                });
            } catch (Exception e) { throw new RuntimeException(e); }
        }, () -> {});
    }

    @FXML private void handleSearchBarrages() { loadBarrages(barrageSearchField.getText()); }

    @FXML
    private void handleNewBarrage() {
        showBarrageDialog(new Barrage());
    }

    @FXML
    private void handleEditBarrage() {
        Barrage selected = barrageTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        showBarrageDialog(selected);
    }

    @FXML
    private void handleDeleteBarrage() {
        Barrage selected = barrageTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!confirm("Supprimer le barrage " + selected.getNom() + " ainsi que ses relevés/productions liés ?")) return;
        runAsync(() -> {
            try { ApiClient.delete("/barrages/" + selected.getId() + "/"); }
            catch (Exception e) { throw new RuntimeException(e); }
        }, () -> loadBarrages(null));
    }

    private void showBarrageDialog(Barrage barrage) {
        boolean isNew = barrage.getId() == null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Nouveau barrage" : "Modifier le barrage");
        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField nomField = new TextField(barrage.getNom());
        TextField locField = new TextField(barrage.getLocalisation());
        TextField capaciteField = new TextField(barrage.getCapaciteInstalleeMw() == null ? "" : barrage.getCapaciteInstalleeMw().toString());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.addRow(0, new Label("Nom"), nomField);
        grid.addRow(1, new Label("Localisation"), locField);
        grid.addRow(2, new Label("Capacité installée (MW)"), capaciteField);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveType) {
            try {
                barrage.setNom(nomField.getText().trim());
                barrage.setLocalisation(locField.getText().trim());
                barrage.setCapaciteInstalleeMw(new BigDecimal(capaciteField.getText().trim()));
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.ERROR, "Capacité invalide.").showAndWait();
                return;
            }
            runAsync(() -> {
                try {
                    if (isNew) ApiClient.post("/barrages/", barrage);
                    else ApiClient.patch("/barrages/" + barrage.getId() + "/", barrage);
                } catch (Exception e) { throw new RuntimeException(e); }
            }, () -> loadBarrages(null));
        }
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        return alert.showAndWait().filter(bt -> bt == ButtonType.YES).isPresent();
    }

    // -----------------------------------------------------------
    // Relevés de niveau d'eau
    // -----------------------------------------------------------

    private void loadReleves() {
        loadReleves("/releves-niveau/?page_size=200");
    }

    private void loadReleves(String path) {
        runAsync(() -> {
            try {
                JsonNode node = ApiClient.get(path);
                List<ReleveNiveauEau> list = JsonMapper.get().convertValue(
                        node.has("results") ? node.get("results") : node,
                        JsonMapper.get().getTypeFactory().constructCollectionType(List.class, ReleveNiveauEau.class));
                Platform.runLater(() -> releves.setAll(list));
            } catch (Exception e) { throw new RuntimeException(e); }
        }, () -> {});
    }

    @FXML
    private void handleFilterReleves() {
        StringBuilder sb = new StringBuilder("/releves-niveau/?page_size=200");
        Barrage b = releveBarrageFilter.getValue();
        if (b != null) sb.append("&barrage=").append(b.getId());
        if (releveDateDebut.getValue() != null) sb.append("&date_debut=").append(releveDateDebut.getValue());
        if (releveDateFin.getValue() != null) sb.append("&date_fin=").append(releveDateFin.getValue());
        if (!releveNiveauMinField.getText().isBlank()) sb.append("&niveau_min=").append(releveNiveauMinField.getText().trim());
        loadReleves(sb.toString());
    }

    @FXML
    private void handleNewReleve() { showReleveDialog(new ReleveNiveauEau()); }

    @FXML
    private void handleEditReleve() {
        ReleveNiveauEau selected = releveTable.getSelectionModel().getSelectedItem();
        if (selected != null) showReleveDialog(selected);
    }

    @FXML
    private void handleDeleteReleve() {
        ReleveNiveauEau selected = releveTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!confirm("Supprimer ce relevé ?")) return;
        runAsync(() -> {
            try { ApiClient.delete("/releves-niveau/" + selected.getId() + "/"); }
            catch (Exception e) { throw new RuntimeException(e); }
        }, this::loadReleves);
    }

    private void showReleveDialog(ReleveNiveauEau releve) {
        boolean isNew = releve.getId() == null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Nouveau relevé" : "Modifier le relevé");
        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        ComboBox<Barrage> barrageBox = new ComboBox<>(barrages);
        if (releve.getBarrage() != null) {
            barrages.stream().filter(b -> b.getId().equals(releve.getBarrage())).findFirst().ifPresent(barrageBox::setValue);
        }
        DatePicker datePicker = new DatePicker(releve.getDate() == null ? LocalDate.now() : releve.getDate());
        TextField niveauField = new TextField(releve.getNiveauM() == null ? "" : releve.getNiveauM().toString());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.addRow(0, new Label("Barrage"), barrageBox);
        grid.addRow(1, new Label("Date"), datePicker);
        grid.addRow(2, new Label("Niveau (m)"), niveauField);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveType) {
            if (barrageBox.getValue() == null || datePicker.getValue() == null) {
                new Alert(Alert.AlertType.ERROR, "Barrage et date sont obligatoires.").showAndWait();
                return;
            }
            try {
                releve.setBarrage(barrageBox.getValue().getId());
                releve.setDate(datePicker.getValue());
                releve.setNiveauM(new BigDecimal(niveauField.getText().trim()));
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.ERROR, "Niveau invalide.").showAndWait();
                return;
            }
            runAsync(() -> {
                try {
                    if (isNew) ApiClient.post("/releves-niveau/", releve);
                    else ApiClient.patch("/releves-niveau/" + releve.getId() + "/", releve);
                } catch (Exception e) { throw new RuntimeException(e); }
            }, this::loadReleves);
        }
    }

    // -----------------------------------------------------------
    // Production journalière
    // -----------------------------------------------------------

    private void loadProductions() { loadProductions("/productions/?page_size=200"); }

    private void loadProductions(String path) {
        runAsync(() -> {
            try {
                JsonNode node = ApiClient.get(path);
                List<ProductionJournaliere> list = JsonMapper.get().convertValue(
                        node.has("results") ? node.get("results") : node,
                        JsonMapper.get().getTypeFactory().constructCollectionType(List.class, ProductionJournaliere.class));
                Platform.runLater(() -> productions.setAll(list));
            } catch (Exception e) { throw new RuntimeException(e); }
        }, () -> {});
    }

    @FXML
    private void handleFilterProductions() {
        StringBuilder sb = new StringBuilder("/productions/?page_size=200");
        Barrage b = productionBarrageFilter.getValue();
        if (b != null) sb.append("&barrage=").append(b.getId());
        if (productionDateDebut.getValue() != null) sb.append("&date_debut=").append(productionDateDebut.getValue());
        if (productionDateFin.getValue() != null) sb.append("&date_fin=").append(productionDateFin.getValue());
        if (!productionMinField.getText().isBlank()) sb.append("&production_min=").append(productionMinField.getText().trim());
        loadProductions(sb.toString());
    }

    @FXML
    private void handleNewProduction() { showProductionDialog(new ProductionJournaliere()); }

    @FXML
    private void handleEditProduction() {
        ProductionJournaliere selected = productionTable.getSelectionModel().getSelectedItem();
        if (selected != null) showProductionDialog(selected);
    }

    @FXML
    private void handleDeleteProduction() {
        ProductionJournaliere selected = productionTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!confirm("Supprimer cette production ?")) return;
        runAsync(() -> {
            try { ApiClient.delete("/productions/" + selected.getId() + "/"); }
            catch (Exception e) { throw new RuntimeException(e); }
        }, this::loadProductions);
    }

    private void showProductionDialog(ProductionJournaliere production) {
        boolean isNew = production.getId() == null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Nouvelle production" : "Modifier la production");
        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        ComboBox<Barrage> barrageBox = new ComboBox<>(barrages);
        if (production.getBarrage() != null) {
            barrages.stream().filter(b -> b.getId().equals(production.getBarrage())).findFirst().ifPresent(barrageBox::setValue);
        }
        DatePicker datePicker = new DatePicker(production.getDate() == null ? LocalDate.now() : production.getDate());
        TextField energieField = new TextField(production.getEnergieProduiteMwh() == null ? "" : production.getEnergieProduiteMwh().toString());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.addRow(0, new Label("Barrage"), barrageBox);
        grid.addRow(1, new Label("Date"), datePicker);
        grid.addRow(2, new Label("Énergie produite (MWh)"), energieField);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveType) {
            if (barrageBox.getValue() == null || datePicker.getValue() == null) {
                new Alert(Alert.AlertType.ERROR, "Barrage et date sont obligatoires.").showAndWait();
                return;
            }
            try {
                production.setBarrage(barrageBox.getValue().getId());
                production.setDate(datePicker.getValue());
                production.setEnergieProduiteMwh(new BigDecimal(energieField.getText().trim()));
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.ERROR, "Valeur d'énergie invalide.").showAndWait();
                return;
            }
            runAsync(() -> {
                try {
                    if (isNew) ApiClient.post("/productions/", production);
                    else ApiClient.patch("/productions/" + production.getId() + "/", production);
                } catch (Exception e) { throw new RuntimeException(e); }
            }, this::loadProductions);
        }
    }

    // -----------------------------------------------------------
    // Corrélation niveau d'eau / production (défi technique du sujet)
    // -----------------------------------------------------------

    @FXML
    private void handleShowCorrelation() {
        Barrage b = correlationBarrageChoice.getValue();
        if (b == null) return;
        runAsync(() -> {
            try {
                JsonNode node = ApiClient.get("/barrages/" + b.getId() + "/correlation/");
                Platform.runLater(() -> renderCorrelationChart(node, b.getNom()));
            } catch (Exception e) { throw new RuntimeException(e); }
        }, () -> {});
    }

    private void renderCorrelationChart(JsonNode points, String barrageNom) {
        correlationChart.getData().clear();
        XYChart.Series<Number, Number> niveauSeries = new XYChart.Series<>();
        niveauSeries.setName("Niveau d'eau (m)");
        XYChart.Series<Number, Number> productionSeries = new XYChart.Series<>();
        productionSeries.setName("Production (MWh)");

        int index = 0;
        for (JsonNode p : points) {
            if (!p.get("niveau_m").isNull()) niveauSeries.getData().add(new XYChart.Data<>(index, p.get("niveau_m").asDouble()));
            if (!p.get("energie_produite_mwh").isNull()) productionSeries.getData().add(new XYChart.Data<>(index, p.get("energie_produite_mwh").asDouble()));
            index++;
        }
        correlationChart.setTitle("Corrélation niveau d'eau / production — " + barrageNom);
        correlationChart.getData().addAll(niveauSeries, productionSeries);
    }

    // -----------------------------------------------------------
    // Utilisateurs (admin uniquement)
    // -----------------------------------------------------------

    private void loadUsers() {
        runAsync(() -> {
            try {
                JsonNode node = ApiClient.get("/utilisateurs/?page_size=200");
                List<Utilisateur> list = JsonMapper.get().convertValue(
                        node.has("results") ? node.get("results") : node,
                        JsonMapper.get().getTypeFactory().constructCollectionType(List.class, Utilisateur.class));
                Platform.runLater(() -> utilisateurs.setAll(list));
            } catch (Exception e) { throw new RuntimeException(e); }
        }, () -> {});
    }

    @FXML
    private void handleNewUser() { showUserDialog(new Utilisateur()); }

    @FXML
    private void handleEditUser() {
        Utilisateur selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) showUserDialog(selected);
    }

    @FXML
    private void handleDeleteUser() {
        Utilisateur selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!confirm("Supprimer le compte " + selected.getUsername() + " ?")) return;
        runAsync(() -> {
            try { ApiClient.delete("/utilisateurs/" + selected.getId() + "/"); }
            catch (Exception e) { throw new RuntimeException(e); }
        }, this::loadUsers);
    }

    private void showUserDialog(Utilisateur user) {
        boolean isNew = user.getId() == null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Nouveau compte utilisateur" : "Modifier le compte");
        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField usernameField = new TextField(user.getUsername());
        TextField emailField = new TextField(user.getEmail());
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(isNew ? "mot de passe initial" : "laisser vide pour ne pas changer");
        CheckBox staffBox = new CheckBox("Administrateur");
        staffBox.setSelected(user.isStaff());
        CheckBox activeBox = new CheckBox("Actif");
        activeBox.setSelected(isNew || user.isActive());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.addRow(0, new Label("Identifiant"), usernameField);
        grid.addRow(1, new Label("Email"), emailField);
        grid.addRow(2, new Label("Mot de passe"), passwordField);
        grid.addRow(3, staffBox);
        grid.addRow(4, activeBox);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveType) {
            if (usernameField.getText().isBlank() || emailField.getText().isBlank()) {
                new Alert(Alert.AlertType.ERROR, "Identifiant et email sont obligatoires.").showAndWait();
                return;
            }
            user.setUsername(usernameField.getText().trim());
            user.setEmail(emailField.getText().trim());
            user.setStaff(staffBox.isSelected());
            user.setActive(activeBox.isSelected());
            if (!passwordField.getText().isBlank()) user.setPassword(passwordField.getText());

            runAsync(() -> {
                try {
                    if (isNew) ApiClient.post("/utilisateurs/", user);
                    else ApiClient.patch("/utilisateurs/" + user.getId() + "/", user);
                } catch (Exception e) { throw new RuntimeException(e); }
            }, this::loadUsers);
        }
    }

    @FXML
    private void handleLogout() {
        Session.clear();
        SceneManager.showLogin();
    }
}