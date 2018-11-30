package com.kudoji.kman.models;

import com.kudoji.kman.utils.DB;
import com.kudoji.kman.Kman;
import com.kudoji.kman.utils.Strings;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author kudoji
 */
public class MMEXImport {
    private enum TableType {payees, accounts};
    
    /**
     * cache for payees' id
     */
    private final ArrayList<Integer> payees = new ArrayList<>();
    
    /**
     * cache for created accounts' id. Also uses later to calculate accounts current balance
     */
    private final HashMap<Integer, Float> accounts = new HashMap<>();
    
    private final DB dbMMEX;
    private boolean isError;
    private final boolean insertAllData;
    
    /**
     * 
     * @param _file file path to mmex database
     * @param _allData inserts all data from mmex if true, otherwise takes data which needed for transactions only
     */
    public MMEXImport(String _file, boolean _allData){
        if (_file == null) throw new IllegalArgumentException();

        dbMMEX = new DB(_file);
        if (!dbMMEX.connect()){
            this.isError = true;
            Kman.showErrorMessage("Cannot connect to mmex database (" + _file + ")");
        }else{
            this.isError = false;
        }
        
        this.insertAllData = _allData;
    }

    public void close(){
        this.dbMMEX.close();
    }

    /**
     * 
     * @return true - no errors
     */
    public boolean getStatus(){
        return !this.isError;
    }
    
    public void loadData(){
        if (this.isError) return;
        
        Kman.getDB().startTransaction();
        
        truncateTables();
        if (this.isError) return; //couldn't truncate tables
        
        //CATEGORY_V1 (CATEGNAME)
        //SUBCATEGORY_V1
        //CURRENCYFORMATS_V1
        //PAYEE_V1
        //CHECKINGACCOUNT_V1
        
        if (this.insertAllData){ //need to transfer all data from mmex db
            //will skip currencies table
            //will skip categories table
            //will skip transaction_types table
            
            if (!insertRecord(TableType.accounts)){
                this.isError = true;
                System.err.println("Couldn't insert accounts");
                return;
            }
            if (!insertRecord(TableType.payees)){
                this.isError = true;
                System.err.println("Couldn't insert payees");
                return;
            }
        }//otherwise, only used by transactions data will e transferred 
        
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "CHECKINGACCOUNT_V1"); // = transactions
        //have to get transaction in chronological order
        params.put("order", "TRANSDATE asc, TRANSID asc");
        
        ArrayList<HashMap<String, String>> rows = this.dbMMEX.selectData(params);
        for (HashMap<String, String> row: rows){
            System.out.println("tid: " + row.get("TRANSID"));
            if (!insertTransaction(row)){
                System.err.println("Cannot move transaction with ID = " + row.get("TRANSID"));
                this.isError = true;
                
                break;
            }
        }
        //update balances balance
        updateAccountsBalance();
        
