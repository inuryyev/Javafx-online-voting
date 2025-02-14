package com.example.java_final_project;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class VotingSystem extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        showMainScreen();
    }

    private void showMainScreen() {
        Label titleLabel = new Label("Online Voting System");
        Button adminPanelButton = new Button("Admin Panel");
        Button surveysButton = new Button("Surveys");

        adminPanelButton.setOnAction(e -> showLoginScreen());
        surveysButton.setOnAction(e -> showSurveysScreen());

        VBox layout = new VBox(20, titleLabel, adminPanelButton, surveysButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 400, 300);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Voting System");
        primaryStage.show();
    }

    private void showLoginScreen() {
        Label loginLabel = new Label("Admin Login");
        Button loginButton = new Button("Login");
        Button backButton = new Button("Back");

        loginButton.setOnAction(e -> showAdminPanel());
        backButton.setOnAction(e -> showMainScreen());

        VBox layout = new VBox(20, loginLabel, loginButton, backButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 400, 300);

        primaryStage.setScene(scene);
    }

    private void showAdminPanel() {
        Label adminLabel = new Label("Admin Panel");
        Button logoutButton = new Button("Logout");

        logoutButton.setOnAction(e -> showMainScreen());

        VBox layout = new VBox(20, adminLabel, logoutButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 400, 300);

        primaryStage.setScene(scene);
    }

    private void showSurveysScreen() {
        Label surveysLabel = new Label("Surveys Page");
        Button backButton = new Button("Back");

        backButton.setOnAction(e -> showMainScreen());

        VBox layout = new VBox(20, surveysLabel, backButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 400, 300);

        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
