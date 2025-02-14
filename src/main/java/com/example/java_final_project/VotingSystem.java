package com.example.java_final_project;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.*;

public class VotingSystem extends Application {

    private Stage primaryStage;
    private Connection connection;
    private boolean isAdminLoggedIn = false;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        connectToDatabase();
        showMainScreen();
    }

    private void connectToDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/voting_system", "root", "");
            System.out.println("Database connected successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showMainScreen() {
        Label titleLabel = new Label("Online Voting System");
        Button adminPanelButton = new Button("Admin Panel");
        Button surveysButton = new Button("Surveys");

        adminPanelButton.setOnAction(e -> {
            if (isAdminLoggedIn) {
                showAdminPanel();
            } else {
                showLoginScreen();
            }
        });
        surveysButton.setOnAction(e -> showSurveysScreen());

        VBox layout = new VBox(20, titleLabel, adminPanelButton, surveysButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 800, 600);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Voting System");
        primaryStage.show();
    }

    private void showSurveyCrudScreen() {
        Label titleLabel = new Label("Manage Surveys");
        TextField surveyTitleField = new TextField();
        surveyTitleField.setPromptText("Survey Title");
        Button addSurveyButton = new Button("Add Survey");
        Button updateSurveyButton = new Button("Update Selected");
        Button deleteSurveyButton = new Button("Delete Selected");
        ListView<String> surveyListView = new ListView<>();
        ObservableList<String> surveyList = FXCollections.observableArrayList();

        loadSurveys(surveyList);
        surveyListView.setItems(surveyList);

        addSurveyButton.setOnAction(e -> {
            String surveyTitle = surveyTitleField.getText();
            if (!surveyTitle.isEmpty()) {
                addSurvey(surveyTitle);
                surveyTitleField.clear();
                loadSurveys(surveyList);
            }
        });

        updateSurveyButton.setOnAction(e -> {
            String selectedSurvey = surveyListView.getSelectionModel().getSelectedItem();
            String newTitle = surveyTitleField.getText();
            if (selectedSurvey != null && !newTitle.isEmpty()) {
                updateSurvey(selectedSurvey, newTitle);
                surveyTitleField.clear();
                loadSurveys(surveyList);
            }
        });

        deleteSurveyButton.setOnAction(e -> {
            String selectedSurvey = surveyListView.getSelectionModel().getSelectedItem();
            if (selectedSurvey != null) {
                deleteSurvey(selectedSurvey);
                loadSurveys(surveyList);
            }
        });

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> showAdminPanel());

        VBox layout = new VBox(20, titleLabel, surveyTitleField, addSurveyButton, updateSurveyButton, surveyListView, deleteSurveyButton, backButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 800, 600);

        primaryStage.setScene(scene);
    }

    private void loadSurveys(ObservableList<String> surveyList) {
        surveyList.clear();
        String query = "SELECT title FROM surveys";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                surveyList.add(rs.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addSurvey(String title) {
        String query = "INSERT INTO surveys (title) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, title);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateSurvey(String oldTitle, String newTitle) {
        String query = "UPDATE surveys SET title = ? WHERE title = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, newTitle);
            stmt.setString(2, oldTitle);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteSurvey(String title) {
        String query = "DELETE FROM surveys WHERE title = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, title);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private VBox createHeader() {
        Button homeButton = new Button("Home Page");
        homeButton.setOnAction(e -> showMainScreen());
        VBox header = new VBox(homeButton);
        header.setAlignment(Pos.TOP_CENTER);
        return header;
    }

    private void showLoginScreen() {
        Label loginLabel = new Label("Admin Login");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        Button loginButton = new Button("Login");
        Button backButton = new Button("Back");
        Label messageLabel = new Label();

        loginButton.setOnAction(e -> {
            if (authenticateAdmin(usernameField.getText(), passwordField.getText())) {
                isAdminLoggedIn = true;
                showAdminPanel();
            } else {
                messageLabel.setText("Invalid credentials");
            }
        });

        backButton.setOnAction(e -> showMainScreen());

        VBox layout = new VBox(20, createHeader(), loginLabel, usernameField, passwordField, loginButton, messageLabel, backButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 800, 600);

        primaryStage.setScene(scene);
    }

    private boolean authenticateAdmin(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showAdminPanel() {
        Label adminLabel = new Label("Admin Panel");
        Button addSurveyButton = new Button("Add Survey");
        Button manageQuestionsButton = new Button("Manage Questions");
        Button manageOptionsButton = new Button("Manage Options");
        Button logoutButton = new Button("Logout");

        addSurveyButton.setOnAction(e -> showSurveyCrudScreen());
        manageQuestionsButton.setOnAction(e -> showQuestionsCrudScreen());
        manageOptionsButton.setOnAction(e -> showOptionsCrudScreen());
        logoutButton.setOnAction(e -> {
            isAdminLoggedIn = false;
            showMainScreen();
        });

        VBox layout = new VBox(20, createHeader(), adminLabel, addSurveyButton, manageQuestionsButton, manageOptionsButton, logoutButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 800, 600);

        primaryStage.setScene(scene);
    }



    private void showQuestionsCrudScreen() {
        Label titleLabel = new Label("Manage Questions");
        ComboBox<String> surveyDropdown = new ComboBox<>();
        TextField questionField = new TextField();
        questionField.setPromptText("Question Text");
        Button addQuestionButton = new Button("Add Question");
        Button updateQuestionButton = new Button("Update Selected");
        Button deleteQuestionButton = new Button("Delete Selected");
        ListView<String> questionListView = new ListView<>();
        ObservableList<String> questionList = FXCollections.observableArrayList();

        loadSurveysIntoDropdown(surveyDropdown);
        surveyDropdown.setOnAction(e -> loadQuestions(questionList, surveyDropdown.getValue()));
        questionListView.setItems(questionList);

        addQuestionButton.setOnAction(e -> {
            String surveyTitle = surveyDropdown.getValue();
            String questionText = questionField.getText();
            if (surveyTitle != null && !questionText.isEmpty()) {
                addQuestion(surveyTitle, questionText);
                questionField.clear();
                loadQuestions(questionList, surveyTitle);
            }
        });

        updateQuestionButton.setOnAction(e -> {
            String selectedQuestion = questionListView.getSelectionModel().getSelectedItem();
            String newQuestionText = questionField.getText();
            if (selectedQuestion != null && !newQuestionText.isEmpty()) {
                updateQuestion(selectedQuestion, newQuestionText);
                questionField.clear();
                loadQuestions(questionList, surveyDropdown.getValue());
            }
        });

        deleteQuestionButton.setOnAction(e -> {
            String selectedQuestion = questionListView.getSelectionModel().getSelectedItem();
            if (selectedQuestion != null) {
                deleteQuestion(selectedQuestion);
                loadQuestions(questionList, surveyDropdown.getValue());
            }
        });

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> showAdminPanel());

        VBox layout = new VBox(20, titleLabel, surveyDropdown, questionField, addQuestionButton, updateQuestionButton, questionListView, deleteQuestionButton, backButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 800, 600);

        primaryStage.setScene(scene);
    }



    private void loadQuestions(ObservableList<String> questionList, String surveyTitle) {
        questionList.clear();
        String query = "SELECT question_text FROM questions WHERE survey_id = (SELECT id FROM surveys WHERE title = ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, surveyTitle);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                questionList.add(rs.getString("question_text"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addQuestion(String surveyTitle, String questionText) {
        String query = "INSERT INTO questions (survey_id, question_text) VALUES ((SELECT id FROM surveys WHERE title = ?), ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, surveyTitle);
            stmt.setString(2, questionText);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateQuestion(String oldQuestion, String newQuestion) {
        String query = "UPDATE questions SET question_text = ? WHERE question_text = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, newQuestion);
            stmt.setString(2, oldQuestion);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteQuestion(String questionText) {
        String query = "DELETE FROM questions WHERE question_text = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, questionText);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void loadQuestionsIntoDropdown(ComboBox<String> dropdown) {
        dropdown.getItems().clear();
        String query = "SELECT question_text FROM questions";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                dropdown.getItems().add(rs.getString("question_text"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }






    private void showSurveysScreen() {
        Label surveysLabel = new Label("Surveys Page");
        Button backButton = new Button("Back");

        backButton.setOnAction(e -> showMainScreen());

        VBox layout = new VBox(20, createHeader(), surveysLabel, backButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 800, 600);

        primaryStage.setScene(scene);
    }

    private void showOptionsCrudScreen() {
        Label titleLabel = new Label("Manage Options");
        ComboBox<String> surveyDropdown = new ComboBox<>();
        ComboBox<String> questionDropdown = new ComboBox<>();
        TextField optionField = new TextField();
        optionField.setPromptText("Option Text");
        Button addOptionButton = new Button("Add Option");
        Button updateOptionButton = new Button("Update Selected");
        Button deleteOptionButton = new Button("Delete Selected");
        ListView<String> optionListView = new ListView<>();
        ObservableList<String> optionList = FXCollections.observableArrayList();

        loadSurveysIntoDropdown(surveyDropdown);
        surveyDropdown.setOnAction(e -> loadQuestionsIntoDropdown(questionDropdown, surveyDropdown.getValue()));
        questionDropdown.setOnAction(e -> loadOptions(optionList, questionDropdown.getValue()));
        optionListView.setItems(optionList);

        addOptionButton.setOnAction(e -> {
            String questionText = questionDropdown.getValue();
            String optionText = optionField.getText();
            if (questionText != null && !optionText.isEmpty()) {
                addOption(questionText, optionText);
                optionField.clear();
                loadOptions(optionList, questionText);
            }
        });

        updateOptionButton.setOnAction(e -> {
            String selectedOption = optionListView.getSelectionModel().getSelectedItem();
            String newOptionText = optionField.getText();
            if (selectedOption != null && !newOptionText.isEmpty()) {
                updateOption(selectedOption, newOptionText);
                optionField.clear();
                loadOptions(optionList, questionDropdown.getValue());
            }
        });

        deleteOptionButton.setOnAction(e -> {
            String selectedOption = optionListView.getSelectionModel().getSelectedItem();
            if (selectedOption != null) {
                deleteOption(selectedOption);
                loadOptions(optionList, questionDropdown.getValue());
            }
        });

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> showAdminPanel());

        VBox layout = new VBox(20, titleLabel, surveyDropdown, questionDropdown, optionField, addOptionButton, updateOptionButton, optionListView, deleteOptionButton, backButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 800, 600);

        primaryStage.setScene(scene);
    }

    private void loadSurveysIntoDropdown(ComboBox<String> dropdown) {
        dropdown.getItems().clear();
        String query = "SELECT title FROM surveys";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                dropdown.getItems().add(rs.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadQuestionsIntoDropdown(ComboBox<String> dropdown, String surveyTitle) {
        dropdown.getItems().clear();
        String query = "SELECT question_text FROM questions WHERE survey_id = (SELECT id FROM surveys WHERE title = ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, surveyTitle);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                dropdown.getItems().add(rs.getString("question_text"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadOptions(ObservableList<String> optionList, String questionText) {
        optionList.clear();
        String query = "SELECT option_text FROM options WHERE question_id = (SELECT id FROM questions WHERE question_text = ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, questionText);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                optionList.add(rs.getString("option_text"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addOption(String questionText, String optionText) {
        String query = "INSERT INTO options (question_id, option_text) VALUES ((SELECT id FROM questions WHERE question_text = ?), ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, questionText);
            stmt.setString(2, optionText);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateOption(String oldOption, String newOption) {
        String query = "UPDATE options SET option_text = ? WHERE option_text = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, newOption);
            stmt.setString(2, oldOption);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteOption(String optionText) {
        String query = "DELETE FROM options WHERE option_text = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, optionText);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}