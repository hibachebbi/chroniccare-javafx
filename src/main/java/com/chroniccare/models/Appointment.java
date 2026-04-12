package com.chroniccare.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Appointment {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PLANNED = "PLANNED";
    public static final String STATUS_REFUSED = "REFUSED";
    public static final String STATUS_RESCHEDULE_PROPOSED = "RESCHEDULE_PROPOSED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int id;
    private int patientId;
    private int nutritionistId;
    private String patientName;
    private String nutritionistName;
    private String motif;
    private LocalDateTime requestedDateTime;
    private LocalDateTime scheduledDateTime;
    private String urgencyLevel;
    private String followUpType;
    private String medicalSnapshot;
    private String status;
    private String nutritionistResponse;
    private LocalDateTime createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public int getNutritionistId() {
        return nutritionistId;
    }

    public void setNutritionistId(int nutritionistId) {
        this.nutritionistId = nutritionistId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getNutritionistName() {
        return nutritionistName;
    }

    public void setNutritionistName(String nutritionistName) {
        this.nutritionistName = nutritionistName;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public LocalDateTime getRequestedDateTime() {
        return requestedDateTime;
    }

    public void setRequestedDateTime(LocalDateTime requestedDateTime) {
        this.requestedDateTime = requestedDateTime;
    }

    public LocalDateTime getScheduledDateTime() {
        return scheduledDateTime;
    }

    public void setScheduledDateTime(LocalDateTime scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public String getFollowUpType() {
        return followUpType;
    }

    public void setFollowUpType(String followUpType) {
        this.followUpType = followUpType;
    }

    public String getMedicalSnapshot() {
        return medicalSnapshot;
    }

    public void setMedicalSnapshot(String medicalSnapshot) {
        this.medicalSnapshot = medicalSnapshot;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNutritionistResponse() {
        return nutritionistResponse;
    }

    public void setNutritionistResponse(String nutritionistResponse) {
        this.nutritionistResponse = nutritionistResponse;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRequestedDateTimeDisplay() {
        return formatDateTime(requestedDateTime);
    }

    public String getScheduledDateTimeDisplay() {
        return formatDateTime(scheduledDateTime);
    }

    public String getStatusLabel() {
        if (STATUS_PENDING.equals(status)) return "En attente";
        if (STATUS_PLANNED.equals(status)) return "Planifie";
        if (STATUS_REFUSED.equals(status)) return "Refuse";
        if (STATUS_RESCHEDULE_PROPOSED.equals(status)) return "Autre creneau propose";
        if (STATUS_COMPLETED.equals(status)) return "Termine";
        return status != null ? status : "-";
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DISPLAY_FORMAT);
    }
}
