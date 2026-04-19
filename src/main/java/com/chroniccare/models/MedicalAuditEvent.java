package com.chroniccare.models;

import java.sql.Timestamp;

public class MedicalAuditEvent {

    private int id;
    private String actorName;
    private String actorRole;
    private String action;
    private String targetName;
    private String details;
    private Timestamp createdAt;

    public MedicalAuditEvent() {}

    public MedicalAuditEvent(
            int id,
            String actorName,
            String actorRole,
            String action,
            String targetName,
            String details,
            Timestamp createdAt
    ) {
        this.id = id;
        this.actorName = actorName;
        this.actorRole = actorRole;
        this.action = action;
        this.targetName = targetName;
        this.details = details;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getActorName() {
        return actorName;
    }

    public String getActorRole() {
        return actorRole;
    }

    public String getAction() {
        return action;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getDetails() {
        return details;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }
}

