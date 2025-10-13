package com.YCorp.chessApp.client.javafx.events;

import com.YCorp.chessApp.server.db.RegexGameData;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

// TO_GAME event specifically has unique functionality, maybe I should use a sub event for that?
public class SceneTransitionEvent extends Event {
    //{whitePlayer, whiteHours, whiteMinutes, whiteSeconds, whiteIncrement, blackHours, blackMinutes, blackSeconds, blackIncrement}
    // whitePlayer == 0 --> false
    // whitePlayer == 1 --> true

    private int[] gameSettings = null;
    private RegexGameData replayGame = null;

    public static final EventType<SceneTransitionEvent> ANY = new EventType<>(Event.ANY, "ANY");
    public static final EventType<SceneTransitionEvent> TO_GAME = new EventType<>(SceneTransitionEvent.ANY, "TO_GAME");
    public static final EventType<SceneTransitionEvent> TO_SETTINGS = new EventType<>(SceneTransitionEvent.ANY, "TO_SETTINGS");
    public static final EventType<SceneTransitionEvent> TO_MENU = new EventType<>(SceneTransitionEvent.ANY, "TO_MENU");
    public static final EventType<SceneTransitionEvent> TO_BROWSER = new EventType<>(SceneTransitionEvent.ANY, "TO_BROWSER");
    public static final EventType<SceneTransitionEvent> TO_REPLAY = new EventType<>(SceneTransitionEvent.ANY, "TO_REPLAY");

    public SceneTransitionEvent(EventType<? extends SceneTransitionEvent> eventType){
        super(eventType);
    }

    public SceneTransitionEvent(EventType<? extends SceneTransitionEvent> eventType, int[] settings){
        super(eventType);
        this.gameSettings = settings;
    }

    public SceneTransitionEvent(EventType<? extends SceneTransitionEvent> eventType, RegexGameData game){
        super(eventType);
        this.replayGame = game;
    }

    public int[] getGameSettings(){
        if (gameSettings == null){
            return null;
        }
        else {
            return gameSettings.clone();
        }
    }

    public RegexGameData getReplay(){
        return this.replayGame;
    }

    // Omit overriding of copyFor and getEventType(), event type works properly for custom events, and this
    // event will only be filtered at the stage level and not bubbled to other nodes
}
