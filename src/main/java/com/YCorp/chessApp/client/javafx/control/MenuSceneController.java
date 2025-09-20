package com.YCorp.chessApp.client.javafx.control;

import com.YCorp.chessApp.client.javafx.classes.interfaces.Closeable;
import com.YCorp.chessApp.client.javafx.events.SceneTransitionEvent;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

// Implementing closable so that it is grouped in with controllers
// Currently nothing to "close" on this scene
public class MenuSceneController implements Closeable{
    @FXML
    Pane menuPane;
    @FXML
    Button playButton;
    @FXML
    Button replayButton;
    @FXML
    Button exitButton;
    @FXML
    ImageView menuImage;

    Pane dummy;
    
    private Image mate = new Image(getClass().getResource("/graphics/menu/mate.png").toExternalForm());

    @FXML
    private void initialize(){
        menuImage.setImage(mate);
        playButton.addEventHandler(ActionEvent.ACTION, this::playButtonHandler);
        exitButton.addEventHandler(ActionEvent.ACTION, this::exitButtonHandler);

        menuImage.setLayoutX(menuPane.getPrefWidth()/2 - menuImage.getBoundsInParent().getWidth()/2);
        menuImage.setLayoutY(menuPane.getPrefHeight()/4 - menuImage.getBoundsInParent().getHeight()/3);

        dummy = new Pane();
        dummy.setVisible(false);
        dummy.setMouseTransparent(true);
        menuPane.getChildren().add(dummy);
    }

    private void exitButtonHandler(ActionEvent e){
        Platform.exit();
    }

    private void playButtonHandler(ActionEvent e){
        System.out.println("Play button pressed");
        dummy.fireEvent(new SceneTransitionEvent(SceneTransitionEvent.TO_SETTINGS));
    }

    public void cleanup(){

    }
}
