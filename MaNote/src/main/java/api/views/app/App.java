package api.views.app;

import api.views.screens.ScreenPrincipal;

import javax.swing.*;

public class App extends JFrame {
    ScreenPrincipal manPage;
    BarreMenu barreMenu;

    public App(String nom) {
        super(nom);
        manPage = new ScreenPrincipal();
        barreMenu = new BarreMenu();

        add(barreMenu);
    }
}
