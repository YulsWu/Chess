package com.YCorp.chessApp.client.javafx.classes;



import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class SceneFactory {
    //Non-fxml nodes
     
    public static Scene buildSettingsScene(){
        Pane settingsCover;
        VBox settingsPanel;
        RadioButton whitePlayerRadio;
        RadioButton blackPlayerRadio;
        RadioButton vsRadio;
        RadioButton freePlayRadio;
        ToggleGroup playerToggleGroup;
        ToggleGroup gameTypeToggleGroup;
        TextField whiteHours;
        TextField whiteMinutes;
        TextField whiteSeconds;
        TextField blackHours;
        TextField blackMinutes;
        TextField blackSeconds;
        TextField whiteIncrement;
        TextField blackIncrement;
        Label settingsTitle;
        Button playButton;
        Button cancelButton;
        HBox playerRadioBox;
        HBox gameTypeRadioBox;
        HBox whiteTimeBox;
        HBox blackTimeBox;
        HBox buttonHbox;
        Pane dummy;

        // Setup root node
        settingsCover = new Pane();
        settingsCover.setPrefSize(960, 540);
        settingsCover.setBackground(new Background(new BackgroundFill(Color.web("#808080", 0.8), CornerRadii.EMPTY, Insets.EMPTY)));
        
        // Setup dummy node for firing events into scene graph
        dummy = new Pane();
        dummy.setVisible(false);
        dummy.setMouseTransparent(true);
        settingsCover.getChildren().add(dummy);

        // Setup opaque settings panel
        settingsPanel = new VBox();
        settingsCover.getChildren().add(settingsPanel);
        settingsPanel.setPrefSize(settingsCover.getPrefWidth()/3, settingsCover.getPrefHeight()/3);
        settingsPanel.setLayoutX(settingsCover.getPrefWidth()/2 - settingsPanel.getPrefWidth()/2);
        settingsPanel.setLayoutY(settingsCover.getPrefHeight()/2 - settingsPanel.getPrefHeight()/2);
        settingsPanel.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

        // Title
        settingsTitle = new Label("Game settings");
        settingsPanel.getChildren().add(settingsTitle);
        settingsPanel.setAlignment(Pos.CENTER);

        // Player radio buttons
        whitePlayerRadio = new RadioButton("White");
        whitePlayerRadio.setSelected(true); // Default selection, avoids checking for null in toggle group
        blackPlayerRadio = new RadioButton("Black");
        playerToggleGroup = new ToggleGroup();
        whitePlayerRadio.setToggleGroup(playerToggleGroup);
        blackPlayerRadio.setToggleGroup(playerToggleGroup);
        // Radio button toggle group
        playerRadioBox = new HBox();
        playerRadioBox.setAlignment(Pos.CENTER);
        playerRadioBox.getChildren().addAll(whitePlayerRadio, blackPlayerRadio);

        // Game type radio buttons
        vsRadio = new RadioButton("vs");
        freePlayRadio = new RadioButton("freeplay");
        freePlayRadio.setSelected(true);
        gameTypeToggleGroup = new ToggleGroup();
        vsRadio.setToggleGroup(gameTypeToggleGroup);
        freePlayRadio.setToggleGroup(gameTypeToggleGroup);
        gameTypeRadioBox = new HBox();
        gameTypeRadioBox.setAlignment(Pos.CENTER);
        gameTypeRadioBox.getChildren().addAll(vsRadio, freePlayRadio);
        
        // Player time settings
        whiteHours = new TextField();
        whiteMinutes = new TextField();
        whiteSeconds = new TextField();
        whiteIncrement = new TextField();
        blackHours = new TextField();
        blackMinutes = new TextField();
        blackSeconds = new TextField();
        blackIncrement = new TextField();

        // Align textfields in hbox, in parent Vbox
        whiteTimeBox = new HBox();
        whiteTimeBox.setAlignment(Pos.CENTER);
        whiteTimeBox.getChildren().addAll(whiteHours, whiteMinutes, whiteSeconds, whiteIncrement);
        blackTimeBox = new HBox();
        blackTimeBox.setAlignment(Pos.CENTER);
        blackTimeBox.getChildren().addAll(blackHours, blackMinutes, blackSeconds, blackIncrement);

        // Play and cancel buttons
        playButton = new Button("Play");
        cancelButton = new Button("Cancel");
        
        // Align buttons
        buttonHbox = new HBox();
        buttonHbox.getChildren().addAll(playButton, cancelButton);
        buttonHbox.setAlignment(Pos.CENTER);

        // Add aligned children to panel
        settingsPanel.getChildren().addAll(gameTypeRadioBox, playerRadioBox, whiteTimeBox, blackTimeBox, buttonHbox);

        // Set IDs for relevant nodes
        whitePlayerRadio.setId("whitePlayerRadio");
        vsRadio.setId("vsRadio");
        whiteHours.setId("whiteHours");
        whiteMinutes.setId("whiteMinutes");
        whiteSeconds.setId("whiteSeconds");
        blackHours.setId("blackHours");
        blackMinutes.setId("blackMinutes");
        blackSeconds.setId("blackSeconds");
        whiteIncrement.setId("whiteIncrement");
        blackIncrement.setId("blackIncrement");
        playButton.setId("playButton");
        cancelButton.setId("cancelButton");
        dummy.setId("dummy");


        return new Scene(settingsCover);
    }

}


