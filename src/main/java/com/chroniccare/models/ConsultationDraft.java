package com.chroniccare.models;

public class ConsultationDraft {
    private String theme;
    private String summary;
    private String recommendations;
    private String mealPlan;
    private String objectives;
    private String followUpMessage;

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

    public String getFollowUpMessage() {
        return followUpMessage;
    }

    public void setFollowUpMessage(String followUpMessage) {
        this.followUpMessage = followUpMessage;
    }
}
