-- Create the 'users' table
CREATE TABLE users (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL
);

-- Create the 'surveys' table
CREATE TABLE surveys (
                         id INT AUTO_INCREMENT PRIMARY KEY,
                         title VARCHAR(255) NOT NULL,
                         description TEXT,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create the 'questions' table
CREATE TABLE questions (
                           id INT AUTO_INCREMENT PRIMARY KEY,
                           survey_id INT,
                           question_text TEXT NOT NULL

);

-- Create the 'options' table
CREATE TABLE options (
                         id INT AUTO_INCREMENT PRIMARY KEY,
                         question_id INT,
                         option_text VARCHAR(255) NOT NULL

);

-- Create the 'votes' table
CREATE TABLE votes (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       survey_id INT,
                       question_id INT,
                       option_id INT,
                       voted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

);
