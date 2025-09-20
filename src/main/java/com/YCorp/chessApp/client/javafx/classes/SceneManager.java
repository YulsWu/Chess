package com.YCorp.chessApp.client.javafx.classes;

import java.util.ArrayList;

import com.YCorp.chessApp.client.javafx.classes.interfaces.Closeable;
// Scene manager class to execute cleanup methods on all controllers
// Currently this can just be done with a list of controllers, but it may not be advisable
// to call cleanup() on inactive future scenes, depending on how they're built. In this case
// we would add a label to each scene and swap the active controller on each scene transition,
// and close only the active scene.
public class SceneManager {
    //private ArrayList<Closeable> controllers = new ArrayList<>();
    private Closeable activeController;

    public SceneManager(){};

    // public void addActiveController(Closeable controller){
    //     controllers.add(controller);
    //     activeController = controller;
    // }

    // public void addController(Closeable controller){
    //     controllers.add(controller);
    // }

    public void cleanup(){
        activeController.cleanup();
        // for (Closeable c : controllers){
        //     c.cleanup();
        // }
    }

    // public void replaceActive(Closeable controller){
    //     controllers.remove(this.activeController);
    //     addActiveController(controller);
    // }

    public void setActiveController(Closeable controller){
        this.activeController = controller;
    }

    public boolean isActive(){
        if (activeController != null){
            return true;
        }
        else{
            return false;
        }
    }
}