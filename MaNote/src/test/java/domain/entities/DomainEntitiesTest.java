package domain.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests des entités du domaine JPA")
class DomainEntitiesTest {

    @Test
    @DisplayName("Annee : instanciation, accesseurs et modificateurs")
    void testAnnee() {
        Annee annee = new Annee(1, "Master 1 Informatique");
        assertEquals(1, annee.getId());
        assertEquals("Master 1 Informatique", annee.getNom());

        annee.setId(2);
        annee.setNom("Master 2");
        assertEquals(2, annee.getId());
        assertEquals("Master 2", annee.getNom());

        Annee empty = new Annee();
        assertNull(empty.getNom());
    }

    @Test
    @DisplayName("Semestre : instanciation, accesseurs et modificateurs")
    void testSemestre() {
        Semestre s = new Semestre(1, '1');
        assertEquals(1, s.getId());
        assertEquals('1', s.getNum());

        s.setId(2);
        s.setNum('2');
        assertEquals(2, s.getId());
        assertEquals('2', s.getNum());

        Semestre s2 = new Semestre('3');
        assertEquals('3', s2.getNum());

        Semestre empty = new Semestre();
        assertEquals('\0', empty.getNum());
    }

    @Test
    @DisplayName("UE : instanciation, accesseurs et modificateurs")
    void testUe() {
        UE ue = new UE(10, "Conception Logicielle", "INFO-M1-CL", 6);
        assertEquals(10, ue.getId());
        assertEquals("Conception Logicielle", ue.getNom());
        assertEquals("INFO-M1-CL", ue.getCode());
        assertEquals(6, ue.getCredit());

        ue.setId(11);
        ue.setNom("Compilation");
        ue.setCode("INFO-M1-COMP");
        ue.setCredit(5);
        assertEquals(11, ue.getId());
        assertEquals("Compilation", ue.getNom());
        assertEquals("INFO-M1-COMP", ue.getCode());
        assertEquals(5, ue.getCredit());

        UE ueNomOnly = new UE("Bases de données");
        assertEquals("Bases de données", ueNomOnly.getNom());

        UE empty = new UE();
        assertNull(empty.getNom());
    }

    @Test
    @DisplayName("Note : instanciation, accesseurs, modificateurs et toString")
    void testNote() {
        Date now = new Date();
        Note note = new Note(100, "Examen Final", 16.5, now);
        assertEquals(100, note.getId());
        assertEquals("Examen Final", note.getNom());
        assertEquals(16.5, note.getValeur());
        assertEquals(now, note.getDate());

        note.setId(101);
        note.setNom("TP noté");
        note.setValeur(18.0);
        Date tomorrow = new Date(now.getTime() + 86400000L);
        note.setDate(tomorrow);
        assertEquals(101, note.getId());
        assertEquals("TP noté", note.getNom());
        assertEquals(18.0, note.getValeur());
        assertEquals(tomorrow, note.getDate());
        assertTrue(note.toString().contains("TP noté"));
        assertTrue(note.toString().contains("18.0"));

        Note noteSansId = new Note("Contrôle Continu", 14.0);
        assertEquals("Contrôle Continu", noteSansId.getNom());
        assertEquals(14.0, noteSansId.getValeur());
        assertNotNull(noteSansId.getDate());

        Note empty = new Note();
        assertNull(empty.getNom());
    }

    @Test
    @DisplayName("Relations et associations bidirectionnelles JPA")
    void testEntityRelations() {
        Annee annee = new Annee("Master 1 Informatique");
        Semestre sem = new Semestre('7', "Semestre 7");
        annee.addSemestre(sem);

        assertEquals(1, annee.getSemestres().size());
        assertEquals(annee, sem.getAnnee());

        UE ue = new UE("Conception Logicielle", "INFO-M1-CL", 6);
        sem.addUe(ue);

        assertEquals(1, sem.getUes().size());
        assertEquals(sem, ue.getSemestre());

        Note note = new Note("TP 1", "TP", new Date(), 20.0, 1.0);
        ue.addNote(note);

        assertEquals(1, ue.getNotes().size());
        assertEquals(ue, note.getUe());

        ue.removeNote(note);
        assertEquals(0, ue.getNotes().size());
        assertNull(note.getUe());

        sem.removeUe(ue);
        assertEquals(0, sem.getUes().size());
        assertNull(ue.getSemestre());

        annee.removeSemestre(sem);
        assertEquals(0, annee.getSemestres().size());
        assertNull(sem.getAnnee());
    }
}
