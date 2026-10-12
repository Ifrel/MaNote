package api.views.format;

import application.engine.model.ComputedResult;
import application.engine.model.EvaluationStatus;
import application.engine.model.GradeEntry;
import application.engine.model.ValidationState;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * Utilitaires de formatage pour l'interface graphique.
 * <p>
 * Centralise l'affichage des notes, barèmes, dates et statuts.
 * <strong>Garantit qu'une note absente ou non renseignée n'est jamais affichée comme un zéro.</strong>
 */
public final class Formatters {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter SHORT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM", Locale.FRENCH);

    private Formatters() {
    }

    /**
     * Formate une note obtenue et son barème.
     */
    public static String formatGrade(Optional<GradeEntry> grade, double maxPoints) {
        if (grade.isEmpty()) {
            return "—";
        }
        GradeEntry entry = grade.get();
        if (entry instanceof GradeEntry.Score score) {
            return String.format(Locale.FRENCH, "%.2f / %.0f", score.points(), score.outOf());
        } else if (entry instanceof GradeEntry.Absence ab) {
            return ab.justified() ? "Absence justifiée (ABJ)" : "Absence injustifiée (ABI)";
        }
        return "—";
    }

    /**
     * Formate un résultat calculé par le moteur (ex: moyenne).
     */
    public static String formatComputedResult(ComputedResult result) {
        if (result instanceof ComputedResult.Value val) {
            String suffix = switch (val.nature()) {
                case FINAL -> "";
                case PROVISIONAL -> " (prov.)";
                case SIMULATED -> " (simulé)";
            };
            return String.format(Locale.FRENCH, "%.2f / %.0f%s", val.value(), val.outOf(), suffix);
        } else if (result instanceof ComputedResult.NotComputable nc) {
            return nc.reason();
        } else if (result instanceof ComputedResult.NotProvided) {
            return "Non communiqué";
        } else if (result instanceof ComputedResult.Failed f) {
            return "Erreur : " + f.message();
        }
        return "—";
    }

    /**
     * Formate une date en français (ex: 12 nov. 2026).
     */
    public static String formatDate(Optional<LocalDate> date) {
        return date.map(d -> d.format(DATE_FORMATTER)).orElse("Date non fixée");
    }

    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "—";
    }

    public static String formatShortDate(LocalDate date) {
        return date != null ? date.format(SHORT_DATE_FORMATTER) : "—";
    }

    /**
     * Libellé en français pour un statut d'évaluation.
     */
    public static String formatEvaluationStatus(EvaluationStatus status) {
        return switch (status) {
            case PLANNED -> "Planifiée";
            case AWAITING_GRADE -> "Note en attente";
            case GRADED -> "Notée";
            case CANCELLED -> "Annulée";
        };
    }

    /**
     * Classe CSS pour le badge de statut d'évaluation.
     */
    public static String getEvaluationStatusBadgeClass(EvaluationStatus status) {
        return switch (status) {
            case PLANNED -> "badge-info";
            case AWAITING_GRADE -> "badge-warning";
            case GRADED -> "badge-success";
            case CANCELLED -> "badge-neutral";
        };
    }

    /**
     * Classe CSS pour le badge de validation académique.
     */
    public static String getValidationBadgeClass(ValidationState state) {
        return switch (state) {
            case VALIDATED -> "badge-success";
            case COMPENSATED -> "badge-info";
            case NOT_VALIDATED -> "badge-danger";
            case AT_RISK -> "badge-warning";
            case IN_PROGRESS -> "badge-info";
            case NOT_PROVIDED -> "badge-neutral";
        };
    }
}
