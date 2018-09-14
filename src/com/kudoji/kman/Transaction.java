/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kudoji.kman;

import java.util.HashMap;
import javafx.scene.control.TreeItem;

/**
 *
 * @author kudoji
 */
public class Transaction {
    private int id;
    private String date;
    private int transaction_types_id;
    private int categories_id;
    private int payees_id;
    private int account_from_id;
    private float amount_from;
    private float balance_from;
    private int account_to_id;
    private float amount_to;
    private float balance_to;
    private String notes;
    
    /**
     * What account to take into consideration
     */
    public static enum AccountTake {
        /**
         * take account_to
         */
        TO,
        /**
         * take account_from
         */
        FROM,
        /**
         * take both account_to and account_from
         */
        BOTH
    }
    /**
     * Keeps transaction type for the transaction
     */
    private TransactionType transactionType = null;
    /**
     * Keeps payee for the transaction
     */
    private Payee payee = null;
    /**
     * Keeps account where money transfer to for the transaction
     */
    private Account accountTo = null;
    /**
     * Keeps account where money transfered from for the transaction
     */
    private Account accountFrom = null;
    /**
     * Show transaction for this account.
     * Affects on getAccountString().
     * Useful in transactions list
     */
    private Account accountForTransaction = null;
    /**
     * Keeps category for the transaction
     */
    private Category category = null;
    
    
    public Transaction(HashMap<String, String> _params){
        this.id = Integer.parseInt(_params.get("id"));
        this.date = _params.get("date");
        this.transaction_types_id = Integer.parseInt(_params.get("transaction_types_id"));
        this.categories_id = Integer.parseInt(_params.get("categories_id"));
        if (_params.get("payees_id") == null){
            this.payees_id = 0;
        }else{
            this.payees_id = Integer.parseInt(_params.get("payees_id"));
        }
        if (_params.get("account_from_id") == null){
            this.account_from_id = 0;
        }else{
            this.account_from_id = Integer.parseInt(_params.get("account_from_id"));
        }
        this.amount_from = Float.parseFloat(_params.get("amount_from"));
        this.balance_from = Float.parseFloat(_params.get("balance_from"));
        if (_params.get("account_to_id") == null){
            this.account_to_id = 0;
        }else{
            this.account_to_id = Integer.parseInt(_params.get("account_to_id"));
        }
        this.amount_to = Float.parseFloat(_params.get("amount_to"));
        this.balance_to = Float.parseFloat(_params.get("balance_to"));
        this.notes = _params.get("notes");
    }
    
    public Transaction(String _date){
        this.date = _date;
    }
    
    public void setAccountForTransaction(Account _account){
        this.accountForTransaction = _account;
    }
    
    /**
     * Update all instance's variables from _params
     * @param _params 
     */
    public void setFields(HashMap<String, String> _params){
//        this.id = (int)_params.get("id");
        this.date = _params.get("date");
        this.transaction_types_id = Integer.parseInt(_params.get("transaction_types_id"));
        this.categories_id = Integer.parseInt(_params.get("categories_id"));
        if (_params.get("payees_id") == null){
            this.payees_id = 0;
        }else{
            this.payees_id = Integer.parseInt(_params.get("payees_id"));
        }
        if (_params.get("account_from_id") == null){
            this.account_from_id = 0;
        }else{
            this.account_from_id = Integer.parseInt(_params.get("account_from_id"));
        }
        this.amount_from = Float.parseFloat(_params.get("amount_from"));
        this.balance_from = Float.parseFloat(_params.get("balance_from"));
        if (_params.get("account_to_id") == null){
            this.account_to_id = 0;
        }else{
            this.account_to_id = Integer.parseInt(_params.get("account_to_id"));
        }
        this.amount_to = Float.parseFloat(_params.get("amount_to"));
        this.balance_to = Float.parseFloat(_params.get("balance_to"));
        this.notes = _params.get("notes");
    }
    
    public int getID(){
        return this.id;
    }
    
