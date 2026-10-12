package api.views.app;

import javax.swing.*;

public class BarreMenu extends JMenuBar {
    private JMenu jMenu1;
    private JMenu jMenu2;
    private JMenu jMenu3;

    public BarreMenu() {
        super();
        this.jMenu1 = new JMenu("Menu 1");
        this.jMenu2 = new JMenu("Menu 2");
        this.jMenu3 = new JMenu("Menu 3");

        jMenu1.add(new JMenuItem("Item 1"));

        add(jMenu1);
        add(jMenu2);
        add(jMenu3);

    }


}
