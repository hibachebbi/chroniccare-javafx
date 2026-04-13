package com.chroniccare.models;

import java.time.LocalDateTime;

public class Commentaire {
    private int id;
    private String contenu;
    private boolean isAnonymous;
    private int likeCount;
    private LocalDateTime createdAt;
    private int publicationId;
    private Integer auteurId;

    public Commentaire() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public boolean isAnonymous() { return isAnonymous; }
    public void setAnonymous(boolean anonymous) { isAnonymous = anonymous; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getPublicationId() { return publicationId; }
    public void setPublicationId(int publicationId) { this.publicationId = publicationId; }
    public Integer getAuteurId() { return auteurId; }
    public void setAuteurId(Integer auteurId) { this.auteurId = auteurId; }
}