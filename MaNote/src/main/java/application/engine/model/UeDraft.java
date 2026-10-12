package application.engine.model;

import java.util.Objects;

/**
 * Données saisies pour créer ou modifier une UE.
 *
 * @param semesterId  semestre de rattachement
 * @param code        code de l'UE
 * @param name        intitulé
 * @param credits     crédits ECTS (≥ 0)
 * @param description description facultative
 */
public record UeDraft(SemesterId semesterId, String code, String name, int credits, String description) {

    public UeDraft {
        Objects.requireNonNull(semesterId, "semesterId");
        code = code == null ? "" : code.strip();
        name = name == null ? "" : name.strip();
        description = description == null ? "" : description.strip();
    }
}
