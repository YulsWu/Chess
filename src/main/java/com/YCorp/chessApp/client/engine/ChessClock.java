package com.YCorp.chessApp.client.engine;
import java.util.ArrayList;

import com.YCorp.chessApp.client.engine.callback.TickCall;
import com.YCorp.chessApp.client.engine.callback.TimeoutCall;

/*
 * Realizing this is way overengineered. The same functionality can be achieved through the use of a single thread modifying
 * each player's chess clock values in addition to using System.nanoTime() to compare and calculate time elapsed
 */

// Base time increment is MILLISECONDS, passed time is measured in nanoseconds but converted to millis before subtracting from timer
public class ChessClock {
    private Thread whiteThread;
    private Thread blackThread;
    private Thread killThread;

    private ChessTimer whiteTimer;
    private ChessTimer blackTimer;

    private boolean blackStarted = false;
    private boolean started = false;
    private boolean whitesTurn = true;

    private final long WHITE_TIME;
    private final long BLACK_TIME;
    private final long WHITE_INCREMENT;
    private final long BLACK_INCREMENT;
    private final int TICK_RATE; // in seconds

    private ArrayList<TickCall> tickCallList = new ArrayList<>();
    private ArrayList<TimeoutCall> timeoutCallList = new ArrayList<>();;

    // Constructor for equal time - tick rate is provided as MILLISECONDS
    public ChessClock(int hours, int minutes, int seconds, int increment, int tickRate){
        this.WHITE_TIME = (hours * (60*60*1000)) + (minutes * (60 * 1000)) + (seconds * 1000);
        this.BLACK_TIME = this.WHITE_TIME;
        this.WHITE_INCREMENT = increment * 1000;
        this.BLACK_INCREMENT = increment * 1000;
        this.TICK_RATE = tickRate;

        // Create timers and their threads
        whiteTimer = new ChessTimer(true);
        blackTimer = new ChessTimer(false);
        whiteThread = new Thread(whiteTimer, "WHITETHREAD");
        blackThread = new Thread(blackTimer, "BLACKTHREAD");

        killThread = new Thread(() -> this.stop(), "KILLTHREAD");
    }

    // Constructor for unequal time - MILLISECOND tick rate
    public ChessClock(int whiteHours, int whiteMinutes, int whiteSeconds, int whiteIncrement, int blackHours, int blackMinutes, int blackSeconds, int blackIncrement, int tickRate){
        this.WHITE_TIME = (whiteHours * (60*60*1000)) + (whiteMinutes * (60 * 1000)) + (whiteSeconds * 1000);
        this.WHITE_INCREMENT = whiteIncrement * 1000;
        this.BLACK_TIME = (blackHours * (60*60*1000)) + (blackMinutes * (60 * 1000)) + (blackSeconds * 1000);
        this.BLACK_INCREMENT = blackIncrement * 1000;
        this.TICK_RATE = tickRate;

        whiteTimer = new ChessTimer(true);
        blackTimer = new ChessTimer(false);
        whiteThread = new Thread(whiteTimer, "WHITETHREAD");
        blackThread = new Thread(blackTimer, "BLACKTHREAD");

        killThread = new Thread(() -> this.stop(), "KILLTHREAD");
    }

    
    // Starts the timers, White always moves first
    public void start(){
        if (!this.started){
            this.started = true;
            whiteThread.start();
        }
    }

    // Toggles the clock to begin decrementing the other player's time
    public void toggle(){
        if (started){
            // Checks current turn and toggles accordingly. Flips turn at the end
            if (whitesTurn){
                whiteTimer.pause();
                whiteTimer.addTime(WHITE_INCREMENT);
                // Fire tick call here?
                // Different behaviour depending on if black's clock has been started
                if (blackStarted){
                    blackTimer.resume();
                }
                else {
                    blackStarted = true;
                    blackThread.start();
                }
            }
            else {
                blackTimer.pause();
                blackTimer.addTime(BLACK_INCREMENT);
                whiteTimer.resume();
            }
            whitesTurn = !whitesTurn;
        }
        // If white's move starts the clock like in most online chess 
        else {
            start();
            toggle();
        }
    }

    public void stop(){
        System.out.println("ChessClock.stop() reached");
        whiteTimer.stop();
        blackTimer.stop();
        started = false;
    }


