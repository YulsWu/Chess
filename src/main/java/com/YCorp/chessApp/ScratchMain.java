package com.YCorp.chessApp;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.YCorp.chessApp.client.engine.ChessClock;
import com.YCorp.chessApp.client.javafx.ScratchFX;


public class ScratchMain {

    public static void main(String[] args){
        try{
            // Debug stream
            // PrintStream fileOut = new PrintStream(new FileOutputStream("fxdebug.txt"));
            // System.setErr(fileOut);
            ScratchFX.launch(ScratchFX.class, args);

        }
        catch (Exception e){
            e.printStackTrace();
        }
        

        // ChessClock clock = new ChessClock(0, 0, 20, 2000, 100);
        // long startTime;
        // long endTime;
        // clock.start();
        // startTime = System.nanoTime();
        // for (int i = 0; i < 5; i++){
        //     try{
        //         Thread.sleep(5000);
        //     }
        //     catch (Exception e){
        //         System.out.println(e);
        //         return;
        //     }
        //     System.out.println();
        //     if (clock.isRunning()){
        //         endTime = System.nanoTime();
        //         clock.toggle();
        //         System.out.println("\nLoop time elapsed: " + (startTime - endTime)/1000000);
        //         startTime = System.nanoTime();
                
        //     }
        //     else {
        //         System.out.println("Reached main else");
        //         return;
        //     }
        // }
        // clock.stop();


    }
}
