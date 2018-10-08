package com.kudoji.kman.utils;

public class Urls {
    /**
     * Opens provided _url
     * @param _url
     */
    public static void openUrl(String _url){
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
            System.err.println(e.getClass() + ": " + e.getMessage());
        }
    }
}
