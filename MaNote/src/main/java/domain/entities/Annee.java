package domain.entities;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entité JPA représentant une Année Universitaire (ex: Master 1 Informatique).
 */
@Entity
@Table(name = "annees")
public class Annee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String nom;

    @OneToMany(mappedBy = "annee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Semestre> semestres = new ArrayList<>();

    public Annee(int id, String nom) {
        this.id = id;
        this.nom = nom;
    }

    public Annee(String nom) {
        this.nom = nom;
    }

    public Annee() {
    }

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

    public List<Semestre> getSemestres() {
        return semestres;
    }

    public void setSemestres(List<Semestre> semestres) {
        this.semestres = semestres;
    }

    public void addSemestre(Semestre semestre) {
        if (semestre != null) {
            semestres.add(semestre);
            semestre.setAnnee(this);
        }
    }

    public void removeSemestre(Semestre semestre) {
        if (semestre != null) {
            semestres.remove(semestre);
            semestre.setAnnee(null);
        }
    }
}
