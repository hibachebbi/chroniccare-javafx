package com.chroniccare.models;

import java.sql.Timestamp;

public class PatientSegmentationRow {

    private final int userId;
    private final String patientName;
    private final String genre;
    private final String mainCondition;
    private final int secondaryCount;
    private final int activityScore;
    private final int views30;
    private final int updates30;
    private final Timestamp lastActivityAt;

    private final String riskLevel;
    private final int riskScore;
    private final String riskDetails;

    private final String behaviorSegment;
    private final String behaviorDetails;

    public PatientSegmentationRow(
            int userId,
            String patientName,
            String genre,
            String mainCondition,
            int secondaryCount,
            int activityScore,
            int views30,
            int updates30,
            Timestamp lastActivityAt,
            String riskLevel,
            int riskScore,
            String riskDetails,
            String behaviorSegment,
            String behaviorDetails
    ) {
        this.userId = userId;
        this.patientName = patientName;
        this.genre = genre;
        this.mainCondition = mainCondition;
        this.secondaryCount = secondaryCount;
        this.activityScore = activityScore;
        this.views30 = views30;
        this.updates30 = updates30;
        this.lastActivityAt = lastActivityAt;
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.riskDetails = riskDetails;
        this.behaviorSegment = behaviorSegment;
        this.behaviorDetails = behaviorDetails;
    }

    public int getUserId() {
        return userId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getGenre() {
        return genre;
    }

    public String getMainCondition() {
        return mainCondition;
    }

    public int getSecondaryCount() {
        return secondaryCount;
    }

    public int getActivityScore() {
        return activityScore;
    }

    public int getViews30() {
        return views30;
    }

    public int getUpdates30() {
        return updates30;
    }

    public Timestamp getLastActivityAt() {
        return lastActivityAt;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getRiskDetails() {
        return riskDetails;
    }

    public String getBehaviorSegment() {
        return behaviorSegment;
    }

    public String getBehaviorDetails() {
        return behaviorDetails;
    }
}

