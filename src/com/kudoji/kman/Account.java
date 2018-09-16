/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kudoji.kman;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import com.kudoji.kman.utils.Strings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/**
 *
 * @author kudoji
 */
public class Account {
    private IntegerProperty id, currencyId;
    private FloatProperty balanceInitial, balanceCurrent;
    private StringProperty name;

    /**
     * Keeps information about accounts which allows reduce of DB usage
     * Used to get Account object by its id
     * Could've used olAccountsList but have to use for cycle every time to find account by id
     */
    private final static HashMap<Integer, Account> hmAccountsList = new HashMap<>();
    /**
     * A copy of the hmAccountsList. Needed for ComboBox
     */
    private final static ObservableList<Account> olAccountsList = FXCollections.observableArrayList();

    public Account(){
        this.id = new SimpleIntegerProperty(0);
        this.name = new SimpleStringProperty("<<accounts>>");
        this.balanceInitial = new SimpleFloatProperty(0);
        this.balanceCurrent = new SimpleFloatProperty(0);
        this.currencyId = new SimpleIntegerProperty(0);
    }
    
    public Account(int _id, String _name){
        this.id = new SimpleIntegerProperty(_id);
        this.name = new SimpleStringProperty(_name);
    }
    
    public Account(HashMap<String, String> _params){
        this.id = new SimpleIntegerProperty(Integer.parseInt(_params.get("id")));
        this.name = new SimpleStringProperty(_params.get("name"));
        this.balanceInitial = new SimpleFloatProperty(Float.parseFloat(_params.get("balance_initial")));
        this.balanceCurrent = new SimpleFloatProperty(Float.parseFloat(_params.get("balance_current")));
        this.currencyId = new SimpleIntegerProperty(Integer.parseInt(_params.get("currencies_id")));
    }

    public final int getId(){
        return this.id.get();
    }

    public final void setId(int _value){
        this.id.set(_value);
    }

    public final String getName(){
        return this.name.get();
    }

    public final void setName(String _name){
        this.name.set(_name);
    }

    public StringProperty nameProperty(){
        return this.name;
    }

    public final float getBalanceInitial(){
        return this.balanceInitial.get();
    }

    public final void setBalanceInitial(float _value){
        this.balanceInitial.set(_value);
    }

    public FloatProperty balanceInitialProperty(){
        return this.balanceInitial;
    }

    public final float getBalanceCurrent(){
        return this.balanceCurrent.get();
    }

    public final void setBalanceCurrent(float _value){
        //  keep only two digits after point
        _value = Strings.formatFloat(_value);

        this.balanceCurrent.set(_value);
    }

    public FloatProperty balanceCurrentProperty(){
        return this.balanceCurrent;
    }

    public final int getCurrencyId(){
        return this.currencyId.get();
    }

    public final void setCurrencyId(int _value){
        this.currencyId.set(_value);
    }

    public IntegerProperty currencyIdProperty(){
        return this.currencyId;
    }

    public final void increaseBalanceCurrent(float _delta){
        //  keep only two digits after point
        _delta = Strings.formatFloat(_delta);

        this.balanceCurrent.set(this.balanceCurrent.get() + _delta);
    }

    public Currency getCurrency() {
        return Currency.getCurrency(this.getCurrencyId());
    }

    @Override
    public String toString(){
        if (this.id.get() > 0){
            return this.name.get() + " (" + Strings.userFormat(this.balanceCurrent.get()) + ")";
        }else{
            return this.name.get(); //don't show balance for the fake account
        }
        
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
            params.put("where", "(account_from_id = " + this.id.get() + " or account_to_id = " + this.id.get() + ") and date <= '" + _date + "'");
        }else{
            // in this case there is a need to find transaction for the _date which is before _tid
            // excluding _tid
            params.put("where", "(account_from_id = " + this.id.get() + " or account_to_id = " + this.id.get() + ") and date <= '" + _date + "' and id < " + _tid);
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
            
            if (this.id.get() == account_from_id){
                return Float.parseFloat(row.get("balance_from"));
            }else if (this.id.get() == account_to_id){
                return Float.parseFloat(row.get("balance_to"));
            }else{
                //something is wrong
                return 0;
            }
        }
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

        for (Account account: Account.getAccounts()){
            TreeItem<Account> tiAccount = new TreeItem<>(account);
            tiAccounts.getChildren().add(tiAccount);
        }

