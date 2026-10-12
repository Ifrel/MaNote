package domain.entities;

import jakarta.persistence.*;

import java.util.Date;

/**
 * Entité JPA représentant une Note / Évaluation universitaire.
 */
@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String nom;

    @Column
    private Double valeur;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

    @Column(nullable = false)
    private String type = "CC"; // QUIZ, TP, CC, PROJET, DEVOIR, EXAMEN, AUTRE

    @Column(nullable = false)
    private String status = "PLANNED"; // PLANNED, AWAITING_GRADE, GRADED, CANCELLED

    @Column(nullable = false)
    private double bareme = 20.0;

    @Column(nullable = false)
    private double coefficient = 1.0;

    @Column(name = "is_absent")
    private boolean isAbsent = false;

    @Column(name = "is_justified")
    private boolean isJustified = false;

    @Column(length = 1000)
    private String appreciation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ue_id")
    private UE ue;

    public Note(int id, String nom, double valeur, Date date) {
        this.id = id;
        this.nom = nom;
        this.valeur = valeur;
        this.date = date;
        this.status = "GRADED";
    }

    public Note(String nom, double valeur) {
        this.nom = nom;
        this.valeur = valeur;
        this.date = new Date();
        this.status = "GRADED";
    }

    public Note(String nom, String type, Date date, double bareme, double coefficient) {
        this.nom = nom;
        this.type = type;
        this.date = date;
        this.bareme = bareme;
        this.coefficient = coefficient;
        this.status = "PLANNED";
    }

    public Note() {
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Double getValeur() {
        return valeur;
    }

    public void setValeur(Double valeur) {
        this.valeur = valeur;
        if (valeur != null) {
            this.status = "GRADED";
            this.isAbsent = false;
        }
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getBareme() {
        return bareme;
    }

    public void setBareme(double bareme) {
        this.bareme = bareme;
    }

    public double getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(double coefficient) {
        this.coefficient = coefficient;
    }

    public double getPoids() {
        return coefficient;
    }

    public void setPoids(double poids) {
        this.coefficient = poids;
    }

    public boolean isAbsent() {
        return isAbsent;
    }

    public void setAbsent(boolean absent) {
        isAbsent = absent;
        if (absent) {
            this.valeur = null;
            this.status = "GRADED";
        }
    }

    public boolean isJustified() {
        return isJustified;
    }

    public void setJustified(boolean justified) {
        isJustified = justified;
    }

    public String getAppreciation() {
        return appreciation;
    }

    public void setAppreciation(String appreciation) {
        this.appreciation = appreciation;
    }

    public UE getUe() {
        return ue;
    }

    public void setUe(UE ue) {
        this.ue = ue;
    }

    @Override
    public String toString() {
        return nom + " : " + (valeur != null ? valeur : (isAbsent ? (isJustified ? "ABJ" : "ABI") : "Non notée")) + " (" + date + ")";
    }
}
