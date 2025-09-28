package com.YCorp.chessApp.client.javafx;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

import com.YCorp.chessApp.client.javafx.classes.GUIEngine;
import com.YCorp.chessApp.client.javafx.classes.SceneManager;
import com.YCorp.chessApp.client.javafx.control.BrowserSceneController;
import com.YCorp.chessApp.client.javafx.control.GameSceneController;
import com.YCorp.chessApp.client.javafx.control.MenuSceneController;
import com.YCorp.chessApp.client.javafx.control.SettingsSceneController;
import com.YCorp.chessApp.client.javafx.events.SceneTransitionEvent;

public class ScratchFX extends Application{
    private SceneManager sceneManager;
    private Stage stage;
    private int[] recentSettings;

    @Override
    public void start(Stage primaryStage) {
        
        // First time assignment of stage member
        stage = primaryStage;
        sceneManager = new SceneManager();
        primaryStage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, this::onWindowClose);
        primaryStage.addEventHandler(SceneTransitionEvent.ANY, this::sceneTransitionHandler);

        loadMenuScene();
        // // Load FXML
        // FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/gameScene.fxml"));

        // // Load root scene from FXML
        // Parent root = loader.load();

        // // Set controller reference
        // GameSceneController controller = loader.getController();

        // // Set root
        // primaryStage.setScene(new Scene(root));

        // // GUIENGINE INIT-------------------
        // // Create GUIEngine, set clock callback targets (board is already set as tick callback recipient)
        // GUIEngine guiEngine = new GUIEngine(0, 0, 30, 0, 100);
        // guiEngine.addTickCallbackTarget(controller);
        // guiEngine.addTimeoutCallbackTarget(controller);

        // // Set guiEngine as Controller's GUIEngine
        // controller.setGUIEngine(guiEngine);
        // controller.init(true);

        // sceneManager = new SceneManager();
        // sceneManager.addActiveController(controller);
        
        // Attach Window close handler to Stage
        // controller.restartProperty().addListener((obs, oldVal, newVal) -> {
        //     if (newVal) {
        //         loadGameScene();
        //     }
        // });
       
        return;

    }

    private void onWindowClose(WindowEvent e){
        sceneManager.cleanup();
    }

    private void loadGameScene(){
        if (this.recentSettings == null){
            System.out.println("No game settings found in " + Thread.currentThread().getStackTrace()[1].getMethodName());
            Platform.exit();
        }
        this.sceneManager.cleanup();
        // Recreate fxml loader, repeat every step


        // Load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/gameScene.fxml"));
        Parent root = new Pane(); // Dummy initial value to satisfy possible initialization error when setting the Stage's root
    
        try {
            root = loader.load();
            
            
        } catch (Exception e){
            System.out.println("Exception occurred in " + Thread.currentThread().getStackTrace()[1].getMethodName());
        }
        
        // Temporary scene object
        // Allows for controller.init() to get the scene reference from root node in order to add event filter
        // We do it like this since setting the stage before all nodes have finished being modified will cause pop-in for the graphics
        Scene tempScene = new Scene(root);

        // Set controller reference
        GameSceneController controller = loader.getController();
        
        
        // GUIENGINE INIT-------------------
        // Create GUIEngine, set clock callback targets (board is already set as tick callback recipient)
        GUIEngine guiEngine = new GUIEngine(this.recentSettings[2], this.recentSettings[3], this.recentSettings[4], this.recentSettings[5], this.recentSettings[6], this.recentSettings[7], this.recentSettings[8], this.recentSettings[9], 100);
        guiEngine.addTickCallbackTarget(controller);
        guiEngine.addTimeoutCallbackTarget(controller);
        
        // Set guiEngine as Controller's GUIEngine
        controller.setGUIEngine(guiEngine);
        controller.init(this.recentSettings[1] == 1 ? true : false, this.recentSettings[0] == 1 ? true : false);

        // Set root AFTER controller.init() since init() affects nodes
        this.stage.setScene(tempScene);
        
        sceneManager.setActiveController(controller);
        
        
        // Attach Window close handler to Stage
        
        // Load root scene from FXML
       

        stage.show();
    }

    private void loadMenuScene(){
        // Cleans up previous controller if there was one (going from game or replays back to menu)
        // Not sure this is the way I want to do this, but the Menu would be the only scene that can transition from
        // an inactive sceneManager (at startup), all other scenes transition from other active scenes so the sceneManager would always be active (and thus need cleanup())
        if (this.sceneManager.isActive()){
            this.sceneManager.cleanup();
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/menuScene.fxml"));

        try {
            Parent root = loader.load();
            this.stage.setScene(new Scene(root));
        }
        catch (Exception e){
            System.out.println("Exception occurred in " + Thread.currentThread().getStackTrace()[1].getMethodName());
        }

        MenuSceneController controller = loader.getController();
     

        sceneManager.setActiveController(controller);

        stage.show();

    };

    private void loadSettingsScene(){
        if (this.sceneManager.isActive()){
            this.sceneManager.cleanup();
        }

        SettingsSceneController controller = new SettingsSceneController();
        this.sceneManager.setActiveController(controller);
        stage.setScene(controller.getScene());
        stage.show();

    }

    private void loadBrowserScene(){
        if (this.sceneManager.isActive()){
            this.sceneManager.cleanup();
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/browserScene.fxml"));
        
        try {
            Parent root = loader.load();
            this.stage.setScene(new Scene(root));
        }
        catch (Exception e){
            e.printStackTrace();
        }
        
        BrowserSceneController controller = loader.getController();

        sceneManager.setActiveController(controller);
        controller.init();
        stage.show();
    }

    private void sceneTransitionHandler(SceneTransitionEvent e){
        System.out.println("Event caught: " + e.getEventType());
        if (e.getEventType() == SceneTransitionEvent.TO_GAME){
            int[] settings = e.getGameSettings();

            // If new settings are provided, replace recentSettings and load game, if not just re-load game with current settings
            if (settings != null){
                this.recentSettings = settings;
            }

            loadGameScene();
        }
        else if (e.getEventType() == SceneTransitionEvent.TO_SETTINGS){
            loadSettingsScene();
        }
        else if (e.getEventType() == SceneTransitionEvent.TO_MENU){
            loadMenuScene();
        }
        else if (e.getEventType() == SceneTransitionEvent.TO_BROWSER){
            loadBrowserScene();
        }
        // Prevent unintended propagation of event
        e.consume();
    }

    
}
