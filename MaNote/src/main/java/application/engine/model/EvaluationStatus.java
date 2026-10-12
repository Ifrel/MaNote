package application.engine.model;

/**
 * Cycle de vie d'une évaluation.
 * <p>
 * Invariant du contrat : une évaluation possède un résultat ({@link GradeEntry})
 * <strong>si et seulement si</strong> son statut est {@link #GRADED}.
 */
public enum EvaluationStatus {

    /**
     * Évaluation prévue, pas encore réalisée.
     */
    PLANNED,

    /**
     * Évaluation réalisée, résultat pas encore connu.
     */
    AWAITING_GRADE,

    /**
     * Résultat disponible (note chiffrée ou absence enregistrée).
     */
    GRADED,

    /**
     * Évaluation annulée ou abandonnée.
     */
    CANCELLED;

    /**
     * @return {@code true} si l'évaluation reste à réaliser ou à noter
     */
    public boolean isOpen() {
        return this == PLANNED || this == AWAITING_GRADE;
    }
}
