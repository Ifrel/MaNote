package api.model;

/**
 * Destinations de navigation de l'application MaNote.
 */
public enum NavigationDestination {
    DASHBOARD("Tableau de bord", "HOME"),
    SEMESTERS("Semestres & Années", "CALENDAR"),
    UE_DETAILS("Détail d'une UE", "BOOK_OPEN"),
    EVALUATIONS("Gestion des évaluations", "LIST"),
    ANALYTICS("Analyse & Progression", "BAR_CHART_2"),
    PLANNING("Planification & Échéances", "CLOCK"),
    ENGINE_CONFIG("Paramétrage du calcul", "SLIDERS"),
    SETTINGS("Paramètres généraux", "SETTINGS");

    private final String title;
    private final String iconName;

    NavigationDestination(String title, String iconName) {
        this.title = title;
        this.iconName = iconName;
    }

    public String getTitle() {
        return title;
    }

    public String getIconName() {
        return iconName;
    }
}
