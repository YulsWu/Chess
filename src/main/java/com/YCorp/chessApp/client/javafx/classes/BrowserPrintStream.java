package com.YCorp.chessApp.client.javafx.classes;

import java.io.OutputStream;
import java.io.PrintStream;

import javafx.collections.ObservableList;

public class BrowserPrintStream extends PrintStream {

    ObservableList<String> browserConsole;
    
    public BrowserPrintStream(OutputStream out, ObservableList<String> browserConsole){
        super(out);
        this.browserConsole = browserConsole;
    }

    @Override
    public void println(String x){
        this.browserConsole.add(x);
        super.println(x);
    }
}
