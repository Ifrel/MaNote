package application.engine.error;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Les données transmises au moteur sont invalides ou incohérentes.
 * Les erreurs sont associées aux champs pour être affichées au bon endroit dans les formulaires.
 */
public class ValidationException extends EngineException {

    private final List<FieldError> errors;

    public ValidationException(List<FieldError> errors) {
        super(errors == null || errors.isEmpty()
                ? "Données invalides."
                : errors.stream().map(FieldError::message).collect(Collectors.joining(" ")));
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public ValidationException(String field, String message) {
        this(List.of(new FieldError(field, message)));
    }

    /**
     * @return erreurs détaillées par champ
     */
    public List<FieldError> errors() {
        return errors;
    }
}
