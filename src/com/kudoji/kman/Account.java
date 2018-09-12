/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kudoji.kman;

import java.time.LocalDate;
import java.util.HashMap;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/**
 *
 * @author kudoji
 */
public class Account {
    private int id, currencies_id;
    private float balance_initial, balance_current;
    private String name;
    private Currency currency;

    public Account(){
        this.id = 0;
        this.name = "<<accounts>>";
        this.balance_initial = 0;
        this.balance_current = 0;
        this.currencies_id = 0;
        this.currency = null;
    }
    
    public Account(int _id, String _name){
        this.id = _id;
        this.name = _name;
    }
    
    public Account(HashMap<String, String> _params){
        this.id = Integer.parseInt(_params.get("id"));
        this.name = _params.get("name");
        this.balance_initial = Float.parseFloat(_params.get("balance_initial"));
        this.balance_current = Float.parseFloat(_params.get("balance_current"));
        this.currencies_id = Integer.parseInt(_params.get("currencies_id"));
        this.currency = Currency.getCurrency(this.currencies_id);
    }
    
    /**
     * set all instance's variables (except ID) from _params
     * @param _params 
     */
    public void setFields(HashMap<String, String> _params){
//        this.id = (int)_params.get("id");
        this.name = _params.get("name");
        this.balance_initial = Float.parseFloat(_params.get("balance_initial"));
        this.balance_current = Float.parseFloat(_params.get("balance_current"));
        this.currencies_id = Integer.parseInt(_params.get("currencies_id"));
    }
    
    @Override
    public String toString(){
        if (this.id > 0){
            return this.name + " (" + this.balance_current + ")";
        }else{
            return this.name; //don't show balance for the fake account
        }
        
    }
    
    public int getID(){
        return this.id;
    }
    
    public String getName(){
        return this.name;
    }
    
    public void setName(String _name){
        this.name = _name;
    }
    
    public float getBalanceInitial(){
        return this.balance_initial;
    }
    
    public float getBalanceCurrent(){
        return this.balance_current;
    }
    
    /**
     * Returns account's balance on particular date
     * It might be end of the day or balance for the date but before particular transaction id
     *
     * @param _date date to return balance on
     * @param _tid transaction id
     *             can be -1, in this case doesn't compare ids but dates only
     *             if it's not -1, method returns balance BEFORE _tid for _date
     *
     * @return float
     */
    public float getBalanceDate(String _date, int _tid){
        if ( (LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE).equals(_date))
        && (_tid == -1) ){
            // this is the recent transaction for the account
            // which means recent balance is current account's balance
            return this.getBalanceCurrent();
        }
        
        //the idea is to get last transaction for the _date and take its balance
        //why date <= _date but not date = _date because where might not be transactions for the _date, in that case we need to take last one from the previous day
        //select * from transactions where (account_from_id = id or account_to_id = id) and date <= _date [ and id < _tid ] order by date desc, id desc limit 1;
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "transactions");
        if (_tid == -1){
            // in this case transaction id doesn't matter (new transaction)
            // check dates only
            params.put("where", "(account_from_id = " + this.id + " or account_to_id = " + this.id + ") and date <= '" + _date + "'");
        }else{
            // in this case there is a need to find transaction for the _date which is before _tid
            // excluding _tid
            params.put("where", "(account_from_id = " + this.id + " or account_to_id = " + this.id + ") and date <= '" + _date + "' and id < " + _tid);
        }
        params.put("order", "date desc, id desc");
        params.put("limit", "1");
        
        java.util.ArrayList<HashMap<String, String>> rows = Kman.getDB().selectData(params);
        if (rows.isEmpty()){
            //  no transactions from the very first day till the _date (and/or _tid)
            return this.getBalanceInitial();
        }else{
            //record is found
            HashMap<String, String> row = rows.get(0);
            int account_from_id = Integer.parseInt(row.get("account_from_id"));
            int account_to_id = Integer.parseInt(row.get("account_to_id"));
            
            if (this.id == account_from_id){
                return Float.parseFloat(row.get("balance_from"));
            }else if (this.id == account_to_id){
                return Float.parseFloat(row.get("balance_to"));
            }else{
                //something is wrong
                return 0;
            }
        }
    }
    
    public void setBalanceCurrent(float _value){
        this.balance_current = _value;
    }
    
    public void increaseBalanceCurrent(float _delta){
        this.balance_current += _delta;
    }
    
    public int getCurrencyID(){
        return this.currencies_id;
    }
    
    public Currency getCurrency(){
        return this.currency;
    }
    
    /**
     * Updates current account data in DB
     * @return 
     */
    public boolean updateDB(){
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "accounts");
        params.put("id", Integer.toString(this.id));
        params.put("name", this.name);
        params.put("balance_initial", Float.toString(this.balance_initial));
        params.put("balance_current", Float.toString(this.balance_current));
        params.put("currencies_id", Integer.toString(this.currencies_id));
        
        return (Kman.getDB().updateData(false, params) > 0);
    }
    /**
     * Reads accounts from DB and adds to TreeView
     * @param _tvAccounts 
     * @return root item for other accounts 
     */
    public static TreeItem populateAccountsTree(TreeView<Account> _tvAccounts){
        TreeItem<Account> tiAccounts; //root item for other accounts
        
        if (_tvAccounts.getRoot() == null){ //tree view is empty
            Account aRoot = new Account();
            TreeItem<Account> tiRoot = new TreeItem<>(aRoot);
            tiRoot.setExpanded(true);
            
            Account aAccounts = new Account(0, "Accounts");
            tiAccounts = new TreeItem<>(aAccounts);
            tiAccounts.setExpanded(true);
            tiRoot.getChildren().add(tiAccounts);
            
            _tvAccounts.setRoot(tiRoot);
            _tvAccounts.setShowRoot(false);
        }else{
            tiAccounts = (TreeItem<Account>)_tvAccounts.getRoot().getChildren().get(0);
            tiAccounts.getChildren().clear();
        }
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "accounts");
        java.util.ArrayList<HashMap<String, String>> rows = Kman.getDB().selectData(params);
        
        for (int i = 0; i < rows.size(); i++){
            Account account = new Account(rows.get(i));
            TreeItem<Account> tiAccount = new TreeItem<>(account);
            tiAccounts.getChildren().add(tiAccount);
        }
        
        return tiAccounts;
    }
    
    /**
     * Finds account in DB and returns its instance
     * @param _id
     * @return 
     */
    public static Account getAccount(int _id){
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "accounts");
        params.put("id", Integer.toString(_id));
        
        java.util.ArrayList<HashMap<String, String>> rows = Kman.getDB().selectData(params); //should be only one row
        
        if (rows.size() == 1){
            return new Account(rows.get(0));
        }else{
            return null;
        }
    }
}
