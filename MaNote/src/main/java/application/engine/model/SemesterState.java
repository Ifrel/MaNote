package application.engine.model;

/**
 * Situation calendaire d'un semestre, déterminée par le moteur.
 */
public enum SemesterState {
    /**
     * Semestre à venir.
     */
    UPCOMING,
    /**
     * Semestre en cours.
     */
    IN_PROGRESS,
    /**
     * Semestre terminé.
     */
    COMPLETED
}
