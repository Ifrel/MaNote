package application.engine.model;

import java.util.Objects;

/**
 * Type (catégorie) d'évaluation : quiz, TP noté, contrôle continu, projet, examen…
 * <p>
 * Les types sont fournis par le moteur ({@code GradeEngine#getEvaluationTypes()}),
 * ce qui permet de les rendre configurables sans modifier l'interface.
 *
 * @param code  code stable (ex. {@code "EXAM"})
 * @param label libellé affiché (ex. « Examen »)
 */
public record EvaluationType(String code, String label) {

    public EvaluationType {
        Objects.requireNonNull(code, "code");
        label = label == null || label.isBlank() ? code : label;
    }
}
