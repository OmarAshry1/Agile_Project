package edu.curriculum.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing a Quiz Question
 */
public class QuizQuestion {
    private final String id;
    private String quizId;
    private int questionNumber;
    private String questionText;
    private QuestionType questionType;
    private int points;
    private List<QuizQuestionOption> options;

    public QuizQuestion(String id, String quizId, int questionNumber, String questionText,
                       QuestionType questionType, int points, List<QuizQuestionOption> options) {
        this.id = id;
        this.quizId = quizId;
        this.questionNumber = questionNumber;
        this.questionText = questionText;
        this.questionType = questionType;
        this.points = points;
        this.options = options != null ? new ArrayList<>(options) : new ArrayList<>();
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getQuizId() { return quizId; }
    public int getQuestionNumber() { return questionNumber; }
    public String getQuestionText() { return questionText; }
    public QuestionType getQuestionType() { return questionType; }
    public int getPoints() { return points; }
    public List<QuizQuestionOption> getOptions() { return new ArrayList<>(options); }

    // --- Setters ---
    public void setQuizId(String quizId) { this.quizId = quizId; }
    public void setQuestionNumber(int questionNumber) { this.questionNumber = questionNumber; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }
    public void setPoints(int points) { this.points = points; }
    public void setOptions(List<QuizQuestionOption> options) {
        this.options = options != null ? new ArrayList<>(options) : new ArrayList<>();
    }

    public void addOption(QuizQuestionOption option) {
        if (option != null) {
            options.add(option);
        }
    }

    @Override
    public String toString() {
        return "Question " + questionNumber + ": " + questionText + " (" + questionType + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuizQuestion)) return false;
        QuizQuestion that = (QuizQuestion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

