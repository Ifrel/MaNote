package application.engine.model;

/**
 * État de validation d'une UE, d'un semestre ou d'une année, tel que déterminé par le moteur.
 * <p>
 * L'interface n'interprète jamais elle-même les seuils de validation : elle affiche l'état fourni.
 */
public enum ValidationState {
    /**
     * Validé(e).
     */
    VALIDATED,
    /**
     * Validé(e) par compensation.
     */
    COMPENSATED,
    /**
     * Non validé(e) (résultat définitif).
     */
    NOT_VALIDATED,
    /**
     * En cours, mais la situation actuelle nécessite une attention particulière.
     */
    AT_RISK,
    /**
     * En cours, pas encore déterminable.
     */
    IN_PROGRESS,
    /**
     * Le moteur ne fournit pas d'information de validation.
     */
    NOT_PROVIDED
}
