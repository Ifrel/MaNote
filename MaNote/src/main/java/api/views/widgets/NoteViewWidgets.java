package api.views.widgets;

import javax.swing.*;

public class NoteViewWidgets extends JPanel {
    private float note;

    public NoteViewWidgets(float note) {
        this.note = note;
    }

    private void build() {
        add(new JLabel(note + ""));
    }

    public void setNote(float note) {
        this.note = note;
    }

    public void update() {

    }
}
