package com.example.java_final_project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javafx.scene.control.Button;
import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        Button b = new Button("button");

        // create a stack pane
        StackPane r = new StackPane();

        // add button
        r.getChildren().add(b);

        Scene scene = new Scene(r, 600, 400);


        stage.setTitle("Hello!");
        stage.setScene(scene);



        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}