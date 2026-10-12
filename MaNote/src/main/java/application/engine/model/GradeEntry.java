package application.engine.model;

import java.util.Objects;

/**
 * Résultat obtenu à une évaluation, tel que saisi par l'étudiant.
 * <p>
 * Ce type scellé représente <strong>uniquement</strong> un résultat effectivement connu.
 * L'absence de résultat n'est jamais codée par une valeur magique ({@code 0}, {@code -1}…) :
 * elle est représentée par {@code Optional.empty()} associé à un {@link EvaluationStatus}
 * explicite ({@link EvaluationStatus#PLANNED} ou {@link EvaluationStatus#AWAITING_GRADE}).
 * <p>
 * La manière dont une absence est prise en compte dans un calcul (neutralisation, zéro,
 * blocage…) relève exclusivement du moteur.
 */
public sealed interface GradeEntry permits GradeEntry.Score, GradeEntry.Absence {

    /**
     * Note chiffrée exprimée dans le barème de l'évaluation.
     *
     * @param points points obtenus (fini, positif ou nul)
     * @param outOf  note maximale du barème (strictement positive)
     */
    record Score(double points, double outOf) implements GradeEntry {
        public Score {
            if (!Double.isFinite(points) || points < 0) {
                throw new IllegalArgumentException("La note doit être un nombre positif ou nul.");
            }
            if (!Double.isFinite(outOf) || outOf <= 0) {
                throw new IllegalArgumentException("Le barème doit être strictement positif.");
            }
        }
    }

    /**
     * L'étudiant était absent à l'évaluation.
     *
     * @param justified {@code true} pour une absence justifiée (ABJ), {@code false} sinon (ABI)
     */
    record Absence(boolean justified) implements GradeEntry {
    }

    /**
     * Crée une note chiffrée.
     */
    static GradeEntry score(double points, double outOf) {
        return new Score(points, outOf);
    }

    /**
     * Crée une absence justifiée ou non.
     */
    static GradeEntry absence(boolean justified) {
        return new Absence(justified);
    }

    /**
     * Vérifie qu'un résultat n'est pas {@code null} (utilitaire pour les constructeurs).
     */
    static GradeEntry require(GradeEntry entry) {
        return Objects.requireNonNull(entry, "grade");
    }
}