    public String getDate(){
        return this.date;
    }
    
    public int getTypeID(){
        return this.transaction_types_id;
    }
    
    public int getAccountFromID(){
        return this.account_from_id;
    }
    
    public int getAccountToID(){
        return this.account_to_id;
    }
    
    public int getPayeeID(){
        return this.payees_id;
    }
    
    public int getCategoryID(){
        return this.categories_id;
    }
    
    public float getAmountFrom(){
        return this.amount_from;
    }
    
    public float getAmountTo(){
        return this.amount_to;
    }
    
    public String getNotes(){
        return this.notes;
    }
    
    /**
     * Returns the TransactionType instance for the transaction
     * @return 
     */
    public TransactionType getType(){
        if (this.id < 1){ //this is a root transaction node (fake transaction)
            return null;
        }
        
        if (this.transactionType == null){
            this.transactionType = TransactionType.getTransactionType(this.transaction_types_id);
        }
        
        return this.transactionType;
    }
    
    public String getTypeString(){
        if (this.transactionType == null){
            this.transactionType = getType();
        }
        
        if (this.transactionType == null){ //maybe a root node
            return "";
        }else{
            return this.transactionType.toString();
        }
    }
    
    /**
     * Returns the payee for the current transaction
     * @return 
     */
    public Payee getPayee(){
        if (this.id < 1) { //this is a fake transaction
            return null;
        }
        
        if (this.payee == null){
            this.payee = Payee.getPayee(this.payees_id);
        }
        
        return this.payee;
    }
    
    public String getPayeeString(){
        if (this.payee == null){
            this.payee = getPayee();
        }
        
        if (this.payee == null){
            return "";
        }else{
            return this.payee.toString();
        }
    }
    
    public Category getCategory(){
        if (this.id < 1){ //fake transaction
            return null;
        }
        
        if (this.category == null){
            this.category = Category.getCategory(this.categories_id);
        }
        
        return this.category;
    }
    
    public String getCategoryString(){
        if (this.category == null){
            this.category = getCategory();
        }
        
        if (this.category == null){
            return "";
        }else{
            return this.category.toString();
        }
    }

    /**
     * Returns accountTo instance for the current transaction
     * @param _getAccountTo if true get accountTo, otherwise - accountFrom
     * @return 
     */
    public Account getAccount(boolean _getAccountTo){
        if (this.id < 1){ //fake transaction
            return null;
        }
        
        if (_getAccountTo){
            if (this.accountTo == null){
                this.accountTo = Account.getAccount(this.account_to_id);
            }

            return this.accountTo;
        }else{
            if (this.accountFrom == null){
                this.accountFrom = Account.getAccount(this.account_from_id);
            }
            
            return this.accountFrom;
        }
    }
    
    public String getAccountString(boolean _getAccountTo){
        if (_getAccountTo){
            if (this.accountTo == null){
                this.accountTo = getAccount(_getAccountTo);
            }

            if (this.accountTo == null){
                return "";
            }else{
                return this.accountTo.getName();
            }
        }else{
            if (this.accountFrom == null){
                this.accountFrom = getAccount(_getAccountTo);
            }

            if (this.accountFrom == null){
                return "";
            }else{
                return this.accountFrom.getName();
            }
        }
    }
    
