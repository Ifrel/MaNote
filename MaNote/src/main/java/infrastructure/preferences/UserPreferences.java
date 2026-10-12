package infrastructure.preferences;

import java.util.prefs.Preferences;

/**
 * Gestionnaire des préférences utilisateur persistées (thème visuel, confirmations, etc.).
 * <p>
 * Utilise {@link Preferences} de l'API standard Java pour une persistance transparente
 * entre les redémarrages de l'application desktop sans nécessiter de base de données.
 */
public final class UserPreferences {

    public enum ThemeMode {
        LIGHT, DARK, SYSTEM
    }

    private static final Preferences PREFS = Preferences.userNodeForPackage(UserPreferences.class);

    private static final String KEY_THEME = "ui.theme";
    private static final String KEY_ACTIVE_YEAR_ID = "app.active_year_id";
    private static final String KEY_ACTIVE_SEMESTER_ID = "app.active_semester_id";
    private static final String KEY_CONFIRM_DELETE = "app.confirm_delete";

    private UserPreferences() {
    }

    public static ThemeMode getTheme() {
        String val = PREFS.get(KEY_THEME, ThemeMode.LIGHT.name());
        try {
            return ThemeMode.valueOf(val);
        } catch (IllegalArgumentException e) {
            return ThemeMode.LIGHT;
        }
    }

    public static void setTheme(ThemeMode theme) {
        if (theme != null) {
            PREFS.put(KEY_THEME, theme.name());
        }
    }

    public static int getActiveYearId(int defaultValue) {
        return PREFS.getInt(KEY_ACTIVE_YEAR_ID, defaultValue);
    }

    public static void setActiveYearId(int yearId) {
        PREFS.putInt(KEY_ACTIVE_YEAR_ID, yearId);
    }

    public static int getActiveSemesterId(int defaultValue) {
        return PREFS.getInt(KEY_ACTIVE_SEMESTER_ID, defaultValue);
    }

    public static void setActiveSemesterId(int semesterId) {
        PREFS.putInt(KEY_ACTIVE_SEMESTER_ID, semesterId);
    }

    public static boolean isConfirmDeleteEnabled() {
        return PREFS.getBoolean(KEY_CONFIRM_DELETE, true);
    }

    public static void setConfirmDeleteEnabled(boolean enabled) {
        PREFS.putBoolean(KEY_CONFIRM_DELETE, enabled);
    }
}
