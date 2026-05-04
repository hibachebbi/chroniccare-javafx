package com.chroniccare.models;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private int recipientUserId;
    private int actorUserId;
    private Integer publicationId;
    private Integer commentaireId;
    private String type;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(int recipientUserId) { this.recipientUserId = recipientUserId; }

    public int getActorUserId() { return actorUserId; }
    public void setActorUserId(int actorUserId) { this.actorUserId = actorUserId; }

    public Integer getPublicationId() { return publicationId; }
    public void setPublicationId(Integer publicationId) { this.publicationId = publicationId; }

    public Integer getCommentaireId() { return commentaireId; }
    public void setCommentaireId(Integer commentaireId) { this.commentaireId = commentaireId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
