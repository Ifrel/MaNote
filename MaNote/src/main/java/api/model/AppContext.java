package api.model;

import application.engine.model.AcademicYearSummary;
import application.engine.model.SemesterSummary;
import application.engine.model.UeId;
import infrastructure.preferences.UserPreferences;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Contexte applicatif partagé contenant l'état d'affichage et de navigation.
 * Utilise les propriétés JavaFX pour permettre des liaisons réactives.
 */
public class AppContext {

    public enum ToastType {
        SUCCESS, INFO, WARNING, ERROR
    }

    private final ObjectProperty<NavigationDestination> currentDestination =
            new SimpleObjectProperty<>(NavigationDestination.DASHBOARD);

    private final ObjectProperty<AcademicYearSummary> activeYear =
            new SimpleObjectProperty<>();

    private final ObjectProperty<SemesterSummary> activeSemester =
            new SimpleObjectProperty<>();

    private final ObjectProperty<UeId> selectedUeId =
            new SimpleObjectProperty<>();

    private final ObjectProperty<UserPreferences.ThemeMode> themeMode =
            new SimpleObjectProperty<>(UserPreferences.getTheme());

    private final StringProperty toastMessage = new SimpleStringProperty("");
    private final ObjectProperty<ToastType> toastType = new SimpleObjectProperty<>(ToastType.INFO);

    public ObjectProperty<NavigationDestination> currentDestinationProperty() {
        return currentDestination;
    }

    public NavigationDestination getCurrentDestination() {
        return currentDestination.get();
    }

    public void setCurrentDestination(NavigationDestination destination) {
        this.currentDestination.set(destination);
    }

    public ObjectProperty<AcademicYearSummary> activeYearProperty() {
        return activeYear;
    }

    public AcademicYearSummary getActiveYear() {
        return activeYear.get();
    }

    public void setActiveYear(AcademicYearSummary year) {
        this.activeYear.set(year);
        if (year != null) {
            UserPreferences.setActiveYearId(year.id().value());
        }
    }

    public ObjectProperty<SemesterSummary> activeSemesterProperty() {
        return activeSemester;
    }

    public SemesterSummary getActiveSemester() {
        return activeSemester.get();
    }

    public void setActiveSemester(SemesterSummary semester) {
        this.activeSemester.set(semester);
        if (semester != null) {
            UserPreferences.setActiveSemesterId(semester.id().value());
        }
    }

    public ObjectProperty<UeId> selectedUeIdProperty() {
        return selectedUeId;
    }

    public UeId getSelectedUeId() {
        return selectedUeId.get();
    }

    public void setSelectedUeId(UeId ueId) {
        this.selectedUeId.set(ueId);
    }

    public ObjectProperty<UserPreferences.ThemeMode> themeModeProperty() {
        return themeMode;
    }

    public UserPreferences.ThemeMode getThemeMode() {
        return themeMode.get();
    }

    public void setThemeMode(UserPreferences.ThemeMode mode) {
        this.themeMode.set(mode);
        UserPreferences.setTheme(mode);
    }

    public StringProperty toastMessageProperty() {
        return toastMessage;
    }

    public ObjectProperty<ToastType> toastTypeProperty() {
        return toastType;
    }

    public void showToast(String message, ToastType type) {
        this.toastType.set(type);
        this.toastMessage.set(message);
    }
}
