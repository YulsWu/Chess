package com.YCorp.chessApp.client.javafx.control;
import java.util.Arrays;

import com.YCorp.chessApp.client.javafx.classes.SceneBuilder;
import com.YCorp.chessApp.client.javafx.classes.interfaces.Closeable;
import com.YCorp.chessApp.client.javafx.events.SceneTransitionEvent;

import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

public class SettingsSceneController implements Closeable {
    private Scene settingsScene;

    RadioButton whitePlayerRadio;
    ToggleGroup playerToggleGroup;
    TextField whiteHours;
    TextField whiteMinutes;
    TextField whiteSeconds;
    TextField blackHours;
    TextField blackMinutes;
    TextField blackSeconds;
    TextField whiteIncrement;
    TextField blackIncrement;
    Button playButton;
    Button cancelButton;
    Pane dummy;

    public SettingsSceneController(){
        settingsScene = SceneBuilder.buildSettingsScene();
        Parent root = settingsScene.getRoot();

        whitePlayerRadio = (RadioButton) root.lookup("#whitePlayerRadio");
        whiteHours = (TextField) root.lookup("#whiteHours");
        whiteMinutes = (TextField) root.lookup("#whiteMinutes");
        whiteSeconds = (TextField) root.lookup("#whiteSeconds");
        whiteIncrement = (TextField) root.lookup("#whiteIncrement");

        blackHours = (TextField) root.lookup("#blackHours");
        blackMinutes = (TextField) root.lookup("#blackMinutes");
        blackSeconds = (TextField) root.lookup("#blackSeconds");
        blackIncrement = (TextField) root.lookup("#blackIncrement");

        playButton = (Button) root.lookup("#playButton");
        cancelButton = (Button) root.lookup("#cancelButton");

        dummy = (Pane) root.lookup("#dummy");


        playButton.addEventHandler(ActionEvent.ACTION, this::settingPlayButtonHandler);
        cancelButton.addEventHandler(ActionEvent.ACTION, this::cancelButtonHandler);
    };


    private void cancelButtonHandler(ActionEvent e){
        dummy.fireEvent(new SceneTransitionEvent(SceneTransitionEvent.TO_MENU));
    }

    public void settingPlayButtonHandler(ActionEvent e){
        TextField[] fields = new TextField[]{whiteHours, whiteMinutes, whiteSeconds, whiteIncrement, blackHours, blackMinutes, blackSeconds, blackIncrement};

        // Sanitize fields for digits
        for (TextField n : fields){
            char[] temp = n.getText().toCharArray();
            for (int i = 0; i < temp.length; i++){
                // If field is empty, this won't execute
                if (!Character.isDigit(temp[i])){
                    // Highlight red and return out of handler to try again
                    n.setStyle("-fx-control-inner-background: pink; -fx-text-fill: red;");
                    return;
                }
            }
        }

        boolean noWhiteTime = (whiteHours.getText() == "" && whiteMinutes.getText() == "" && whiteSeconds.getText() == "") ? true : false;
        boolean noBlackTime = (blackHours.getText() == "" && blackMinutes.getText() == "" && blackSeconds.getText() == "") ? true : false;

        if (noWhiteTime || noBlackTime){
            if (noWhiteTime){
                for (TextField n : new TextField[]{whiteHours, whiteMinutes, whiteSeconds}){
                    n.setStyle("-fx-control-inner-background: pink; -fx-text-fill: red;");
                }
            }
            if (noBlackTime){
                for (TextField n : new TextField[]{blackHours, blackMinutes, blackSeconds}){
                    n.setStyle("-fx-control-inner-background: pink; -fx-text-fill: red;");
                }
            }
            return;
        }

        int[] finalSettings = new int[9];
        TextField[] timeFields = new TextField[] {whiteHours, whiteMinutes, whiteSeconds, whiteIncrement, blackHours, blackMinutes, blackSeconds, blackIncrement};
        
        for (int i = 0; i < timeFields.length; i++){
            if (timeFields[i].getText() == ""){
                finalSettings[i + 1] = 0;
            }
            else {
                finalSettings[i + 1] = Integer.valueOf(timeFields[i].getText());
            }
        }

        // int wh = Integer.valueOf(whiteHours.getText());
        // int wm = Integer.valueOf(whiteMinutes.getText());
        // int ws = Integer.valueOf(whiteSeconds.getText());
        // int wi = Integer.valueOf(whiteIncrement.getText());

        // int bh = Integer.valueOf(blackHours.getText());
        // int bm = Integer.valueOf(blackMinutes.getText());
        // int bs = Integer.valueOf(blackSeconds.getText());
        // int bi = Integer.valueOf(blackIncrement.getText());

        if (whitePlayerRadio.isSelected()){
            finalSettings[0] = 1;
        }
        else {
            finalSettings[0] = 0;
        }

        // Fire FX event with payload to trigger scene transition on Stage
        dummy.fireEvent(new SceneTransitionEvent(SceneTransitionEvent.TO_GAME, finalSettings));

        //Debug
        System.out.println("Game settings: " + Arrays.toString(finalSettings));

    }
        
    public Scene getScene(){
        // Return reference to true scene, copying would result in the stage displaying a different scene than is being controlled
        return settingsScene;
    }


    public void cleanup(){};
}
