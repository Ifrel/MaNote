package application.engine.model;

import java.util.Collection;

/**
 * Décompte des évaluations par statut pour un périmètre (UE, semestre…).
 *
 * @param planned       évaluations prévues
 * @param awaitingGrade évaluations réalisées dont le résultat est attendu
 * @param graded        évaluations dont le résultat est disponible
 * @param cancelled     évaluations annulées
 */
public record EvaluationProgress(int planned, int awaitingGrade, int graded, int cancelled) {

    public EvaluationProgress {
        if (planned < 0 || awaitingGrade < 0 || graded < 0 || cancelled < 0) {
            throw new IllegalArgumentException("Les compteurs doivent être positifs");
        }
    }

    public static final EvaluationProgress EMPTY = new EvaluationProgress(0, 0, 0, 0);

    /**
     * Compte les statuts fournis (utilitaire pour les implémentations du moteur).
     */
    public static EvaluationProgress of(Collection<EvaluationStatus> statuses) {
        int p = 0, a = 0, g = 0, c = 0;
        for (EvaluationStatus s : statuses) {
            switch (s) {
                case PLANNED -> p++;
                case AWAITING_GRADE -> a++;
                case GRADED -> g++;
                case CANCELLED -> c++;
            }
        }
        return new EvaluationProgress(p, a, g, c);
    }

    /**
     * @return nombre d'évaluations actives (hors annulées)
     */
    public int total() {
        return planned + awaitingGrade + graded;
    }

    /**
     * @return nombre d'évaluations restant à réaliser ou à noter
     */
    public int remaining() {
        return planned + awaitingGrade;
    }

    /**
     * @return fraction (0..1) des évaluations actives disposant d'un résultat ; 0 si aucune évaluation
     */
    public double gradedRatio() {
        return total() == 0 ? 0 : (double) graded / total();
    }
}
