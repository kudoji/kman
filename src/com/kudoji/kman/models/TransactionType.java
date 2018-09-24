package com.kudoji.kman.models;

import com.kudoji.kman.Kman;

import java.util.HashMap;

/**
 *
 * @author kudoji
 */
public class TransactionType {
    /**
     * IDs for account types
     */
    public final static int ACCOUNT_TYPES_DEPOSIT = 1;
    public final static int ACCOUNT_TYPES_WITHDRAWAL = 2;
    public final static int ACCOUNT_TYPES_TRANSFER = 3;
    
    private int id;
    private String name;
    
    public TransactionType(HashMap<String, String> _params){
        this.id = Integer.parseInt(_params.get("id"));
        this.name = _params.get("name");
    }
    
    public int getId(){
        return this.id;
    }
    
    /**
     * Gets data from DB and return and TransactionType instance
     * @param _id
     * @return 
     */
    public static TransactionType getTransactionType(int _id){
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "transaction_types");
        params.put("id", Integer.toString(_id));
        
        java.util.ArrayList<HashMap<String, String>> rows = Kman.getDB().selectData(params); //should be only one row
        
        if (rows.size() == 1){
            return new TransactionType(rows.get(0));
        }else{
            return null;
        }
    }
    
    @Override
    public String toString(){
        return this.name;
    }
}
