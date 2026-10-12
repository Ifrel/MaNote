package application.engine.model;

import java.time.LocalDate;
import java.util.*;

/**
 * Critères de recherche des évaluations. Tous les critères sont facultatifs et cumulatifs.
 *
 * @param semesterId semestre
 * @param ueId       UE
 * @param typeCode   type d'évaluation
 * @param statuses   statuts acceptés (vide = tous)
 * @param from       date minimale incluse (les évaluations sans date sont alors exclues)
 * @param to         date maximale incluse (les évaluations sans date sont alors exclues)
 * @param text       texte recherché dans l'intitulé, le type et l'UE (insensible à la casse)
 */
public record EvaluationFilter(
        Optional<SemesterId> semesterId,
        Optional<UeId> ueId,
        Optional<String> typeCode,
        Set<EvaluationStatus> statuses,
        Optional<LocalDate> from,
        Optional<LocalDate> to,
        String text) {

    public EvaluationFilter {
        semesterId = semesterId == null ? Optional.empty() : semesterId;
        ueId = ueId == null ? Optional.empty() : ueId;
        typeCode = typeCode == null ? Optional.empty() : typeCode;
        statuses = statuses == null || statuses.isEmpty() ? Set.of() : Set.copyOf(statuses);
        from = from == null ? Optional.empty() : from;
        to = to == null ? Optional.empty() : to;
        text = text == null ? "" : text.strip();
    }

    /**
     * Filtre sans aucun critère.
     */
    public static EvaluationFilter all() {
        return new EvaluationFilter(Optional.empty(), Optional.empty(), Optional.empty(), Set.of(),
                Optional.empty(), Optional.empty(), "");
    }

    public EvaluationFilter withSemester(SemesterId id) {
        return new EvaluationFilter(Optional.ofNullable(id), ueId, typeCode, statuses, from, to, text);
    }

    public EvaluationFilter withUe(UeId id) {
        return new EvaluationFilter(semesterId, Optional.ofNullable(id), typeCode, statuses, from, to, text);
    }

    public EvaluationFilter withType(String code) {
        return new EvaluationFilter(semesterId, ueId, Optional.ofNullable(code), statuses, from, to, text);
    }

    public EvaluationFilter withStatuses(Set<EvaluationStatus> accepted) {
        return new EvaluationFilter(semesterId, ueId, typeCode, accepted, from, to, text);
    }

    public EvaluationFilter withStatus(EvaluationStatus first, EvaluationStatus... others) {
        return withStatuses(EnumSet.of(first, others));
    }

    public EvaluationFilter between(LocalDate start, LocalDate end) {
        return new EvaluationFilter(semesterId, ueId, typeCode, statuses, Optional.ofNullable(start),
                Optional.ofNullable(end), text);
    }

    public EvaluationFilter withText(String query) {
        return new EvaluationFilter(semesterId, ueId, typeCode, statuses, from, to, query);
    }

    /**
     * Indique si une évaluation satisfait ce filtre. Utilitaire mis à disposition des implémentations
     * du moteur (aucune règle académique n'intervient ici).
     */
    public boolean matches(EvaluationSummary e) {
        Objects.requireNonNull(e, "e");
        if (semesterId.isPresent() && !semesterId.get().equals(e.semesterId())) return false;
        if (ueId.isPresent() && !ueId.get().equals(e.ue().id())) return false;
        if (typeCode.isPresent() && !typeCode.get().equals(e.type().code())) return false;
        if (!statuses.isEmpty() && !statuses.contains(e.status())) return false;
        if (from.isPresent() && (e.date().isEmpty() || e.date().get().isBefore(from.get()))) return false;
        if (to.isPresent() && (e.date().isEmpty() || e.date().get().isAfter(to.get()))) return false;
        if (!text.isEmpty()) {
            String q = text.toLowerCase(Locale.ROOT);
            String haystack = (e.title() + " " + e.type().label() + " " + e.ue().code() + " " + e.ue().name())
                    .toLowerCase(Locale.ROOT);
            return haystack.contains(q);
        }
        return true;
    }
}
