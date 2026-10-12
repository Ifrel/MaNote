package application.engine.model;

import java.util.Objects;

/**
 * Référence légère vers une UE, incluse dans les résumés d'évaluation.
 *
 * @param id   identifiant de l'UE
 * @param code code de l'UE (ex. « INF701 »)
 * @param name intitulé de l'UE
 */
public record UeRef(UeId id, String code, String name) {

    public UeRef {
        Objects.requireNonNull(id, "id");
        code = code == null ? "" : code;
        name = name == null ? "" : name;
    }
}
