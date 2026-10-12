package infrastructure.persistence;

import domain.entities.Annee;
import domain.entities.Note;
import domain.entities.Semestre;
import domain.entities.UE;

import java.util.Calendar;

/**
 * Initialisateur de données MaNote.
 * Ne charge aucune fausse donnée ni donnée préconfigurée.
 * Si l'étudiant a placé un fichier 'cursus.yaml' dans ~/.manote/, le synchronise automatiquement.
 * Sinon, la base reste vierge et prête pour les saisies de l'étudiant.
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initializeIfEmpty(JpaManager jpaManager) {
        CurriculumYamlService yamlService = new CurriculumYamlService(jpaManager);
        yamlService.createTemplateFileIfNotExists();

        // Si l'étudiant a défini un cursus.yaml, on synchronise
        if (yamlService.yamlFileExists()) {
            yamlService.syncIfModified();
        }
        // Sinon, la base de données reste totalement vierge (0 lignes).
    }

    /**
     * Méthode réservée exclusivement aux tests unitaires (base en mémoire H2 :memory:).
     */
    public static void seedDemoDataForTesting(JpaManager jpaManager) {
        jpaManager.executeInTransaction(em -> {
            Annee m1 = new Annee("Master 1 Informatique (2025-2026)");
            em.persist(m1);

            Semestre s7 = new Semestre('7', "Semestre 7 (M1 S1)");
            m1.addSemestre(s7);
            em.persist(s7);

            Semestre s8 = new Semestre('8', "Semestre 8 (M1 S2)");
            m1.addSemestre(s8);
            em.persist(s8);

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -10);

            UE ueCl = new UE("Conception Logicielle Avancée & Design Patterns", "INFO-M1-CL", 6);
            s7.addUe(ueCl);
            em.persist(ueCl);

            Note n1 = new Note("Quiz 1 : Principes SOLID", "QUIZ", cal.getTime(), 20.0, 0.3);
            n1.setValeur(17.0);
            ueCl.addNote(n1);
            em.persist(n1);

            cal.add(Calendar.DAY_OF_MONTH, 5);
            Note n2 = new Note("Projet : Application Desktop MaNote", "PROJET", cal.getTime(), 20.0, 0.4);
            n2.setStatus("AWAITING_GRADE");
            ueCl.addNote(n2);
            em.persist(n2);

            cal.add(Calendar.DAY_OF_MONTH, 15);
            Note n3 = new Note("Examen Terminal", "EXAMEN", cal.getTime(), 20.0, 0.3);
            n3.setStatus("PLANNED");
            ueCl.addNote(n3);
            em.persist(n3);
        });
    }
}
