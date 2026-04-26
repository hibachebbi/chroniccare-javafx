package com.chroniccare.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Commentaire {
    private int id;
    private String contenu;
    private boolean isAnonymous;
    private int likeCount;
    private LocalDateTime createdAt;
    private int publicationId;
    private Integer auteurId;
    private Integer parentId; // null = commentaire racine, sinon = reponse
    private List<Commentaire> reponses = new ArrayList<>();

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
    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }
    public List<Commentaire> getReponses() { return reponses; }
    public void setReponses(List<Commentaire> reponses) { this.reponses = reponses; }
    public void addReponse(Commentaire r) { this.reponses.add(r); }
    public boolean isReponse() { return parentId != null; }
}