    /**
     * Used in TreeTableView to show current account string
     * 
     * @return 
     */
    public String getAccountString(){
        if (this.id < 1){ //fake transaction
            return "";
        }
        
        if (this.transaction_types_id == TransactionType.ACCOUNT_TYPES_DEPOSIT){
            //in that case it makes sense to show payee in the table
            if (this.accountForTransaction == null){
                return getAccountString(true) + " < " + getPayeeString();
            }else{
                return " < " + getPayeeString();
            }
            
        }else if (this.transaction_types_id == TransactionType.ACCOUNT_TYPES_WITHDRAWAL){
            //show payee as well
            if (this.accountForTransaction == null){
                return getAccountString(false) + " > " + getPayeeString();
            }else{
                return " > " + getPayeeString();
            }
        }else if (this.transaction_types_id == TransactionType.ACCOUNT_TYPES_TRANSFER){
            //show account where money transfered to
            if (this.accountForTransaction == null){
                //show both sides
                return getAccountString(false) + " > " + getAccountString(true);
            }else{
                //transaction is shown for the pacrticular account
                if (this.accountForTransaction.getId() == this.account_from_id){
                    return " > " + getAccountString(true);
                }else if (this.accountForTransaction.getId() == this.account_to_id){
                    return " < " + getAccountString(false);
                }else{ //something is wrong
                    return "";
                }
            }
            
            
        }else { //something wrong
            return "";
        }
    }
    
    /**
     * Shows transaction's amount depending on the transaction's type
     * 
     * @return 
     */
    public String getAmountString(){
        if (this.id < 1){ //fake transaction
            return "";
        }
        
        switch (this.transaction_types_id){
            case TransactionType.ACCOUNT_TYPES_DEPOSIT:
                return "+" + String.valueOf(this.amount_to);
            case TransactionType.ACCOUNT_TYPES_WITHDRAWAL:
                return "-" + String.valueOf(this.amount_from);
            case TransactionType.ACCOUNT_TYPES_TRANSFER:
                if (this.accountForTransaction == null){
                    return " -" + String.valueOf(this.amount_from) + " > +" + String.valueOf(this.amount_to);
                }else{
                    //show amount for the account
                    if (this.accountForTransaction.getId() == this.account_from_id){
                        return "-" + String.valueOf(this.amount_from);
                    }else if (this.accountForTransaction.getId() == this.account_to_id){
                        return "+" + String.valueOf(this.amount_to);
                    }else{ //something is wrong
                        return "";
                    }
                }
            default: //something is wrong
                return "";
        }
    }
    
    public String getBalanceString(){
        if (this.id < 1){ //fake transaction
            return "";
        }
        
        switch (this.transaction_types_id){
            case TransactionType.ACCOUNT_TYPES_DEPOSIT:
                return String.valueOf(this.balance_to);
            case TransactionType.ACCOUNT_TYPES_WITHDRAWAL:
                return String.valueOf(this.balance_from);
            case TransactionType.ACCOUNT_TYPES_TRANSFER:
                if (this.accountForTransaction == null){
                    return String.valueOf(this.balance_from) + " > " + String.valueOf(this.balance_to);
                }else{
                    //show balance for the selected account
                    if (this.accountForTransaction.getId() == this.account_from_id){
                        return String.valueOf(this.balance_from);
                    }else if (this.accountForTransaction.getId() == this.account_to_id){
                        return String.valueOf(this.balance_to);
                    }else{ //something is wrong
                        return "";
                    }
                }
                
            default: //something is wrong
                return "";
        }
    }

