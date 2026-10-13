package api.views.widgets;

import application.engine.model.ComputedResult;
import application.engine.model.ValidationState;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;

import java.util.Locale;

/**
 * Anneau de progression affichant une moyenne (valeur / échelle).
 * <p>
 * Le composant ne fait que de la mise à l'échelle graphique : la couleur est dictée par
 * l'{@link ValidationState} fourni par le moteur, jamais par un seuil connu de l'interface.
 */
public class ScoreRing extends StackPane {

    private static final String[] TONES = {"tone-success", "tone-info", "tone-warning", "tone-danger", "tone-neutral"};

    private final Arc fill;
    private final Label valueLabel = new Label("—");
    private final Label captionLabel = new Label();

    public ScoreRing(double size, double stroke) {
        double center = size / 2;
        double radius = (size - stroke) / 2;

        Circle track = new Circle(center, center, radius);
        track.getStyleClass().add("ring-track");
        track.setStrokeWidth(stroke);

        fill = new Arc(center, center, radius, radius, 90, 0);
        fill.setType(ArcType.OPEN);
        fill.setStrokeWidth(stroke);
        fill.setStrokeLineCap(StrokeLineCap.ROUND);
        fill.getStyleClass().add("ring-fill");
        fill.setVisible(false);

        Pane canvas = new Pane(track, fill);
        canvas.setMinSize(size, size);
        canvas.setPrefSize(size, size);
        canvas.setMaxSize(size, size);

        valueLabel.getStyleClass().add("ring-value");
        valueLabel.setStyle("-fx-font-size: " + Math.round(size * 0.24) + "px; -fx-font-weight: bold;");
        captionLabel.getStyleClass().add("ring-caption");

        VBox labels = new VBox(0, valueLabel, captionLabel);
        labels.setAlignment(Pos.CENTER);
        labels.setMouseTransparent(true);

        setAlignment(Pos.CENTER);
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
        getChildren().addAll(canvas, labels);
    }

    /**
     * Classe CSS de teinte associée à un état de validation (même logique que les badges).
     */
    public static String tone(ValidationState state) {
        return switch (state) {
            case VALIDATED -> "tone-success";
            case COMPENSATED, IN_PROGRESS -> "tone-info";
            case AT_RISK -> "tone-warning";
            case NOT_VALIDATED -> "tone-danger";
            case NOT_PROVIDED -> "tone-neutral";
        };
    }

    public void setResult(ComputedResult result, ValidationState state) {
        fill.getStyleClass().removeAll(TONES);
        valueLabel.getStyleClass().removeAll(TONES);

        if (result instanceof ComputedResult.Value v) {
            double ratio = Math.max(0, Math.min(1, v.value() / v.outOf()));
            fill.setLength(-360 * ratio);
            fill.setVisible(ratio > 0);
            valueLabel.setText(String.format(Locale.FRENCH, "%.2f", v.value()));
            captionLabel.setText(switch (v.nature()) {
                case FINAL -> String.format(Locale.FRENCH, "sur %.0f", v.outOf());
                case PROVISIONAL -> "provisoire";
                case SIMULATED -> "simulé";
            });
            String tone = tone(state);
            fill.getStyleClass().add(tone);
            valueLabel.getStyleClass().add(tone);
        } else {
            fill.setVisible(false);
            valueLabel.setText("—");
            captionLabel.setText("sans note");
        }
    }
}
