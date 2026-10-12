package application.engine.error;

/**
 * Le moteur ne prend pas en charge cette opération.
 * L'interface évite normalement de la proposer en consultant {@code EngineCapabilities}.
 */
public class OperationNotSupportedException extends EngineException {

    public OperationNotSupportedException(String operation) {
        super("Fonctionnalité non disponible avec le moteur actuel : " + operation + ".");
    }
}
