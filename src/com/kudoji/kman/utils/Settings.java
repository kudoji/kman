package com.kudoji.kman.utils;

import com.kudoji.kman.Kman;
import javafx.stage.Stage;
import java.util.prefs.*;

public class Settings {
    private final static String KMAN_DB_NAME = "kmanDBName";
    private final static String WINDOW_SAVE_NAME = "saveWindowPosition";
    private final static String WINDOW_X_NAME = "windowsX";
    private final static String WINDOW_Y_NAME = "windowsY";
    private final static String WINDOW_WIDTH_NAME = "windowsWidth";
    private final static String WINDOW_HEIGHT_NAME = "windowsHeight";

    private Preferences prefs;
    private Stage stage;

    private boolean saveWindowPosition;

    public Settings(Stage stage){
        this.prefs = Preferences.userNodeForPackage(Settings.class);
        this.stage = stage;
    }

    /**
     * Returns DB name
     * @return
     */
    public String getDBName(){
        return this.prefs.get(KMAN_DB_NAME, Kman.KMAN_DB_NAME_DEFAULT);
    }

    public void readSettings(){
        this.saveWindowPosition = this.prefs.getBoolean(WINDOW_SAVE_NAME, true);
        if (this.saveWindowPosition){
            this.stage.setX(this.prefs.getDouble(WINDOW_X_NAME, this.stage.getX()));
            this.stage.setY(this.prefs.getDouble(WINDOW_Y_NAME, this.stage.getY()));
            this.stage.setWidth(this.prefs.getDouble(WINDOW_WIDTH_NAME, this.stage.getWidth()));
            this.stage.setHeight(this.prefs.getDouble(WINDOW_HEIGHT_NAME, this.stage.getHeight()));
        }
    }

    public void saveSettings(){
        this.prefs.putBoolean(WINDOW_SAVE_NAME, this.saveWindowPosition);
        if (this.saveWindowPosition){
            this.prefs.putDouble(WINDOW_X_NAME, this.stage.getX());
            this.prefs.putDouble(WINDOW_Y_NAME, this.stage.getY());
            this.prefs.putDouble(WINDOW_WIDTH_NAME, this.stage.getWidth());
            this.prefs.putDouble(WINDOW_HEIGHT_NAME, this.stage.getHeight());
        }

        //  save current DB name to open app with
        this.prefs.put(KMAN_DB_NAME, Kman.getDB().getFile());
    }
}
