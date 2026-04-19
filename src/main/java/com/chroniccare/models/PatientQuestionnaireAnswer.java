package com.chroniccare.models;

import java.sql.Timestamp;

public class PatientQuestionnaireAnswer {
    private int id;
    private int userId;
    private String conditionName;
    private String questionKey;
    private String questionLabel;
    private String answerValue;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getConditionName() { return conditionName; }
    public void setConditionName(String conditionName) { this.conditionName = conditionName; }

    public String getQuestionKey() { return questionKey; }
    public void setQuestionKey(String questionKey) { this.questionKey = questionKey; }

    public String getQuestionLabel() { return questionLabel; }
    public void setQuestionLabel(String questionLabel) { this.questionLabel = questionLabel; }

    public String getAnswerValue() { return answerValue; }
    public void setAnswerValue(String answerValue) { this.answerValue = answerValue; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}