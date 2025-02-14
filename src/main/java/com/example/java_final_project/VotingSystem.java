package com.example.java_final_project;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.*;

public class VotingSystem extends Application {

    private Stage primaryStage;
    private Connection connection;
    private boolean isAdminLoggedIn = false;
    private ObservableList<Survey> currentSurveys;
    private int currentSurveyIndex;
    private ObservableList<Question> currentQuestions;
    private int currentQuestionIndex;

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
        surveysButton.setOnAction(e -> startUserSurvey());

        VBox layout = new VBox(20, titleLabel, adminPanelButton, surveysButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Voting System");
        primaryStage.show();
    }

    private void startUserSurvey() {
        currentSurveys = FXCollections.observableArrayList();
        String query = "SELECT id, title FROM surveys";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                currentSurveys.add(new Survey(rs.getInt("id"), rs.getString("title")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (currentSurveys.isEmpty()) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "No surveys available.").showAndWait();
            showMainScreen();
            return;
        }
        currentSurveyIndex = 0;
        showUserSurveyScreen(currentSurveys.get(currentSurveyIndex));
    }

    private void showUserSurveyScreen(Survey survey) {
        currentQuestions = FXCollections.observableArrayList();
        currentQuestionIndex = 0;
        String query = "SELECT id, question_text FROM questions WHERE survey_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, survey.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                currentQuestions.add(new Question(rs.getInt("id"), rs.getString("question_text")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (currentQuestions.isEmpty()) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "No questions found for this survey.").showAndWait();
            showNextSurvey();
            return;
        }
        showQuestionScreen(survey, currentQuestions.get(currentQuestionIndex));
    }

    private void showQuestionScreen(Survey survey, Question question) {
        ObservableList<Option> options = FXCollections.observableArrayList();
        String query = "SELECT id, option_text FROM options WHERE question_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, question.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                options.add(new Option(rs.getInt("id"), rs.getString("option_text")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Label surveyTitleLabel = new Label(survey.getTitle());
        surveyTitleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Label questionLabel = new Label(question.getText());
        questionLabel.setStyle("-fx-font-size: 18px;");

        ToggleGroup toggleGroup = new ToggleGroup();
        VBox optionsBox = new VBox(10);
        for (Option opt : options) {
            RadioButton radioButton = new RadioButton(opt.getText());
            radioButton.setUserData(opt);
            radioButton.setToggleGroup(toggleGroup);
            optionsBox.getChildren().add(radioButton);
        }

        Label errorLabel = new Label();
        Button submitButton = new Button("Submit");
        submitButton.setOnAction(e -> {
            RadioButton selected = (RadioButton) toggleGroup.getSelectedToggle();
            if (selected == null) {
                errorLabel.setText("Please select an option.");
                return;
            }
            Option selectedOption = (Option) selected.getUserData();
            recordVote(survey.getId(), question.getId(), selectedOption.getId());
            currentQuestionIndex++;
            if (currentQuestionIndex < currentQuestions.size()) {
                showQuestionScreen(survey, currentQuestions.get(currentQuestionIndex));
            } else {
                showNextSurvey();
            }
        });

        VBox layout = new VBox(20, surveyTitleLabel, questionLabel, optionsBox, errorLabel, submitButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
    }

    private void showNextSurvey() {
        currentSurveyIndex++;
        if (currentSurveyIndex < currentSurveys.size()) {
            Survey nextSurvey = currentSurveys.get(currentSurveyIndex);
            showUserSurveyScreen(nextSurvey);
        } else {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "All surveys completed.").showAndWait();
            showMainScreen();
        }
    }

    private void recordVote(int surveyId, int questionId, int optionId) {
        String query = "INSERT INTO votes (survey_id, question_id, option_id) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, surveyId);
            stmt.setInt(2, questionId);
            stmt.setInt(3, optionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showVotesScreen() {
        TableView<VoteRecord> table = new TableView<>();
        TableColumn<VoteRecord, String> surveyCol = new TableColumn<>("Survey");
        surveyCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSurveyTitle()));
        TableColumn<VoteRecord, String> questionCol = new TableColumn<>("Question");
        questionCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getQuestionText()));
        TableColumn<VoteRecord, String> optionCol = new TableColumn<>("Option");
        optionCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOptionText()));
        TableColumn<VoteRecord, String> dateCol = new TableColumn<>("Voted At");
        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getVotedAt()));
        table.getColumns().addAll(surveyCol, questionCol, optionCol, dateCol);

        ObservableList<VoteRecord> votesList = FXCollections.observableArrayList();
        String query = "SELECT s.title AS survey_title, q.question_text, o.option_text, v.voted_at " +
                "FROM votes v " +
                "JOIN surveys s ON v.survey_id = s.id " +
                "JOIN questions q ON v.question_id = q.id " +
                "JOIN options o ON v.option_id = o.id " +
                "ORDER BY v.voted_at";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                votesList.add(new VoteRecord(rs.getString("survey_title"),
                        rs.getString("question_text"),
                        rs.getString("option_text"),
                        rs.getString("voted_at")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        table.setItems(votesList);

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> showAdminPanel());

        VBox layout = new VBox(20, table, backButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
    }

    private void showSurveyCrudScreen() {
        Label titleLabel = new Label("Manage Surveys");
        TextField surveyTitleField = new TextField();
        surveyTitleField.setPromptText("Survey Title");
        Button addSurveyButton = new Button("Add Survey");
        Button updateSurveyButton = new Button("Update Selected");
        Button deleteSurveyButton = new Button("Delete Selected");
        ListView<Survey> surveyListView = new ListView<>();
        ObservableList<Survey> surveyList = FXCollections.observableArrayList();

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
            Survey selectedSurvey = surveyListView.getSelectionModel().getSelectedItem();
            String newTitle = surveyTitleField.getText();
            if (selectedSurvey != null && !newTitle.isEmpty()) {
                updateSurvey(selectedSurvey.getId(), newTitle);
                surveyTitleField.clear();
                loadSurveys(surveyList);
            }
        });

        deleteSurveyButton.setOnAction(e -> {
            Survey selectedSurvey = surveyListView.getSelectionModel().getSelectedItem();
            if (selectedSurvey != null) {
                deleteSurvey(selectedSurvey.getId());
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

    private void loadSurveys(ObservableList<Survey> surveyList) {
        surveyList.clear();
        String query = "SELECT id, title FROM surveys";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                surveyList.add(new Survey(rs.getInt("id"), rs.getString("title")));
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

    private void updateSurvey(int id, String newTitle) {
        String query = "UPDATE surveys SET title = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, newTitle);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteSurvey(int id) {
        String query = "DELETE FROM surveys WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        Button viewVotesButton = new Button("View Votes");
        Button logoutButton = new Button("Logout");

        addSurveyButton.setOnAction(e -> showSurveyCrudScreen());
        manageQuestionsButton.setOnAction(e -> showQuestionsCrudScreen());
        manageOptionsButton.setOnAction(e -> showOptionsCrudScreen());
        viewVotesButton.setOnAction(e -> showVotesScreen());
        logoutButton.setOnAction(e -> {
            isAdminLoggedIn = false;
            showMainScreen();
        });

        VBox layout = new VBox(20, createHeader(), adminLabel, addSurveyButton, manageQuestionsButton, manageOptionsButton, viewVotesButton, logoutButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
    }

    private void showQuestionsCrudScreen() {
        Label titleLabel = new Label("Manage Questions");
        Label surveyLabel = new Label("Select Survey:");
        ComboBox<Survey> surveyDropdown = new ComboBox<>();
        TextField questionField = new TextField();
        questionField.setPromptText("Question Text");
        Button addQuestionButton = new Button("Add Question");
        Button updateQuestionButton = new Button("Update Selected");
        Button deleteQuestionButton = new Button("Delete Selected");
        ListView<Question> questionListView = new ListView<>();
        ObservableList<Question> questionList = FXCollections.observableArrayList();

        loadSurveysIntoDropdown(surveyDropdown);
        surveyDropdown.setOnAction(e -> {
            Survey selectedSurvey = surveyDropdown.getValue();
            if (selectedSurvey != null) {
                loadQuestions(questionList, selectedSurvey.getId());
            }
        });
        questionListView.setItems(questionList);

        addQuestionButton.setOnAction(e -> {
            Survey selectedSurvey = surveyDropdown.getValue();
            String questionText = questionField.getText();
            if (selectedSurvey != null && !questionText.isEmpty()) {
                addQuestion(selectedSurvey.getId(), questionText);
                questionField.clear();
                loadQuestions(questionList, selectedSurvey.getId());
            }
        });
        updateQuestionButton.setOnAction(e -> {
            Question selectedQuestion = questionListView.getSelectionModel().getSelectedItem();
            String newQuestionText = questionField.getText();
            if (selectedQuestion != null && !newQuestionText.isEmpty()) {
                updateQuestion(selectedQuestion.getId(), newQuestionText);
                questionField.clear();
                Survey selectedSurvey = surveyDropdown.getValue();
                if (selectedSurvey != null) {
                    loadQuestions(questionList, selectedSurvey.getId());
                }
            }
        });
        deleteQuestionButton.setOnAction(e -> {
            Question selectedQuestion = questionListView.getSelectionModel().getSelectedItem();
            if (selectedQuestion != null) {
                deleteQuestion(selectedQuestion.getId());
                Survey selectedSurvey = surveyDropdown.getValue();
                if (selectedSurvey != null) {
                    loadQuestions(questionList, selectedSurvey.getId());
                }
            }
        });

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> showAdminPanel());

        VBox layout = new VBox(20, titleLabel, surveyLabel, surveyDropdown, questionField, addQuestionButton, updateQuestionButton, questionListView, deleteQuestionButton, backButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
    }

    private void loadQuestions(ObservableList<Question> questionList, int surveyId) {
        questionList.clear();
        String query = "SELECT id, question_text FROM questions WHERE survey_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, surveyId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                questionList.add(new Question(rs.getInt("id"), rs.getString("question_text")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addQuestion(int surveyId, String questionText) {
        String query = "INSERT INTO questions (survey_id, question_text) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, surveyId);
            stmt.setString(2, questionText);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateQuestion(int questionId, String newQuestionText) {
        String query = "UPDATE questions SET question_text = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, newQuestionText);
            stmt.setInt(2, questionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteQuestion(int questionId) {
        String query = "DELETE FROM questions WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, questionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSurveysIntoDropdown(ComboBox<Survey> dropdown) {
        dropdown.getItems().clear();
        String query = "SELECT id, title FROM surveys";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                dropdown.getItems().add(new Survey(rs.getInt("id"), rs.getString("title")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showOptionsCrudScreen() {
        Label titleLabel = new Label("Manage Options");
        Label surveyLabel = new Label("Select Survey:");
        ComboBox<Survey> surveyDropdown = new ComboBox<>();
        Label questionLabel = new Label("Select Question:");
        ComboBox<Question> questionDropdown = new ComboBox<>();
        TextField optionField = new TextField();
        optionField.setPromptText("Option Text");
        Button addOptionButton = new Button("Add Option");
        Button updateOptionButton = new Button("Update Selected");
        Button deleteOptionButton = new Button("Delete Selected");
        ListView<Option> optionListView = new ListView<>();
        ObservableList<Option> optionList = FXCollections.observableArrayList();

        loadSurveysIntoDropdown(surveyDropdown);
        surveyDropdown.setOnAction(e -> {
            Survey selectedSurvey = surveyDropdown.getValue();
            if (selectedSurvey != null) {
                loadQuestionsIntoDropdown(questionDropdown, selectedSurvey.getId());
            }
        });
        questionDropdown.setOnAction(e -> {
            Question selectedQuestion = questionDropdown.getValue();
            if (selectedQuestion != null) {
                loadOptions(optionList, selectedQuestion.getId());
            }
        });
        optionListView.setItems(optionList);

        addOptionButton.setOnAction(e -> {
            Question selectedQuestion = questionDropdown.getValue();
            String optionText = optionField.getText();
            if (selectedQuestion != null && !optionText.isEmpty()) {
                addOption(selectedQuestion.getId(), optionText);
                optionField.clear();
                loadOptions(optionList, selectedQuestion.getId());
            }
        });
        updateOptionButton.setOnAction(e -> {
            Option selectedOption = optionListView.getSelectionModel().getSelectedItem();
            String newOptionText = optionField.getText();
            if (selectedOption != null && !newOptionText.isEmpty()) {
                updateOption(selectedOption.getId(), newOptionText);
                optionField.clear();
                Question selectedQuestion = questionDropdown.getValue();
                if (selectedQuestion != null) {
                    loadOptions(optionList, selectedQuestion.getId());
                }
            }
        });
        deleteOptionButton.setOnAction(e -> {
            Option selectedOption = optionListView.getSelectionModel().getSelectedItem();
            if (selectedOption != null) {
                deleteOption(selectedOption.getId());
                Question selectedQuestion = questionDropdown.getValue();
                if (selectedQuestion != null) {
                    loadOptions(optionList, selectedQuestion.getId());
                }
            }
        });

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> showAdminPanel());

        VBox layout = new VBox(20, titleLabel, surveyLabel, surveyDropdown, questionLabel, questionDropdown, optionField, addOptionButton, updateOptionButton, optionListView, deleteOptionButton, backButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
    }

    private void loadQuestionsIntoDropdown(ComboBox<Question> dropdown, int surveyId) {
        dropdown.getItems().clear();
        String query = "SELECT id, question_text FROM questions WHERE survey_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, surveyId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                dropdown.getItems().add(new Question(rs.getInt("id"), rs.getString("question_text")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadOptions(ObservableList<Option> optionList, int questionId) {
        optionList.clear();
        String query = "SELECT id, option_text FROM options WHERE question_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, questionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                optionList.add(new Option(rs.getInt("id"), rs.getString("option_text")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addOption(int questionId, String optionText) {
        String query = "INSERT INTO options (question_id, option_text) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, questionId);
            stmt.setString(2, optionText);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateOption(int optionId, String newOptionText) {
        String query = "UPDATE options SET option_text = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, newOptionText);
            stmt.setInt(2, optionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteOption(int optionId) {
        String query = "DELETE FROM options WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, optionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class Survey {
        private int id;
        private String title;

        public Survey(int id, String title) {
            this.id = id;
            this.title = title;
        }
        public int getId() {
            return id;
        }
        public String getTitle() {
            return title;
        }
        @Override
        public String toString() {
            return title;
        }
    }

    public static class Question {
        private int id;
        private String text;

        public Question(int id, String text) {
            this.id = id;
            this.text = text;
        }
        public int getId() {
            return id;
        }
        public String getText() {
            return text;
        }
        @Override
        public String toString() {
            return text;
        }
    }

    public static class Option {
        private int id;
        private String text;

        public Option(int id, String text) {
            this.id = id;
            this.text = text;
        }
        public int getId() {
            return id;
        }
        public String getText() {
            return text;
        }
        @Override
        public String toString() {
            return text;
        }
    }

    public static class VoteRecord {
        private String surveyTitle;
        private String questionText;
        private String optionText;
        private String votedAt;

        public VoteRecord(String surveyTitle, String questionText, String optionText, String votedAt) {
            this.surveyTitle = surveyTitle;
            this.questionText = questionText;
            this.optionText = optionText;
            this.votedAt = votedAt;
        }
        public String getSurveyTitle() {
            return surveyTitle;
        }
        public String getQuestionText() {
            return questionText;
        }
        public String getOptionText() {
            return optionText;
        }
        public String getVotedAt() {
            return votedAt;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
