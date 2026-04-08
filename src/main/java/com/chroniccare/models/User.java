package com.chroniccare.models;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String roles;
    private String telephone;
    private String genre;
    private String approvalStatus;
    private String medicalCondition;
    private String photoProfil;
    private boolean isActive;

    public User() {}

    public User(String nom, String prenom, String email,
                String password, String roles, String telephone,
                String genre) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.roles = roles;
        this.telephone = telephone;
        this.genre = genre;
        this.isActive = true;
        this.approvalStatus = "pending";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public String getMedicalCondition() { return medicalCondition; }
    public void setMedicalCondition(String medicalCondition) { this.medicalCondition = medicalCondition; }

    public String getPhotoProfil() { return photoProfil; }
    public void setPhotoProfil(String photoProfil) { this.photoProfil = photoProfil; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}