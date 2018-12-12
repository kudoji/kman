package com.kudoji.kman.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Urls {
    private static final Logger log = Logger.getLogger(Urls.class.getName());
    /**
     * Opens provided _url
     * @param _url
     */
    public static void openUrl(String _url){
        if (_url == null) throw new IllegalArgumentException();

        Runtime rt = Runtime.getRuntime();

        //  check for OS first
        String os = System.getProperty("os.name").toLowerCase();
        String command = "";
        if (os.indexOf("win") >= 0){
            //  Windows
            command = "rundll32 url.dll,FileProtocolHandler " + _url;
        }else if (os.indexOf("mac") >= 0){
            //  Mac OS X
            command = "open " + _url;
        }else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0){
            //  Linux/Unix
            command = "xdg-open " + _url;
        }else{
            //  unknown OS type
            return;
        }

        try{
            rt.exec(command);
        }catch (Exception e){
            log.log(Level.WARNING, e.getMessage(), e);
        }
    }
}
