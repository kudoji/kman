package com.kudoji.kman.models;

import java.util.HashMap;
import java.util.List;

import com.kudoji.kman.Kman;
import com.kudoji.kman.utils.Strings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

/**
 *
 * @author kudoji
 */
public class Currency {
    public static final String URL_CURRENCY_RATES = "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";
    private final String URL_CURRENCY_RATES_HISTORY = "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist.xml";
    public static final String BASE_CURRENCY_CODE = "USD";
    
    private SimpleIntegerProperty id;
    private SimpleStringProperty name, code, sample, rateString;
    private SimpleBooleanProperty starts_with_code;
    private SimpleFloatProperty rate;
    /**
     * Keeps information about currencies which allows reduce of DB usage
     * Used to get Currency object by currency_id
     * Could've used olCurrenciesList but have to use for cycle every time
     */
    private final static HashMap<Integer, Currency> hmCurrenciesList = new HashMap<>();
    /**
     * A copy of the hmCurrenciesList. Needed for ComboBox
     */
    private final static ObservableList<Currency> olCurrenciesList = FXCollections.observableArrayList();
    
    public Currency(HashMap<String, String> _params){
        if (_params == null) throw new IllegalArgumentException();

        this.id = new SimpleIntegerProperty(Integer.parseInt(_params.get("id")));
        this.name = new SimpleStringProperty(_params.get("name"));
        this.code = new SimpleStringProperty(_params.get("code"));
        this.starts_with_code = new SimpleBooleanProperty(_params.get("starts_with_code").equals("1"));
        this.sample = new SimpleStringProperty(this.getSample());
        this.rate = new SimpleFloatProperty(Float.parseFloat(_params.get("rate")));
        this.rateString = new SimpleStringProperty(this.getRateString());
    }
    
    public void setFields(HashMap<String, String> _params){
        if (_params == null) throw new IllegalArgumentException();

//        this.id.set((int)_params.get("id"));
        this.name.set(_params.get("name"));
        this.code.set(_params.get("code"));
        this.starts_with_code.set(_params.get("starts_with_code").equals("1"));
        this.sample.set(this.getSample());
        this.rate.set(Float.parseFloat(_params.get("rate")));
        this.rateString.set(this.getRateString());
    }
    
    public int getID(){
        return this.id.get();
    }
    
    public String getName(){
        return this.name.get();
    }
    
    public void setName(String _name){
        if (_name == null) throw new IllegalArgumentException();

        this.name.set(_name);
    }
    
    public StringProperty nameProperty(){
        return this.name;
    }
    
    public String getCode(){
        return this.code.get();
    }
    
    public void setCode(String _code){
        if (_code == null) throw new IllegalArgumentException();

        this.code.set(_code);
    }
    
    public StringProperty codeProperty(){
        return this.code;
    }
    
    public boolean getStartsWithCode(){
        return this.starts_with_code.get();
    }
    
    public void setStartsWithCode(boolean _starts_with_code){
        this.starts_with_code.set(_starts_with_code);
    }
    
    public BooleanProperty StartsWithCodeProperty(){
        return this.starts_with_code;
    }
    
    public void setRate(float _rate){
        if (_rate < 0f) throw new IllegalArgumentException();

        this.rate.set(_rate);
        this.rateString.set(this.getRateString());
    }
    
    public float getRate(){
        return this.rate.get();
    }
    
    public FloatProperty rateProperty(){
        return this.rate;
    }
    
    public String getRateString(){
        return "1 " + Currency.BASE_CURRENCY_CODE + " = " + Strings.userFormat(this.rate.get()) + " " + this.code.get();
    }
    
    public static String getRateString(String _code, float _rate){
        return "1 " + Currency.BASE_CURRENCY_CODE + " = " + Strings.userFormat(_rate) + " " + _code;
    }
    
    public StringProperty rateStringProperty(){
        return this.rateString;
    }
    
    /**
     * creates user friendly currency name
     * @return 
     */
    public final String getSample(){
        return Currency.getSample(this.code.get(), this.starts_with_code.get());
    }
    
    public static String getSample(String _code, boolean _starts_with_code){
        String result = "100.99";
        
        if (_starts_with_code){
            result = _code + " " + result;
        }else{
            result = result + " " + _code;
        }
        
        return result;
    }
    
    public StringProperty sampleProperty(){
        return this.sample;
    }
    
    private static void getCurrenciesCache(){
        if (Currency.hmCurrenciesList.isEmpty()){ //never filled, need to get data from DB
//            System.out.println(java.time.LocalDateTime.now());
            HashMap<String, String> params = new HashMap<>();
            params.put("table", "currencies");
            params.put("order", "name asc");
            
            java.util.ArrayList<HashMap<String, String>> rows = Kman.getDB().selectData(params);
            for (HashMap<String, String> row: rows){
                Currency.hmCurrenciesList.put(Integer.parseInt(row.get("id")), new Currency(row));
            }
//            System.out.println(java.time.LocalDateTime.now());
        }
    }
    /**
     * Returns ObservableList of all currencies
     * Uses cache which makes calling the function fast with no re-reading DB
     * 
     * @return 
     */
    public static ObservableList<Currency> getCurrencies(){
        if (Currency.hmCurrenciesList.isEmpty()){ //never filled, need to get data from DB
            Currency.getCurrenciesCache();
        }
        
        if (Currency.olCurrenciesList.isEmpty()){
            Currency.olCurrenciesList.setAll(FXCollections.observableArrayList(Currency.hmCurrenciesList.values()));
            
            Currency.olCurrenciesList.addListener((Change<? extends Currency> c) -> {
                //needs to update hmCurrenciesList HashMap any time list has changed
                while (c.next()){
                    if (c.wasAdded()){
                        List<? extends Currency> lCurrencies = c.getAddedSubList();
                        for (Currency currency : lCurrencies){
//                            System.out.println("added: " + currency);
                            Currency.hmCurrenciesList.put(currency.getID(), currency);
                        }
                    }else if (c.wasRemoved()){
                        List<? extends Currency> lCurrencies = c.getRemoved();
                        for (Currency currency : lCurrencies){
//                            System.out.println("removed: " + currency);
                            Currency.hmCurrenciesList.remove(currency.getID());
                        }
                    }
                }
            });
        }
        
        return Currency.olCurrenciesList;
    }
    
    /**
     * Returns Currency object by currency_id
     * @param _id
     * @return 
     */
    public static Currency getCurrency(int _id){
        if (_id <= 0) return null;
        
        if (Currency.hmCurrenciesList.isEmpty()){
            Currency.getCurrenciesCache();
        }
        
        return Currency.hmCurrenciesList.get(_id);
    }
    
    @Override
    public String toString(){
        return this.name.get() + " (" + this.code.get() + ")";
    }
}
