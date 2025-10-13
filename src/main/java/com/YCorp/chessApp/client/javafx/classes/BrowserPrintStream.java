package com.YCorp.chessApp.client.javafx.classes;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import javafx.application.Platform;
import javafx.collections.ObservableList;

public class BrowserPrintStream extends PrintStream {

    private final ObservableList<String> browserConsole;
    private final OutputStream original;
    private final StringBuilder buffer = new StringBuilder();
    private int cursor = 0;
    private boolean carriageReturn = false;


    public BrowserPrintStream(OutputStream out, ObservableList<String> browserConsole){
        super(out, true);
        this.browserConsole = browserConsole;
        this.original = out;
    }

    // @Override
    // public void write(byte[] buf, int off, int len){
    //     // Forward to real console
    //     try{
    //         original.write(buf, off, len);
    //     }
    //     catch (Exception e){
    //         e.printStackTrace();
    //     }

    //     String text = new String(buf, off, len, StandardCharsets.UTF_8);

    //     for (char c : text.toCharArray()) {
    //         int lastInd = browserConsole.size() == 0 ? 0 : browserConsole.size() - 1;

    //         if (c == '\n') {
    //             if (carriageReturn && !browserConsole.isEmpty()){
    //                 Platform.runLater(() -> browserConsole.set(lastInd, buffer.toString()));
    //                 carriageReturn = false;
                  
    //             }
    //             else{
    //                 Platform.runLater(() -> browserConsole.add(buffer.toString()));
                  
    //             }
    //             buffer.setLength(0);
    //             cursor = 0;

    //         } else if (c == '\r') {
    //             if (carriageReturn && !browserConsole.isEmpty()){
    //                 Platform.runLater(() -> browserConsole.set(lastInd, buffer.toString()));
          
    //             }
    //             else {
    //                 Platform.runLater(() -> browserConsole.add(buffer.toString()));
                   
    //             }
    //             cursor = 0; // reset cursor to beginning of line
    //             carriageReturn = true;

    //         } else {
    //             // overwrite or append at cursor
    //             if (cursor < buffer.length()) {
    //                 buffer.setCharAt(cursor, c);
    //             } else {
    //                 buffer.append(c);
    //             }
    //             cursor++;
    //         }
    //     }
    // }

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
