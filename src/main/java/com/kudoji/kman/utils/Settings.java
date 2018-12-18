package com.kudoji.kman.utils;

import com.kudoji.kman.Kman;
import javafx.stage.Stage;
import java.util.prefs.*;

public class Settings {
    private static Settings instance;

    private final static String KMAN_DB_NAME = "kmanDBName";
    private final static String WINDOW_SAVE_NAME = "saveWindowPosition";
    private final static String WINDOW_X_NAME = "windowsX";
    private final static String WINDOW_Y_NAME = "windowsY";
    private final static String WINDOW_WIDTH_NAME = "windowsWidth";
    private final static String WINDOW_HEIGHT_NAME = "windowsHeight";
    private final static String WINDOW_DIVIDER_POSITION = "windowsDividerPosition";

    private final Preferences prefs;
    private final Stage stage;

    private double windowDividerPosition = 0.2979274611398964;

    private boolean saveWindowPosition;

    private Settings(Stage stage){
        this.prefs = Preferences.userNodeForPackage(Settings.class);
        this.stage = stage;
    }

    public static Settings getInstance(Stage stage){
        if (instance == null){
            instance = new Settings(stage);
        }

        return instance;
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

            this.windowDividerPosition = this.prefs.getDouble(WINDOW_DIVIDER_POSITION, this.windowDividerPosition);
        }
    }

    public void saveSettings(){
        this.prefs.putBoolean(WINDOW_SAVE_NAME, this.saveWindowPosition);
        if (this.saveWindowPosition){
            this.prefs.putDouble(WINDOW_X_NAME, this.stage.getX());
            this.prefs.putDouble(WINDOW_Y_NAME, this.stage.getY());
            this.prefs.putDouble(WINDOW_WIDTH_NAME, this.stage.getWidth());
            this.prefs.putDouble(WINDOW_HEIGHT_NAME, this.stage.getHeight());

            this.prefs.putDouble(WINDOW_DIVIDER_POSITION, this.windowDividerPosition);
        }

        //  save current DB name to open app with
        this.prefs.put(KMAN_DB_NAME, Kman.getDB().getFile());
    }

    public Boolean getSaveWindowPosition(){
        return saveWindowPosition;
    }
    public void setSaveWindowPosition(Boolean _flag){
        saveWindowPosition = _flag;
    }

    public double getWindowDividerPosition(){
        return this.windowDividerPosition;
    }

    public void setWindowDividerPosition(double position){
        this.windowDividerPosition = position;
    }
}
