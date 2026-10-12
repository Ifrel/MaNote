package domain.entities;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entité JPA représentant un Semestre universitaire (ex: Semestre 7 ou S1).
 */
@Entity
@Table(name = "semestres")
public class Semestre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private char num;

    @Column
    private String nom;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "annee_id")
    private Annee annee;

    @OneToMany(mappedBy = "semestre", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UE> ues = new ArrayList<>();

    public Semestre(int id, char num) {
        this.id = id;
        this.num = num;
    }

    public Semestre(char num) {
        this.num = num;
    }

    public Semestre(char num, String nom) {
        this.num = num;
        this.nom = nom;
    }

    public Semestre() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public char getNum() {
        return num;
    }

    public void setNum(char num) {
        this.num = num;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Annee getAnnee() {
        return annee;
    }

    public void setAnnee(Annee annee) {
        this.annee = annee;
    }

    public List<UE> getUes() {
        return ues;
    }

    public void setUes(List<UE> ues) {
        this.ues = ues;
    }

    public void addUe(UE ue) {
        if (ue != null) {
            ues.add(ue);
            ue.setSemestre(this);
        }
    }

    public void removeUe(UE ue) {
        if (ue != null) {
            ues.remove(ue);
            ue.setSemestre(null);
        }
    }
}
