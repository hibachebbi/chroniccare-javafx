package com.chroniccare.models;

import java.sql.Timestamp;

public class PatientMedicalRecord {
    private int id;
    private int userId;
    private String mainCondition;
    private String secondaryConditions;
    private String questionnaireAnswers;
    private String generatedRecord;
    private String importedPdfText;
    private String pdfPath;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getMainCondition() { return mainCondition; }
    public void setMainCondition(String mainCondition) { this.mainCondition = mainCondition; }

    public String getSecondaryConditions() { return secondaryConditions; }
    public void setSecondaryConditions(String secondaryConditions) { this.secondaryConditions = secondaryConditions; }

    public String getQuestionnaireAnswers() { return questionnaireAnswers; }
    public void setQuestionnaireAnswers(String questionnaireAnswers) { this.questionnaireAnswers = questionnaireAnswers; }

    public String getGeneratedRecord() { return generatedRecord; }
    public void setGeneratedRecord(String generatedRecord) { this.generatedRecord = generatedRecord; }

    public String getImportedPdfText() { return importedPdfText; }
    public void setImportedPdfText(String importedPdfText) { this.importedPdfText = importedPdfText; }

    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}