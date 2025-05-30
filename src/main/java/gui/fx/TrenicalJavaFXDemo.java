// CREA QUESTO FILE IN: src/main/java/gui/fx/TrenicalJavaFXDemo.java
package fx;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;

/**
 * 🎨 TRENICAL JavaFX - INTERFACCIA MODERNA
 */
public class TrenicalJavaFXDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Layout principale con gradiente
        BorderPane root = new BorderPane();
        root.setStyle("""
            -fx-background-color: linear-gradient(to bottom, 
                #f8f9fa 0%, #e9ecef 100%);
        """);

        // Header moderno
        HBox header = createModernHeader();
        root.setTop(header);

        // Sidebar elegante
        VBox sidebar = createElegantSidebar();
        root.setLeft(sidebar);

        // Area contenuto con cards
        ScrollPane content = createContentWithCards();
        root.setCenter(content);

        // Status bar stiloso
        HBox statusBar = createStylishStatusBar();
        root.setBottom(statusBar);

        // Scena con styling
        Scene scene = new Scene(root, 1200, 800);
        // Rimuovo il CSS file per ora

        primaryStage.setTitle("🚂 TreniCal - Interfaccia Moderna JavaFX");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Animazione di entrata
        playWelcomeAnimation(root);
    }

    private HBox createModernHeader() {
        HBox header = new HBox(20);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("""
            -fx-background-color: linear-gradient(to right, 
                #667eea 0%, #764ba2 100%);
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);
        """);

        // Logo e titolo
        Label logo = new Label("🚂");
        logo.setStyle("-fx-font-size: 28px;");

        Label title = new Label("TreniCal");
        title.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 24px;
            -fx-font-weight: bold;
        """);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Pulsanti header moderni
        Button search = createGlassButton("🔍 Cerca", "#28a745");
        Button profile = createGlassButton("👤 Profilo", "#17a2b8");
        Button notifications = createGlassButton("🔔 Notifiche", "#ffc107");

        header.getChildren().addAll(logo, title, spacer, search, profile, notifications);
        return header;
    }

    private Button createGlassButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-text-fill: white;
            -fx-background-radius: 20;
            -fx-padding: 8 16 8 16;
            -fx-font-weight: bold;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 1);
            -fx-cursor: hand;
        """, color));

        // Effetto hover
        button.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        });

        button.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });

        return button;
    }

    private VBox createElegantSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(250);
        sidebar.setPadding(new Insets(20));
        sidebar.setStyle("""
            -fx-background-color: rgba(255, 255, 255, 0.9);
            -fx-background-radius: 0 15 15 0;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 5, 0);
        """);

        // Sezione utente
        VBox userSection = new VBox(10);
        userSection.setAlignment(Pos.CENTER);
        userSection.setStyle("""
            -fx-background-color: linear-gradient(45deg, #ff9a9e 0%, #fecfef 100%);
            -fx-background-radius: 15;
            -fx-padding: 20;
        """);

        Label userAvatar = new Label("👤");
        userAvatar.setStyle("-fx-font-size: 40px;");

        Label userName = new Label("Mario Rossi");
        userName.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label userStatus = new Label("Cliente Fedeltà Gold");
        userStatus.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        userSection.getChildren().addAll(userAvatar, userName, userStatus);

        // Menu navigazione
        VBox menuSection = new VBox(5);
        String[] menuItems = {
                "🔍 Cerca Tratte", "🎫 I Miei Biglietti", "💳 Carta Fedeltà",
                "🎉 Promozioni", "📊 Statistiche", "⚙️ Impostazioni"
        };

        for (String item : menuItems) {
            Button menuButton = createMenuButton(item);
            menuSection.getChildren().add(menuButton);
        }

        sidebar.getChildren().addAll(userSection, new Separator(), menuSection);
        return sidebar;
    }

    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #333;
            -fx-font-size: 14px;
            -fx-padding: 12 16 12 16;
            -fx-background-radius: 8;
            -fx-cursor: hand;
        """);

        button.setOnMouseEntered(e -> {
            button.setStyle("""
                -fx-background-color: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
                -fx-text-fill: white;
                -fx-font-size: 14px;
                -fx-padding: 12 16 12 16;
                -fx-background-radius: 8;
                -fx-cursor: hand;
            """);
        });

        button.setOnMouseExited(e -> {
            button.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #333;
                -fx-font-size: 14px;
                -fx-padding: 12 16 12 16;
                -fx-background-radius: 8;
                -fx-cursor: hand;
            """);
        });

        return button;
    }

    private ScrollPane createContentWithCards() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        // Cards moderne per tratte
        String[][] tratte = {
                {"Milano Centrale", "Roma Termini", "09:30", "13:45", "€89.50", "true"},
                {"Napoli Centrale", "Firenze SMN", "14:20", "18:15", "€65.00", "false"},
                {"Torino Porta Nuova", "Bologna Centrale", "07:45", "11:30", "€45.50", "true"},
                {"ReggioCalabria", "Milano", "06:00", "14:30", "€125.00", "true"},
                {"Rende", "Roma", "08:15", "15:45", "€95.50", "false"}
        };

        for (String[] tratta : tratte) {
            VBox card = createTrainCard(
                    tratta[0], tratta[1], tratta[2], tratta[3], tratta[4],
                    Boolean.parseBoolean(tratta[5])
            );
            content.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        return scroll;
    }

    private VBox createTrainCard(String from, String to, String departure,
                                 String arrival, String price, boolean available) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setStyle(String.format("""
            -fx-background-color: white;
            -fx-background-radius: 15;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);
            -fx-border-color: %s;
            -fx-border-width: 2;
            -fx-border-radius: 15;
        """, available ? "#28a745" : "#ffc107"));

        // Header card
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label route = new Label(from + " → " + to);
        route.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = new Label(available ? "✅ Disponibile" : "⚠️ Ultimi posti");
        status.setStyle(String.format("-fx-text-fill: %s; -fx-font-weight: bold;",
                available ? "#28a745" : "#ffc107"));

        header.getChildren().addAll(route, spacer, status);

        // Dettagli orari
        HBox timeInfo = new HBox(30);
        timeInfo.setAlignment(Pos.CENTER_LEFT);

        VBox depInfo = new VBox(5);
        Label depLabel = new Label("Partenza");
        depLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        Label depTime = new Label(departure);
        depTime.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        depInfo.getChildren().addAll(depLabel, depTime);

        Label arrow = new Label("✈️");
        arrow.setStyle("-fx-font-size: 20px;");

        VBox arrInfo = new VBox(5);
        Label arrLabel = new Label("Arrivo");
        arrLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        Label arrTime = new Label(arrival);
        arrTime.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        arrInfo.getChildren().addAll(arrLabel, arrTime);

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        VBox priceInfo = new VBox(5);
        priceInfo.setAlignment(Pos.CENTER_RIGHT);
        Label priceLabel = new Label("Prezzo");
        priceLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        Label priceValue = new Label(price);
        priceValue.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #28a745;");
        priceInfo.getChildren().addAll(priceLabel, priceValue);

        timeInfo.getChildren().addAll(depInfo, arrow, arrInfo, spacer2, priceInfo);

        // Pulsanti azione
        HBox actions = new HBox(10);
        Button buyButton = createActionButton("💳 Acquista", "#28a745");
        Button reserveButton = createActionButton("📝 Prenota", "#17a2b8");
        Button detailsButton = createActionButton("ℹ️ Dettagli", "#6c757d");

        actions.getChildren().addAll(buyButton, reserveButton, detailsButton);

        card.getChildren().addAll(header, timeInfo, new Separator(), actions);

        // Animazione al mouse hover
        card.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);
            scale.setToX(1.02);
            scale.setToY(1.02);
            scale.play();
        });

        card.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });

        return card;
    }

    private Button createActionButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-text-fill: white;
            -fx-background-radius: 20;
            -fx-padding: 8 16 8 16;
            -fx-font-weight: bold;
            -fx-cursor: hand;
        """, color));

        return button;
    }

    private HBox createStylishStatusBar() {
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(10, 20, 10, 20));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("""
            -fx-background-color: rgba(255, 255, 255, 0.9);
            -fx-border-color: #dee2e6;
            -fx-border-width: 1 0 0 0;
        """);

        Label status = new Label("🟢 Sistema connesso");
        status.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label time = new Label("🕒 " + java.time.LocalDateTime.now().toString().substring(0, 19));
        time.setStyle("-fx-text-fill: #6c757d;");

        ProgressBar connection = new ProgressBar(0.8);
        connection.setPrefWidth(100);
        connection.setStyle("-fx-accent: #28a745;");

        statusBar.getChildren().addAll(status, spacer, time, new Label("Connessione:"), connection);
        return statusBar;
    }

    private void playWelcomeAnimation(BorderPane root) {
        // Animazione fade-in
        FadeTransition fade = new FadeTransition(Duration.millis(1000), root);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();

        // Animazione slide-in dalla sinistra per sidebar
        if (root.getLeft() != null) {
            TranslateTransition slide = new TranslateTransition(Duration.millis(800), root.getLeft());
            slide.setFromX(-250);
            slide.setToX(0);
            slide.play();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}