        return tiAccounts;
    }

    private static void getAccountsCache(){
        if (Account.hmAccountsList.isEmpty()){ //never filled, need to get data from DB
//            System.out.println(java.time.LocalDateTime.now());
            HashMap<String, String> params = new HashMap<>();
            params.put("table", "accounts");
            //  doesn't make sense because list will be create from HashMap
            //  and structure of HashMap is based on account's id
//            params.put("order", "name asc");

            java.util.ArrayList<HashMap<String, String>> rows = Kman.getDB().selectData(params);
            for (HashMap<String, String> row: rows){
                Account.hmAccountsList.put(Integer.parseInt(row.get("id")), new Account(row));
            }
//            System.out.println(java.time.LocalDateTime.now());
        }
    }

    /**
     * Returns ObservableList of all accounts
     * Uses cache which makes calling the function fast with no re-reading DB
     *
     * @return
     */
    public static ObservableList<Account> getAccounts(){
        if (Account.hmAccountsList.isEmpty()){ //never filled, need to get data from DB
            Account.getAccountsCache();
        }

        if (Account.olAccountsList.isEmpty()){
            Account.olAccountsList.setAll(FXCollections.observableArrayList(Account.hmAccountsList.values()).sorted());

            Account.olAccountsList.addListener((ListChangeListener.Change<? extends Account> c) -> {
                //needs to update hmCurrenciesList HashMap any time list has changed
                while (c.next()){
                    if (c.wasAdded()){
                        List<? extends Account> lAccounts = c.getAddedSubList();
                        for (Account account : lAccounts){
//                            System.out.println("added: " + account);
                            Account.hmAccountsList.put(account.getId(), account);
                        }
                    }else if (c.wasRemoved()){
                        List<? extends Account> lAccounts = c.getRemoved();
                        for (Account account : lAccounts){
//                            System.out.println("removed: " + account);
                            Account.hmAccountsList.remove(account.getId());
                        }
                    }
                }
            });
        }

        return Account.olAccountsList;
    }

    /**
     * Returns Account object by its id
     * @param _id
     * @return
     */
    public static Account getAccount(int _id){
        if (_id == 0) return null;

        if (Account.hmAccountsList.isEmpty()){
            Account.getAccountsCache();
        }

        return Account.hmAccountsList.get(_id);
    }

    /**
     * Does all necessary operation for updating/inserting account
     * It works with DB and account cache
     *
     * Method works differently:
     *  id is set (is not 0) - account data will be updated in DB
     *  id is NOT set (equals 0) - account data will be inserted into DB
     *
     * @return
     */
    public boolean update(){
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "accounts");
        params.put("name", this.getName());
        params.put("balance_initial", Strings.formatFloatToString(this.getBalanceInitial()));
        params.put("balance_current", Strings.formatFloatToString(this.getBalanceCurrent()));
        params.put("currencies_id", String.valueOf(this.getCurrencyId()));

        if (this.getId() > 0){
            //  this is existed account
            params.put("id", String.valueOf(this.getId()));
        }

        int accountID;
        //use insert since the account is new (this.isNew = true)
        //use update since the account is existed (this.isNew = false)
        accountID = Kman.getDB().updateData(this.getId() == 0, params);

        if (accountID > 0){
            //  successfully inserted
            if (this.getId() == 0){
                //  this is a new account

                this.setId(accountID);

                Account.getAccounts().add(this);
            }else{
                //  this is an existed account
                //  nothing to do here because account is already in cache and DB
            }
        }else{
            //  DB error
            return false;
        }

        return true;
    }

    /**
     * Does all necessary operations for deleting account
     *
     * @return  True if account is sucessfully deleted
     *          False in case of error
     */
    public boolean delete(){
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "accounts");
        params.put("where", "id = " + this.getId());

        if (Kman.getDB().deleteData(params)) {
            Account.getAccounts().remove(this);

            return true;
        }

        return false;
    }

    /**
     * Copies _value account into current account
     *
     * @param _value
     */
    public void copyFrom(Account _value){
        this.setId(_value.getId());
        this.setName(_value.getName());
        this.setBalanceInitial(_value.getBalanceInitial());
        this.setBalanceCurrent(_value.getBalanceCurrent());
        this.setCurrencyId(_value.getCurrencyId());
    }
}
