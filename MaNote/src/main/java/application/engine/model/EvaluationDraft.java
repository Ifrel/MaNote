package application.engine.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Données saisies pour créer ou modifier une évaluation.
 * <p>
 * Une évaluation est toujours créée <em>sans</em> résultat : la note est enregistrée séparément
 * via {@code GradeEngine#recordGrade}. Le statut {@link EvaluationStatus#GRADED} n'est donc
 * accepté qu'à la modification d'une évaluation déjà notée.
 *
 * @param title       intitulé (obligatoire)
 * @param ueId        UE de rattachement
 * @param typeCode    code du type d'évaluation ({@link EvaluationType#code()})
 * @param date        date prévue ou effective (facultative)
 * @param status      statut souhaité
 * @param maxPoints   barème (strictement positif)
 * @param weight      coefficient ou poids, si le moteur le gère
 * @param description informations complémentaires (facultatives)
 */
public record EvaluationDraft(
        String title,
        UeId ueId,
        String typeCode,
        Optional<LocalDate> date,
        EvaluationStatus status,
        double maxPoints,
        OptionalDouble weight,
        String description) {

    public EvaluationDraft {
        title = title == null ? "" : title.strip();
        Objects.requireNonNull(ueId, "ueId");
        Objects.requireNonNull(typeCode, "typeCode");
        date = date == null ? Optional.empty() : date;
        Objects.requireNonNull(status, "status");
        weight = weight == null ? OptionalDouble.empty() : weight;
        description = description == null ? "" : description.strip();
    }
}
