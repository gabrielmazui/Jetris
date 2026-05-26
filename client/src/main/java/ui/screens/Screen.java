package ui.screens;

import javafx.scene.Parent;

public interface Screen {
    Parent getRoot();
    default void RecconectHandler(){
        // menu de recconect com blur no menu atual
    }
}