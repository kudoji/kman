package com.kudoji.kman.utils;

public class Strings {
    public Strings(){
        throw new UnsupportedOperationException("Class is not instantiable");
    }

    /**
     * Returns user formatted float value
     *
     * @param _value
     * @return
     */
    public static String userFormat(float _value){
        return String.format(java.util.Locale.US, "%,.2f", _value);
    }

    /**
     * Removes user format from formatted string
     *
     * @param formattedString
     * @return
     */
    public static String userFormatRemove(String formattedString){
        if (formattedString == null) throw new IllegalArgumentException();

        return formattedString.trim().replace(",", "");
    }

    /**
     * Keeps two digits after point only.
     * For instance, value 123.34824234 would be converted to 123.35
     *
     * @param _value
     * @return Formatted float value
     */
    public static float formatFloat(float _value){
        return Float.parseFloat(String.format("%.2f", _value));
    }

    /**
     * Keeps two digits after point only.
     * For instance, value 123.34824234 would be converted to 123.35
     *
     * @param _value
     * @return Formatted String value
     */
    public static String formatFloatToString(float _value){
        return String.format("%.2f", _value);
    }
}
