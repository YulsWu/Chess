package com.YCorp.chessApp.client.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.YCorp.chessApp.client.parser.UciParser;

public class StockfishClient {

    private Process stockfish;
    private ProcessBuilder fishstock;
    private BufferedReader reader;
    private BufferedWriter writer;

    private int targetElo;


    public StockfishClient(){
        // Path jarDir = Paths.get(StockfishClient.class
        //         .getProtectionDomain()
        //         .getCodeSource()
        //         .getLocation()
        //         .toURI())
        //     .getParent();
    
        // private final Path ENGINE_PATH = jarDir.resolve("stockfish/stockfish-windows-x86-64-avx2.exe");
    
        // ProcessBuilder pb = new ProcessBuilder(ENGINE_PATH.toAbsolutePath().toString()).redirectErrorStream(true);

        String ENGINE_PATH = "C:\\Users\\yulun\\AppData\\Local\\Programs\\Java\\Chess\\dist\\stockfish\\stockfish-windows-x86-64-avx2.exe";
        this.fishstock = new ProcessBuilder(ENGINE_PATH).redirectErrorStream(true);

        
        try {
            this.stockfish = fishstock.start();
            this.reader = new BufferedReader(new InputStreamReader(stockfish.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(stockfish.getOutputStream()));
            
            System.out.println("Sending uci command");
            sendCommand("uci");
            
            
            Thread.sleep(50);

            while (!readUciOk()){
                this.stockfish.destroyForcibly();
                System.out.println("No UciOk recieved, spawning new process");
                this.stockfish = fishstock.start();
                this.reader = new BufferedReader(new InputStreamReader(stockfish.getInputStream()));
                this.writer = new BufferedWriter(new OutputStreamWriter(stockfish.getOutputStream()));

                sendCommand("uci");
            }
            System.out.println("StockfishClient(): uciok");
        }
        catch (Exception e){
            System.out.println("StockfishClient(): " + e);
        }

        sendCommand("setoption name UCI_LimitStrength value true");
        sendCommand("setoption name Skill Level value 5");

        // Read all output to ensure settings were applied properly (Only outputs for errors, no output = good)
        printBuffer();
    }

    public void sendCommand(String command){
        try{
            writer.write(command + '\n');
            writer.flush();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean readUciOk(){
        String line;
        long timeout = 2000;
        long start = System.currentTimeMillis();

        try {
            while (System.currentTimeMillis() - start < timeout){
                if (reader.ready()){
                    line = reader.readLine();
                     
                    if (line.equals("uciok")){
                        return true;
                    }
                    else {
                        Thread.sleep(50);
                    }
                }
            }
            return false;
        }       
        catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public void printBuffer(){
        try{
            if (!reader.ready()){
                System.out.println("No data in read buffer");
            }

            while(reader.ready()){
                System.out.println(reader.readLine());
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    // Returns null in case of no data found, otherwise best move
    // Moves are returned as algebraic square labels, however internally squares are numbered based on perspective.
    // Must convert moves to the player's perspective of the board blackSquareIndex = 63 - whiteSquareIndex;
    public int[] getBestMove(){
        String line;
        String move;
        try{
            while ((line = reader.readLine()) != null){
                if ((move = UciParser.extractBestMove(line)) != null){
                    return UciParser.convertMove(move);
                };
            }
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }

        return null;
    }

    public boolean sendUciNewGameAndWait(){
        sendCommand("ucinewgame");
        sendCommand("isready");

        if (readOutputLine("readyok")){
            return true;
        }
        else {
            return false;
        }
    }

    public boolean readOutputLine(String line){
        String readLine;
        try{
            while ((readLine = reader.readLine()) != null){
                if (readLine.equals(line)){
                    return true;
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return false;
    }
}
