package domain.entities;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entité JPA représentant une Unité d'Enseignement (UE).
 */
@Entity
@Table(name = "ues")
public class UE {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private int credit;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "semestre_id")
    private Semestre semestre;

    @OneToMany(mappedBy = "ue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Note> notes = new ArrayList<>();

    public UE(int id, String nom, String code, int credit) {
        this.id = id;
        this.nom = nom;
        this.code = code;
        this.credit = credit;
    }

    public UE(String nom, String code, int credit) {
        this.nom = nom;
        this.code = code;
        this.credit = credit;
    }

    public UE(String nom) {
        this.nom = nom;
    }

    public UE() {
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getCredit() {
        return credit;
    }

    public void setCredit(int credit) {
        this.credit = credit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Semestre getSemestre() {
        return semestre;
    }

    public void setSemestre(Semestre semestre) {
        this.semestre = semestre;
    }

    public List<Note> getNotes() {
        return notes;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
    }

    public void addNote(Note note) {
        if (note != null) {
            notes.add(note);
            note.setUe(this);
        }
    }

    public void removeNote(Note note) {
        if (note != null) {
            notes.remove(note);
            note.setUe(null);
        }
    }
}
