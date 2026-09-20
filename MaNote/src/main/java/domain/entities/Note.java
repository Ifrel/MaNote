package domain.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Date;

@Entity
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nom;
    private double valeur;
    private Date date;

    public Note(String nom, double valeur) {
        this.nom = nom;
        this.valeur = valeur;
        this.date = new Date();
    }

    public Note() {
    }

    // Getter
    public String getNom() {
        return nom;
    }

    public double getValeur() {
        return valeur;
    }

    public Date getDate() {
        return date;
    }

    // Setter
    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setValeur(double valeur) {
        this.valeur = valeur;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String toString() {
        return nom + " : " + valeur + " (" + date + ")";
    }
}
