package application.engine.config;

import application.engine.model.AcademicYearId;
import application.engine.model.SemesterId;
import application.engine.model.UeId;

import java.util.Objects;

/**
 * Périmètre auquel s'applique un ensemble de paramètres de calcul.
 * <p>
 * Le moteur indique, via {@code EngineCapabilities#configurationScopes()}, les types de périmètre qu'il
 * sait configurer. La logique d'héritage entre périmètres (global → année → semestre → UE) lui appartient.
 */
public sealed interface ConfigurationScope {

    /**
     * Types de périmètre.
     */
    enum Kind {GLOBAL, YEAR, SEMESTER, UE, EVALUATION_TYPE}

    /**
     * @return le type de ce périmètre
     */
    Kind kind();

    /**
     * Paramètres par défaut, valables partout.
     */
    record Global() implements ConfigurationScope {
        public Kind kind() {
            return Kind.GLOBAL;
        }
    }

    /**
     * Paramètres d'une année universitaire.
     */
    record Year(AcademicYearId id) implements ConfigurationScope {
        public Year {
            Objects.requireNonNull(id, "id");
        }

        public Kind kind() {
            return Kind.YEAR;
        }
    }

    /**
     * Paramètres d'un semestre.
     */
    record Semester(SemesterId id) implements ConfigurationScope {
        public Semester {
            Objects.requireNonNull(id, "id");
        }

        public Kind kind() {
            return Kind.SEMESTER;
        }
    }

    /**
     * Paramètres d'une UE.
     */
    record Ue(UeId id) implements ConfigurationScope {
        public Ue {
            Objects.requireNonNull(id, "id");
        }

        public Kind kind() {
            return Kind.UE;
        }
    }

    /**
     * Paramètres propres à une catégorie (type) d'évaluation.
     */
    record EvaluationType(String typeCode) implements ConfigurationScope {
        public EvaluationType {
            Objects.requireNonNull(typeCode, "typeCode");
        }

        public Kind kind() {
            return Kind.EVALUATION_TYPE;
        }
    }

    static ConfigurationScope global() {
        return new Global();
    }
}
