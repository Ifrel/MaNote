package application.engine.model;

/**
 * Nature d'un résultat calculé : permet de distinguer une donnée réelle d'une estimation.
 */
public enum ResultNature {

    /**
     * Résultat définitif : toutes les données nécessaires sont disponibles.
     */
    FINAL,

    /**
     * Résultat provisoire : calculé sur les seules données disponibles, susceptible d'évoluer.
     */
    PROVISIONAL,

    /**
     * Résultat simulé : dépend d'hypothèses saisies par l'utilisateur, ce n'est pas un résultat obtenu.
     */
    SIMULATED
}
