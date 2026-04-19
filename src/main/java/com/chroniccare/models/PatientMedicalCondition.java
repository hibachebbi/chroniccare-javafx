package com.chroniccare.models;

import java.sql.Timestamp;

public class PatientMedicalCondition {
    private int id;
    private int userId;
    private String conditionName;
    private Timestamp createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getConditionName() { return conditionName; }
    public void setConditionName(String conditionName) { this.conditionName = conditionName; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}