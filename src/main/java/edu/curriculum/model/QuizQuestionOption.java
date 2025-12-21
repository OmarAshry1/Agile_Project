package edu.curriculum.model;

import java.util.Objects;

/**
 * Model representing an option for an MCQ question
 */
public class QuizQuestionOption {
    private final String id;
    private String questionId;
    private String optionText;
    private boolean isCorrect;
    private int optionOrder;

    public QuizQuestionOption(String id, String questionId, String optionText, boolean isCorrect, int optionOrder) {
        this.id = id;
        this.questionId = questionId;
        this.optionText = optionText;
        this.isCorrect = isCorrect;
        this.optionOrder = optionOrder;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getQuestionId() { return questionId; }
    public String getOptionText() { return optionText; }
    public boolean isCorrect() { return isCorrect; }
    public int getOptionOrder() { return optionOrder; }

    // --- Setters ---
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    public void setOptionText(String optionText) { this.optionText = optionText; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
    public void setOptionOrder(int optionOrder) { this.optionOrder = optionOrder; }

    @Override
    public String toString() {
        return optionText + (isCorrect ? " (Correct)" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuizQuestionOption)) return false;
        QuizQuestionOption that = (QuizQuestionOption) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

