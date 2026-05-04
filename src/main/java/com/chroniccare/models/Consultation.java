package com.chroniccare.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Consultation {
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private int id;
    private int appointmentId;
    private int patientId;
    private int nutritionistId;
    private String patientName;
    private String nutritionistName;
    private String appointmentStatus;
    private String theme;
    private String summary;
    private String recommendations;
    private String mealPlan;
    private String objectives;
    private String medicalNotes;
    private LocalDateTime consultationDateTime;
    private LocalDate followUpDate;
    private boolean read;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(int appointmentId) {
        this.appointmentId = appointmentId;
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

    public String getAppointmentStatus() {
        return appointmentStatus;
    }

    public void setAppointmentStatus(String appointmentStatus) {
        this.appointmentStatus = appointmentStatus;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }

    public String getMealPlan() {
        return mealPlan;
    }

    public void setMealPlan(String mealPlan) {
        this.mealPlan = mealPlan;
    }

    public String getObjectives() {
        return objectives;
    }

    public void setObjectives(String objectives) {
        this.objectives = objectives;
    }

    public String getMedicalNotes() {
        return medicalNotes;
    }

    public void setMedicalNotes(String medicalNotes) {
        this.medicalNotes = medicalNotes;
    }

    public LocalDateTime getConsultationDateTime() {
        return consultationDateTime;
    }

    public void setConsultationDateTime(LocalDateTime consultationDateTime) {
        this.consultationDateTime = consultationDateTime;
    }

    public LocalDate getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDate followUpDate) {
        this.followUpDate = followUpDate;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getConsultationDateDisplay() {
        return consultationDateTime == null ? "-" : consultationDateTime.format(DATE_TIME_FORMAT);
    }

    public String getFollowUpDateDisplay() {
        return followUpDate == null ? "-" : followUpDate.format(DATE_FORMAT);
    }

    public String getAppointmentStatusLabel() {
        if (Appointment.STATUS_PLANNED.equals(appointmentStatus)) return "Planifiee";
        if (Appointment.STATUS_COMPLETED.equals(appointmentStatus)) return "Terminee";
        if (Appointment.STATUS_PENDING.equals(appointmentStatus)) return "En attente";
        return appointmentStatus != null ? appointmentStatus : "-";
    }
}