    public void fireTickCall(boolean whiteTimer, long timeLeft){

        for (TickCall t : this.tickCallList){
            t.tickCall(whiteTimer, timeLeft);
        }
    }

    public void fireTimeoutCall(boolean whiteTimer){
        for (TimeoutCall t : this.timeoutCallList){
            t.timeoutCall(whiteTimer);
        }
    }

    public boolean isRunning(){
        return this.started;
    }

    public long getTimeLeft(boolean white){
        if (white){
            return whiteTimer.getTimeLeft();
        }
        else {
            return blackTimer.getTimeLeft();
        }
    }

    public void setTickCallbackTargets(ArrayList<TickCall> targets){
        this.tickCallList = targets;
    }

    public void setTimeoutCallbackTargets(ArrayList<TimeoutCall> targets){
        this.timeoutCallList = targets;
    }

    public void addTickCallbackTarget(TickCall t){
        this.tickCallList.add(t);
    }

    public void addTimeoutCallbackTarget(TimeoutCall t){
        this.timeoutCallList.add(t);
    }

    // INNER CLASS------------------------------------------------------------------------------------------------
    // Subclass ChessTimer that will be running on separate threads
    private class ChessTimer implements Runnable{
        private int tick;
        private final Object lock = new Object();
        
        private volatile long timeLeft;
        private volatile boolean running = true;
        private volatile boolean paused = false;

        private boolean whiteTimer;

        private long lastTickNano;

        
        private ChessTimer(boolean whiteTimer){
            this.whiteTimer = whiteTimer;
            this.timeLeft = whiteTimer ? WHITE_TIME : BLACK_TIME; // References Time limit and Tick rate from parent
            this.tick = ChessClock.this.TICK_RATE;
        }

        @Override public void run(){
            // Checks to see if timer has been paused
            while (running && timeLeft > 0){
                // Syncronized block to check if timer has become paused
                // If so, we release the lock and cause the thread to sleep with lock.wait()
                // Technically now another synchronized(lock){} block could run but I'm more using
                // the lock as a way to sleep threads rather than for concurrency
                // Once unpaused, execution will continue and exit the while(paused) block since paused should be == false now
                synchronized (lock) {
                    while (paused){
                        try {
                            // Thread is paused while lock object is locked
                            lock.wait();
                        }
                        // If the thread is interrupted during sleep, we ensure the thread exits cleanly.
                        catch (InterruptedException e){
                            // If interrupted return out of run(), release thread
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }

                // If a timer is stopped while thread is sleeping, we want to exit
                // run() without incrementing again
                if (!running){
                    System.out.println((this.whiteTimer ? "White's" : "Black's") + " timer exited while asleep");
                    return;
                    
                } 

                // Actual clock behaviour
                // Take timestamp before sleep
                try {
                    lastTickNano = System.nanoTime();
                    Thread.sleep(tick);
                }
                catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    return;
                }

                // After sleep calculate actual time elapsed and subtract from the timer
                // Divide ns by 1mill to get ms
                this.timeLeft -= (System.nanoTime() - lastTickNano)/1000000;
                ChessClock.this.fireTickCall(this.whiteTimer, this.timeLeft);
            }

            if (this.timeLeft <= 0){
                // Send timeout event
                ChessClock.this.fireTimeoutCall(this.whiteTimer);
                // Start kill thread to avoid self-killing thread hangs
                // Probably not actually an issue
                ChessClock.this.killThread.start();
            }

            System.out.println((this.whiteTimer ? "White's" : "Black's") + " run() exited while awake");
        }

        public long getTimeLeft(){
            return this.timeLeft;
        }

        public void pause(){
            if (this.running){
                this.paused = true;
            }
        }

        public void resume(){
            if (this.running){

                this.paused = false;
                synchronized(this.lock){
                    lock.notifyAll();
                }

            }
        }

        public void stop(){
            this.running = false;
            // If paused, wakes the thread
            // running = false causes the awoken thread to return out of run()
            if (paused){
                synchronized(this.lock){
                    lock.notifyAll();
                }
                paused = false;
            }
        }

        public void addTime(long time){
            this.timeLeft += time;
        }

        
    }

}