        if (!this.isError){
            Kman.getDB().commitTransaction();
        }else{
            Kman.getDB().rollbackTransaction();
            System.err.println("Error happened during import process...");
        }
    }

    private void updateAccountsBalance(){
        HashMap<String, String> params = new HashMap<>();
        
        for (java.util.Map.Entry<Integer, Float> account: this.accounts.entrySet()){
            params.put("table", "accounts");
            params.put("id", account.getKey().toString());
            params.put("balance_current", account.getValue().toString());
            
            if (Kman.getDB().updateData(false, params) != 1){
                System.out.println("Couldn't update account's balance. ID: " + account.getKey());
                this.isError = true;
                
                return;
            }
        }
    }
    
    private void truncateTables(){
        if (this.isError) return;
        
        String sqlText;
        
        sqlText = "delete from accounts;";
        if (!Kman.getDB().execSQL(sqlText)){
            this.isError = true;
            return;
        }

        sqlText = "delete from payees;";
        if (!Kman.getDB().execSQL(sqlText)){
            this.isError = true;
            return;
        }

        sqlText = "delete from transactions;";
        if (!Kman.getDB().execSQL(sqlText)){
            this.isError = true;
            return;
        }
    }
    
    /**
     * Finds currency's id in kman db by _id from mmmex db
     * @param _id
     * @return returns kman currency id or 0 in case of error
     */
    private int findCurrency(int _id){
        //CURRENCYFORMATS_V1
        //CURRENCY_SYMBOL
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "CURRENCYFORMATS_V1");
        params.put("where", "CURRENCYID = " + Integer.toString(_id));
        
        ArrayList<HashMap<String, String>> rows = this.dbMMEX.selectData(params);
        if (rows.size() != 1) return 0;
        
        HashMap<String, String> row = rows.get(0);
        
        params.put("table", "currencies");
        params.put("where", "code = '" + row.get("CURRENCY_SYMBOL") + "'");
        rows = Kman.getDB().selectData(params);
        
        if (rows.size() != 1) return 0;
        row = rows.get(0);
        
        return Integer.parseInt(row.get("id"));
    }
    
    /**
     * Finds kman's category by using data set from mmex db
     * 
     * @param _category_id mmex category id
     * @param _category_sub_id mmex sub category id
     * @return 2 (Unknown) if nothing found, or fount id
     */
    private int findCategory(int _category_id, int _category_sub_id){
        //mmex tables:
        //CATEGORY_V1
        //SUBCATEGORY_V1
        
        HashMap<String, String> params = new HashMap<>();
        if (_category_sub_id > 0){
            params.put("table", "SUBCATEGORY_V1");
            params.put("where", "SUBCATEGID = " + Integer.toString(_category_sub_id));
        }else{
            params.put("table", "CATEGORY_V1");
            params.put("where", "CATEGID = " + Integer.toString(_category_id));
        }
        
        ArrayList<HashMap<String, String>> rows = this.dbMMEX.selectData(params);
        if (rows.size() != 1) return 2;
        
        HashMap<String, String> row = rows.get(0);
        
        params.put("table", "categories");
        if (_category_sub_id > 0){
            params.put("where", "name = '" + row.get("SUBCATEGNAME") + "'");
        }else{
            params.put("where", "name = '" + row.get("CATEGNAME") + "'");
        }
        
        rows = Kman.getDB().selectData(params);
        
        if (rows.size() != 1) return 2;
        row = rows.get(0);
        
        return Integer.parseInt(row.get("id"));
    }
    
    /**
     * Takes all data from mmex's table and puts in corresponded kman's table
     * 
     * @param _tableType
     * @return 
     */
    private boolean insertRecord(TableType _tableType){
        if (_tableType != TableType.accounts && _tableType != TableType.payees) return false; //incorrect table type
        
        HashMap<String, String> params = new HashMap<>();
        if (_tableType == TableType.payees){
            params.put("table", "PAYEE_V1");
        }else if (_tableType == TableType.accounts){
            params.put("table", "ACCOUNTLIST_V1");
        }
        
        ArrayList<HashMap<String, String>> rows = this.dbMMEX.selectData(params);
        if (rows.isEmpty()){ //no records?
            return true;
        }
        
        boolean result = true;
        
        for (HashMap<String, String> row: rows){
            if (_tableType == TableType.payees){
                params.clear();
                params.put("table", "payees");
                params.put("id", row.get("PAYEEID"));
                params.put("name", row.get("PAYEENAME"));
            }else if (_tableType == TableType.accounts){
                params.clear();
                params.put("table", "accounts");
                params.put("id", row.get("ACCOUNTID"));
                params.put("name", row.get("ACCOUNTNAME"));
                params.put("balance_initial", row.get("INITIALBAL"));
                params.put("balance_current", "0.0");
                int currency_id = findCurrency(Integer.parseInt(row.get("CURRENCYID")));
                params.put("currencies_id", Integer.toString(currency_id));
            }
            
            int inserted_id = Kman.getDB().updateData(true, params);
            if (inserted_id > 0){
                if (_tableType == TableType.payees){
                    this.payees.add(inserted_id);
                }else if (_tableType == TableType.accounts){
                    this.accounts.put(inserted_id, 0f);
                }
            }else{
                return false;
            }
        }
        
        return result;
    }
    
    /**
     * Finds data in MMEX's table and inserts data into corresponded table to kman db
     * @param _id
     * @return true if record created, false in other case
     */
    private boolean insertRecord(int _id, TableType _tableType){
        if (_tableType == TableType.payees){
            if (this.payees.indexOf(_id) != -1){
                //already created
                return true;
            }
        }else if (_tableType == TableType.accounts){
            if (this.accounts.containsKey(_id)){
                //already created
                return true;
            }
        }else{ // this type is not set
            return false;
        }
        
        HashMap<String, String> params = new HashMap<>();
        if (_tableType == TableType.payees){
            params.put("table", "PAYEE_V1");
            params.put("PAYEEID", String.valueOf(_id));
        }else if (_tableType == TableType.accounts){
            params.put("table", "ACCOUNTLIST_V1");
            params.put("ACCOUNTID", String.valueOf(_id));
        }
        
        ArrayList<HashMap<String, String>> rows = this.dbMMEX.selectData(params);
        if (rows.size() != 1){ //something is wrong
            return false;
        }
        
        HashMap<String, String> row = rows.get(0);
        
        params.clear();
        if (_tableType == TableType.payees){
            params.put("table", "payees");
            params.put("id", row.get("PAYEEID"));
            params.put("name", row.get("PAYEENAME"));
        }else if (_tableType == TableType.accounts){
            params.put("table", "accounts");
            params.put("id", row.get("ACCOUNTID"));
            params.put("name", row.get("ACCOUNTNAME"));
            params.put("balance_initial", row.get("INITIALBAL"));
            int currency_id = findCurrency(Integer.parseInt(row.get("CURRENCYID")));
            params.put("currencies_id", Integer.toString(currency_id));
        }
        
        boolean result = Kman.getDB().updateData(true, params) > 0;
        if (result){
            if (_tableType == TableType.payees){
                this.payees.add(_id);
            }else if (_tableType == TableType.accounts){
                this.accounts.put(_id, 0f);
            }
        }

        return result;
    }
    
    private boolean insertTransaction(HashMap<String, String> _row){
        //CHECKINGACCOUNT_V1 table
        
        String transactiontType = _row.get("TRANSCODE").trim();
        int tansaction_type_id;
        String transactionDate = _row.get("TRANSDATE").trim();
        String notes = _row.get("NOTES").trim();
        int payee_id = Integer.parseInt(_row.get("PAYEEID"));
        
        int category_id = Integer.parseInt(_row.get("CATEGID"));
        int category_sub_id = Integer.parseInt(_row.get("SUBCATEGID"));
        
        int account_to_id, account_from_id;
        
        float amount_to, balance_to;
        float amount_from, balance_from;

        if ("Deposit".equals(transactiontType)){
            tansaction_type_id = TransactionType.ACCOUNT_TYPES_DEPOSIT;
            account_to_id = Integer.parseInt(_row.get("ACCOUNTID"));
            account_from_id = 0;

            amount_to = Float.parseFloat(_row.get("TRANSAMOUNT"));
            amount_from = 0;
        }else if ("Transfer".equals(transactiontType)){
            tansaction_type_id = TransactionType.ACCOUNT_TYPES_TRANSFER;
            account_to_id = Integer.parseInt(_row.get("TOACCOUNTID"));
            account_from_id = Integer.parseInt(_row.get("ACCOUNTID"));
            
            amount_to = Float.parseFloat(_row.get("TOTRANSAMOUNT"));
            amount_from = Float.parseFloat(_row.get("TRANSAMOUNT"));
            
            payee_id = 0;
        }else if ("Withdrawal".equals(transactiontType)){
            tansaction_type_id = TransactionType.ACCOUNT_TYPES_WITHDRAWAL;
            account_to_id = 0;
            account_from_id = Integer.parseInt(_row.get("ACCOUNTID"));

            amount_to = 0;
            amount_from = Float.parseFloat(_row.get("TRANSAMOUNT"));
        }else{
            return false;
        }
        
        if (!this.insertAllData){ //records might not be created yet
            if (payee_id > 0){
                if (!insertRecord(payee_id, TableType.payees)){
                    System.err.println("Payee with id = " + payee_id + " cannot be created");
                    return false;
                }
            }

            if (account_to_id > 0){
                if (!insertRecord(account_to_id, TableType.accounts)){
                    System.err.println("Account with id = " + account_to_id + " cannot be created");
                    return false;
                }
            }

            if (account_from_id > 0){
                if (!insertRecord(account_from_id, TableType.accounts)){
                    System.err.println("Account with id = " + account_from_id + " cannot be created");
                    return false;
                }
            }
        }
        
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "transactions");
        params.put("date", transactionDate);
        params.put("transaction_types_id", Integer.toString(tansaction_type_id));
        params.put("categories_id", Integer.toString(findCategory(category_id, category_sub_id)));
        if (payee_id < 1){ //some transactions can have no payee even if it MUST be (i.e, withdrawal)
            params.put("payees_id", null); //to avoid foreign_key constain error
        }else{
            params.put("payees_id", Integer.toString(payee_id));
        }
        
        if (account_from_id > 0){
            //keep only two digits after point
            balance_from = Strings.formatFloat(this.accounts.get(account_from_id) - amount_from);
            this.accounts.put(account_from_id, balance_from);
        }else{
            balance_from = 0;
        }
        if (account_to_id > 0){
            //keep only two digits after point
            balance_to = Strings.formatFloat(this.accounts.get(account_to_id) + amount_to);
            this.accounts.put(account_to_id, balance_to);
        }else{
            balance_to = 0;
        }
        
        if (account_from_id == 0){
            params.put("account_from_id", null); //to avoid foreign_key constrain error
        }else{
            params.put("account_from_id", Integer.toString(account_from_id));
        }
        if (account_to_id == 0){
            params.put("account_to_id", null); //to avoid foreign_key constrain error
        }else{
            params.put("account_to_id", Integer.toString(account_to_id));
        }
        params.put("amount_from", Float.toString(amount_from));
        params.put("amount_to", Float.toString(amount_to));
        
        params.put("balance_from", Float.toString(balance_from));
        params.put("balance_to", Float.toString(balance_to));
        
        params.put("notes", notes);
        
        if (_row.get("TRANSID").equals("313")){
            System.out.println(_row);
            System.out.println(params);
        }
        
        return (Kman.getDB().updateData(true, params) > 0);
    }
}
