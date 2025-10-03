package com.YCorp.chessApp.client.javafx.classes;

import java.io.OutputStream;
import java.io.PrintStream;

import javafx.application.Platform;
import javafx.collections.ObservableList;

public class BrowserPrintStream extends PrintStream {

    ObservableList<String> browserConsole;
    
    public BrowserPrintStream(OutputStream out, ObservableList<String> browserConsole){
        super(out);
        this.browserConsole = browserConsole;
    }

    @Override
    public void println(String x){
        Platform.runLater(() -> {
            this.browserConsole.add(x);
        });
        super.println(x);
    }

    @Override 
    public void print(String x){
        Platform.runLater(() -> {
            if (this.browserConsole.size() != 0){
                this.browserConsole.set(this.browserConsole.size() - 1, x);
            }
            else {
                this.browserConsole.set(this.browserConsole.size(), x);
            }
        });
        super.print(x);
    }
}
