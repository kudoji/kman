package com.kudoji.kman.models;

import java.util.HashMap;

import com.kudoji.kman.Kman;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

/**
 *
 * @author kudoji
 */
public class Payee {
    private SimpleIntegerProperty id;
    private SimpleStringProperty name;
    
    /**
     * Keeps information about payees which allows reduce DB usage
     */
    private final static javafx.collections.ObservableList<Payee> payeesCache = FXCollections.observableArrayList();
    
    public Payee(HashMap<String, String> _params){
        this.id = new SimpleIntegerProperty(Integer.parseInt(_params.get("id")));
        this.name = new SimpleStringProperty(_params.get("name"));
    }
    
    public void setFields(HashMap<String, String> _params){
//        this.id = new SimpleIntegerProperty((int)_params.get("id"));
        this.name.set(_params.get("name"));
        //for categories
//        this.name.set((String)_params.get("name"));
//        this.name.set((String)_params.get("name"));
    }

    public String getName(){
        return this.name.get();
    }
    
    public void setName(String _name){
        this.name.set(_name);
    }
    
    /**
     * make name field observable by TableViewCell
     * @return 
     */
    public StringProperty nameProperty(){
        return this.name;
    }
    
    public int getID(){
        return this.id.get();
    }
    
    public static Payee getPayee(int _id){
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "payees");
        params.put("id", Integer.toString(_id));
        
        java.util.ArrayList<HashMap<String, String>> rows = Kman.getDB().selectData(params); //should be only one row
        
        if (rows.size() == 1){
            return new Payee(rows.get(0));
        }else{
            return null;
        }
    }
    
    public static javafx.collections.ObservableList<Payee> getPayees(){
        if (Payee.payeesCache.isEmpty()){ //never filled, need to get data from DB
            HashMap<String, String> params = new HashMap<>();
            params.put("table", "payees");
            params.put("order", "name asc");
            
            java.util.ArrayList<HashMap<String, String>> rows = Kman.getDB().selectData(params);
            for (HashMap<String, String> row: rows){
                Payee.payeesCache.add(new Payee(row));
            }
        }
        
        return Payee.payeesCache;
    }
    
    @Override
    public String toString(){
        return this.name.get();
    }
}
