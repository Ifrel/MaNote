package application.engine.model;

/**
 * Identifiant typé d'une entité « Evaluation » échangé entre la présentation et le moteur.
 * <p>
 * La valeur reprend le type {@code int} des identifiants des entités du domaine
 * ({@code domain.entities}). Un identifiant typé évite de confondre, par exemple,
 * l'identifiant d'une UE avec celui d'une évaluation.
 *
 * @param value valeur brute de l'identifiant, attribuée par le moteur
 */
public record EvaluationId(int value) {

    @Override
    public String toString() {
        return "Evaluation#" + value;
    }
}
