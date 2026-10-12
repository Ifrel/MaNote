package application.engine.error;

/**
 * Erreur signalée par le moteur. Le message doit être compréhensible par l'utilisateur final
 * (il est affiché tel quel par l'interface).
 */
public class EngineException extends RuntimeException {

    public EngineException(String message) {
        super(message);
    }

    public EngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
