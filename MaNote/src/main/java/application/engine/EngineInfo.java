package application.engine;

import java.util.Objects;

/**
 * Description du moteur branché, affichée dans l'interface.
 *
 * @param name          nom du moteur
 * @param version       version
 * @param description   description courte
 * @param demonstration {@code true} si le moteur fournit des données de démonstration : l'interface
 *                      l'indique alors clairement à l'utilisateur
 */
public record EngineInfo(String name, String version, String description, boolean demonstration) {

    public EngineInfo {
        Objects.requireNonNull(name, "name");
        version = version == null ? "" : version;
        description = description == null ? "" : description;
    }
}
