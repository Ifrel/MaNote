package application.engine.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Point de l'évolution d'un résultat dans le temps (ex. moyenne provisoire du semestre après chaque note).
 *
 * @param date  date du point
 * @param label libellé de l'événement à l'origine du point (ex. intitulé de l'évaluation notée)
 * @param value résultat calculé par le moteur à cette date
 */
public record TimelinePoint(LocalDate date, String label, ComputedResult value) {

    public TimelinePoint {
        Objects.requireNonNull(date, "date");
        label = label == null ? "" : label;
        Objects.requireNonNull(value, "value");
    }
}