    /**
     * Deletes current transaction from DB making all necessary changes to other transactions
     *
     * @param _useDBTransaction
     * @return
     */
    public boolean delete(boolean _useDBTransaction){
        if (_useDBTransaction){
            if (!Kman.getDB().startTransaction()){
                Kman.showErrorMessage("Couldn't start SQL transaction...");
                return false;
            }
        }

        Account account;
        switch (this.getTypeID()){
            case TransactionType.ACCOUNT_TYPES_DEPOSIT:
                //increase all transactions after the current one
                if (!Transaction.increaseBalance(this, Transaction.AccountTake.TO, -this.getAmountTo())){
                    if (_useDBTransaction) Kman.getDB().rollbackTransaction();

                    System.err.println("Unable to update transactions' balance after '" +
                            this.getID() + "' for accountID: " + this.getAccountToID());

                    return false;
                }
                //decrease accounts balance
                account = this.getAccount(true);
                account.increaseBalanceCurrent(-this.getAmountTo());
                if (!account.update()){
                    if (_useDBTransaction) Kman.getDB().rollbackTransaction();

                    System.err.println("Unable to save current balance for " + account + " account");

                    return false;
                }

                break;
            case TransactionType.ACCOUNT_TYPES_WITHDRAWAL:
                if (!Transaction.increaseBalance(this, Transaction.AccountTake.FROM, this.getAmountFrom())){
                    if (_useDBTransaction) Kman.getDB().rollbackTransaction();

                    System.err.println("Unable to update transactions' balance after '" +
                            this.getID() + "' for accountID: " + this.getAccountFromID());

                    return false;
                }
                //increase accounts balance
                account = this.getAccount(false);
                account.increaseBalanceCurrent(this.getAmountFrom());
                if (!account.update()){
                    if (_useDBTransaction) Kman.getDB().rollbackTransaction();

                    System.err.println("Unable to save current balance for " + account + " account");

                    return false;
                }

                break;
            case TransactionType.ACCOUNT_TYPES_TRANSFER:
                //in case of transfer, transaction touches both accounts
                if (!Transaction.increaseBalance(this, Transaction.AccountTake.FROM, this.getAmountFrom())){
                    if (_useDBTransaction) Kman.getDB().rollbackTransaction();

                    System.err.println("Unable to update transactions' balance after '" +
                            this.getID() + "' for accountID: " + this.getAccountFromID());

                    return false;
                }
                //increase accounts balance
                account = this.getAccount(false);
                account.increaseBalanceCurrent(this.getAmountFrom());
                if (!account.update()){
                    if (_useDBTransaction) Kman.getDB().rollbackTransaction();

                    System.err.println("Unable to save current balance for " + account + " account");

                    return false;
                }

                if (!Transaction.increaseBalance(this, Transaction.AccountTake.TO, -this.getAmountTo())){
                    if (_useDBTransaction) Kman.getDB().rollbackTransaction();

                    System.err.println("Unable to update transactions' balance after '" +
                            this.getID() + "' for accountID: " + this.getAccountToID());

                    return false;
                }
                //decrese accounts balance
                account = this.getAccount(true);
                account.increaseBalanceCurrent(-this.getAmountTo());
                if (!account.update()){
                    if (_useDBTransaction) Kman.getDB().rollbackTransaction();

                    System.err.println("Unable to save current balance for " + account + " account");

                    return false;
                }

                break;
            default: //something is wrong
        }

        //finally, delete the transaction
        java.util.HashMap<String, String> params = new java.util.HashMap<>();
        params.put("table", "transactions");
        params.put("where", "id = " + this.getID());

        if (!Kman.getDB().deleteData(params)){
            if (_useDBTransaction) Kman.getDB().rollbackTransaction();

            System.err.println("Unable to detele transaction with id: " + this.getID());

            return false;
        }

        if (_useDBTransaction){
            if (!Kman.getDB().commitTransaction()){
                Kman.getDB().rollbackTransaction();
                System.err.println("Unable to commit SQL transaction");
                return false;
            }
        }

        return true;
    }

    /**
     * Increases transactions' balance for the _accountID after particular _dateAfter (excluding it)
     * Make sense of using this method with NEW transaction ONLY
     *
     * For existed transaction use overridden method
     *
     * @param _dateAfter
     * @param _accountID
     * @param _delta can be negative
     * @return 
     */
    public static boolean increaseBalance(String _dateAfter, int _accountID, float _delta){
        //  update transactions set
        //
        // balance_from = balance_from + case when account_from_id = _accountID then _delta else 0 end,
        //  balance_to = balance_to + case when account_to_id = _accountID then _delta else 0 end
        //  where (account_from_id = _accountID or account_to_id = _accountID) and date > _dateAfter;

        HashMap<String, String> params = new HashMap<>();
        params.put("table", "transactions");
        params.put("set", "balance_from = balance_from + case when account_from_id = " + _accountID + " then " + _delta + " else 0 end, balance_to = balance_to + case when account_to_id = " + _accountID + " then " + _delta + " else 0 end");

        // this is a new transaction
        // need to change transactions after its date only
        params.put("where", "(account_from_id = " + _accountID + " or account_to_id = " + _accountID + ") and date > '" + _dateAfter + "'");

        return (Kman.getDB().updateData(false, params) != 0);
    }
    
