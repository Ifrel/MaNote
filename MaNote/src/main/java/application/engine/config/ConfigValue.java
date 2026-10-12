package application.engine.config;

import java.util.Objects;

/**
 * Valeur typée d'un paramètre de calcul.
 */
public sealed interface ConfigValue {

    /**
     * Valeur numérique.
     */
    record NumberValue(double value) implements ConfigValue {
        public NumberValue {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Valeur non finie");
        }
    }

    /**
     * Valeur booléenne (activé / désactivé).
     */
    record BooleanValue(boolean value) implements ConfigValue {
    }

    /**
     * Choix parmi une liste fournie par le moteur ({@link Choice#code()}).
     */
    record ChoiceValue(String code) implements ConfigValue {
        public ChoiceValue {
            Objects.requireNonNull(code, "code");
        }
    }

    /**
     * Texte libre.
     */
    record TextValue(String value) implements ConfigValue {
        public TextValue {
            value = value == null ? "" : value;
        }
    }

    static ConfigValue number(double v) {
        return new NumberValue(v);
    }

    static ConfigValue bool(boolean v) {
        return new BooleanValue(v);
    }

    static ConfigValue choice(String code) {
        return new ChoiceValue(code);
    }

    static ConfigValue text(String v) {
        return new TextValue(v);
    }
}
