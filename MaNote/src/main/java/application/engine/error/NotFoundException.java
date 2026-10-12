package application.engine.error;

/**
 * L'entité demandée n'existe pas (ou plus).
 */
public class NotFoundException extends EngineException {

    public NotFoundException(String what, Object id) {
        super(what + " introuvable (" + id + "). Elle a peut-être été supprimée.");
    }

    public NotFoundException(String message) {
        super(message);
    }
}