    /**
     * Increases transactions' balance for all  transaction after _transaction (excluding it)
     * Make sense for using the method for EXISTED transaction only
     * 
     * @param _transaction
     * @param _accountTake increase balance for _accountTake transactions
     * @param _delta can be negative
     * @return 
     */
    public static boolean increaseBalance(Transaction _transaction, AccountTake _accountTake, float _delta){
        //transaction id is not reliable data because date affects the order. For instance,
        //
        //date       | id   id (not possible)
        //
        //2018/07/21 | 10   11
        //2018/07/21 | 11   10
        //2018/07/22 |  9    9
        //2018/07/24 |  1    2
        //2018/07/24 |  2    1
        //
        //but for one date transactions ordered by id ascendinly
        //
        String account_from_id, account_to_id;
        if (null == _accountTake){ //not acceptible
            return false;
        }else switch (_accountTake) {
            case FROM:
                // for withdrawal
                account_from_id = String.valueOf(_transaction.getAccountFromID());
                account_to_id = account_from_id;
                break;
            case TO:
                // for deposit
                account_from_id = String.valueOf(_transaction.getAccountToID());
                account_to_id = account_from_id;
                break;
            case BOTH:
                // for transfer
                account_from_id = String.valueOf(_transaction.getAccountToID()) + ", " + String.valueOf(_transaction.getAccountToID());
                account_to_id = account_from_id;
                break;
            default:
                //somethin is wrong
                return false;
        }

        //update transactions set 
        //balance_from = balance_from + case when account_from_id = _accountID then _delta else 0 end, 
        //balance_to = balance_to + case when account_to_id = _accountID then _delta else 0 end 
        //where (account_from_id = _accountID or account_to_id = _accountID) and ( (date > _dateAfter) or ( (date = _dateAfter) and (id > _id) ) );
        
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "transactions");
        params.put("set", "balance_from = balance_from + case when account_from_id in (" + account_from_id + ") then " + _delta + " else 0 end, balance_to = balance_to + case when account_to_id in (" + account_to_id + ") then " + _delta + " else 0 end");
        params.put("where", "(account_from_id in (" + account_from_id + ") or account_to_id in (" + account_to_id + ") ) and ( (date > '" + _transaction.getDate() + "') or ( date = '" + _transaction.getDate() + "' and id > " + _transaction.getID() + " ) )");
        
        return (Kman.getDB().updateData(false, params) != 0);
    }

    /**
     * 
     * @param _ttvTransactions
     * @param _accountFilter filter transactions for the account. No filter if _accountFilter is null or its ID < 1
     */
    public static void populateTransactionsTable(javafx.scene.control.TreeTableView<Transaction> _ttvTransactions, Account _accountFilter){
        TreeItem<Transaction> tiRoot = (TreeItem<Transaction>)_ttvTransactions.getRoot();//.getChildren().get(0);
        tiRoot.getChildren().clear(); //clear the table before filling it
        
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "transactions");
        params.put("order", "date asc, id asc");
        Account accountFotTransaction = null;
        if (_accountFilter != null){
            int accountID = _accountFilter.getId();
            if (accountID > 0){
                params.put("where", "account_from_id = " + accountID + " or account_to_id = " + accountID);
                accountFotTransaction = _accountFilter;
            }
        }
//        params.put("where", "");
        java.util.ArrayList<HashMap<String, String>> rows = Kman.getDB().selectData(params);
        for (HashMap<String, String> row: rows){
            Transaction transaction = new Transaction(row);
            transaction.setAccountForTransaction(accountFotTransaction);
            TreeItem<Transaction> tiTransaction = new TreeItem(transaction);
            
            tiRoot.getChildren().add(tiTransaction);
        }
    }
}
