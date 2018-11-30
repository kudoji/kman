package com.kudoji.kman.models;

import java.math.BigDecimal;
import java.util.HashMap;

import com.kudoji.kman.Kman;
import com.kudoji.kman.utils.Strings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import com.kudoji.kman.enums.AccountTake;

/**
 *
 * @author kudoji
 */
public class Transaction {
    private int id;
    private StringProperty date;
    /**
     * Keeps
     */
    private StringProperty typeUserFormat;
    private StringProperty accountUserFormat;
    private StringProperty categoryUserFormat;
    private StringProperty amountUserFormat;
    private StringProperty balanceUserFormat;

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
     * Keeps payee for the transaction
     */
    private Payee payee = null;
    /**
     * Keeps account where money transfer to for the transaction
     */
    private Account accountTo = null;
    /**
     * Keeps account money transferred from for the transaction
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
        if (_params == null) throw new IllegalArgumentException();

        this.id = Integer.parseInt(_params.get("id"));
        this.date = new SimpleStringProperty(_params.get("date"));

        this.typeUserFormat = new SimpleStringProperty("");
        setTypeId(Integer.parseInt(_params.get("transaction_types_id")));

        this.categoryUserFormat = new SimpleStringProperty("");
        setCategoryId(Integer.parseInt(_params.get("categories_id")));

        if (_params.get("payees_id") == null){
            setPayeeId(0);
        }else{
            setPayeeId(Integer.parseInt(_params.get("payees_id")));
        }
        if (_params.get("account_from_id") == null){
            setAccountFromId(0);
        }else{
            setAccountFromId(Integer.parseInt(_params.get("account_from_id")));
        }

        this.amount_from = Float.parseFloat(_params.get("amount_from"));
        this.balance_from = Float.parseFloat(_params.get("balance_from"));
        if (_params.get("account_to_id") == null){
            setAccountToId(0);
        }else{
            setAccountToId(Integer.parseInt(_params.get("account_to_id")));
        }
        this.amount_to = Float.parseFloat(_params.get("amount_to"));
        this.balance_to = Float.parseFloat(_params.get("balance_to"));
        this.notes = _params.get("notes");

        this.accountUserFormat = new SimpleStringProperty("");
        setAccountUserFormat(getAccountString());

        this.amountUserFormat = new SimpleStringProperty("");
        setAmountUserFormat(getAmountString());

        this.balanceUserFormat = new SimpleStringProperty("");
        setBalanceUserFormat(getBalanceString());
    }

    /**
     * Sets the account this transactions is shown for
     * Affects on visual part of account's, amount's and balance's user representation
     *
     * @param _account
     */
    public void setAccountForTransaction(Account _account){
        this.accountForTransaction = _account;

        setAccountUserFormat(getAccountString());
        setAmountUserFormat(getAmountString());
        setBalanceUserFormat(getBalanceString());
    }
    
    /**
     * Update all instance's variables from _params
     * @param _params 
     */
    public void setFields(HashMap<String, String> _params){
        if (_params == null) throw new IllegalArgumentException();

//        this.id = (int)_params.get("id");
        setDate(_params.get("date"));
        setTypeId(Integer.parseInt(_params.get("transaction_types_id")));

        setCategoryId(Integer.parseInt(_params.get("categories_id")));
        if (_params.get("payees_id") == null){
            setPayeeId(0);
        }else{
            setPayeeId(Integer.parseInt(_params.get("payees_id")));
        }
        if (_params.get("account_from_id") == null){
            setAccountFromId(0);
        }else{
            setAccountFromId(Integer.parseInt(_params.get("account_from_id")));
        }
        this.amount_from = Float.parseFloat(_params.get("amount_from"));
        this.balance_from = Float.parseFloat(_params.get("balance_from"));
        if (_params.get("account_to_id") == null){
            setAccountToId(0);
        }else{
            setAccountToId(Integer.parseInt(_params.get("account_to_id")));
        }
        this.amount_to = Float.parseFloat(_params.get("amount_to"));
        this.balance_to = Float.parseFloat(_params.get("balance_to"));
        this.notes = _params.get("notes");

        setAccountUserFormat(getAccountString());
        setAmountUserFormat(getAmountString());
        setBalanceUserFormat(getBalanceString());
    }
    
    public int getId(){
        return this.id;
    }
    
    public final String getDate(){
        return this.date.get();
    }

    public final void setDate(String _date){
        if (_date == null) throw new IllegalArgumentException();

        this.date.set(_date);
    }

    public StringProperty dateProperty(){
        return this.date;
    }

    /**
     * Returns transaction's type id
     *
     * @return
     */
    public final int getTypeId(){
        return this.transaction_types_id;
    }

    /**
     * Changes transaction's type to the selected _id
     *
     * @param _id
     */
    public final void setTypeId(int _id){
        if (this.transaction_types_id != _id){
            //  just to avoid re-setting to the same value
            this.transaction_types_id = _id;
            this.typeUserFormat.set(TransactionType.getTransactionType(this.transaction_types_id).toString());
        }
    }

    public StringProperty typeUserFormatProperty(){
        return this.typeUserFormat;
    }

    public void setPayeeId(int _id){
        if (this.payees_id != _id){
            this.payees_id = _id;
            this.payee = Payee.getPayee(this.payees_id);
        }
    }

    public int getPayeeId(){
        return this.payees_id;
    }

    public void setAccountFromId(int _id){
        if (this.account_from_id != _id){
            this.account_from_id = _id;
            this.accountFrom = Account.getAccount(this.account_from_id);
        }
    }

    public int getAccountFromId(){
        return this.account_from_id;
    }

    public void setAccountToId(int _id){
        if (this.account_to_id != _id){
            this.account_to_id = _id;
            this.accountTo = Account.getAccount(this.account_to_id);
        }
    }

    public int getAccountToId(){
        return this.account_to_id;
    }

    /**
     * Sets account formatted string to the specified value
     * @param _account
     */
    public final void setAccountUserFormat(String _account){
        this.accountUserFormat.set(_account);
    }

    public StringProperty accountUserFormatProperty(){
        return this.accountUserFormat;
    }

    public int getCategoryId(){
        return this.categories_id;
    }

    public void setCategoryId(int _id){
        if (this.categories_id != _id){
            this.categories_id = _id;
            this.category = Category.getCategory(this.categories_id);
            this.categoryUserFormat.set(getCategoryString());
        }
    }

    public StringProperty categoryUserFormatProperty(){
        return this.categoryUserFormat;
    }

    public float getAmountFrom(){
        return this.amount_from;
    }

    public float getAmountTo(){
        return this.amount_to;
    }

    public void setAmountUserFormat(String _value){
        this.amountUserFormat.set(_value);
    }

    public StringProperty amountUserFormatProperty(){
        return this.amountUserFormat;
    }

    public void setBalanceUserFormat(String _value){
        this.balanceUserFormat.set(_value);
    }
    public StringProperty balanceUserFormatProperty(){
        return this.balanceUserFormat;
    }
    
    public String getNotes(){
        return this.notes;
    }
    
    public String getPayeeString(){
        if (this.payee == null){
            return "";
        }else{
            return this.payee.toString();
        }
    }
    
    public String getCategoryString(){
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
            return this.accountTo;
        }else{
            return this.accountFrom;
        }
    }
    
    public String getAccountString(boolean _getAccountTo){
        Account acc = getAccount(_getAccountTo);
        if (acc != null){
            return getAccount(_getAccountTo).getName();
        }else{
            return "";
        }
    }
    
    /**
     * Used in TableView to show current account string
     * 
     * @return 
     */
    public String getAccountString(){
        if (this.id < 1){ //fake transaction
            return "";
        }
        
        if (this.transaction_types_id == TransactionType.ACCOUNT_TYPES_DEPOSIT){
            //in that case it makes sense to show payee in the table
            if (this.accountForTransaction == null || this.accountForTransaction.getId() == 0){   //  root account
                return getAccountString(true) + " < " + getPayeeString();
            }else{
                return " < " + getPayeeString();
            }
        }else if (this.transaction_types_id == TransactionType.ACCOUNT_TYPES_WITHDRAWAL){
            //show payee as well
            if (this.accountForTransaction == null || this.accountForTransaction.getId() == 0){
                return getAccountString(false) + " > " + getPayeeString();
            }else{
                return " > " + getPayeeString();
            }
        }else if (this.transaction_types_id == TransactionType.ACCOUNT_TYPES_TRANSFER){
            //show account where money transfered to
            if (this.accountForTransaction == null || this.accountForTransaction.getId() == 0){
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
     * Shows transaction's user formatted amount depending on the transaction's type
     * 
     * @return 
     */
    public String getAmountString(){
        if (this.id < 1){ //fake transaction
            return "";
        }
        
        switch (this.transaction_types_id){
            case TransactionType.ACCOUNT_TYPES_DEPOSIT:
                return "+" + Strings.userFormat(this.amount_to);
            case TransactionType.ACCOUNT_TYPES_WITHDRAWAL:
                return "-" + Strings.userFormat(this.amount_from);
            case TransactionType.ACCOUNT_TYPES_TRANSFER:
                if (this.accountForTransaction == null || this.accountForTransaction.getId() == 0){
                    return " -" + Strings.userFormat(this.amount_from) + " > +" + Strings.userFormat(this.amount_to);
                }else{
                    //show amount for the account
                    if (this.accountForTransaction.getId() == this.account_from_id){
                        return "-" + Strings.userFormat(this.amount_from);
                    }else if (this.accountForTransaction.getId() == this.account_to_id){
                        return "+" + Strings.userFormat(this.amount_to);
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
                return Strings.userFormat(this.balance_to);
            case TransactionType.ACCOUNT_TYPES_WITHDRAWAL:
                return Strings.userFormat(this.balance_from);
            case TransactionType.ACCOUNT_TYPES_TRANSFER:
                if (this.accountForTransaction == null || this.accountForTransaction.getId() == 0){
                    return Strings.userFormat(this.balance_from) + " > " + Strings.userFormat(this.balance_to);
                }else{
                    //show balance for the selected account
                    if (this.accountForTransaction.getId() == this.account_from_id){
                        return Strings.userFormat(this.balance_from);
                    }else if (this.accountForTransaction.getId() == this.account_to_id){
                        return Strings.userFormat(this.balance_to);
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
        switch (this.getTypeId()){
            case TransactionType.ACCOUNT_TYPES_DEPOSIT:
                //increase all transactions after the current one
                if (!Transaction.increaseBalance(this, AccountTake.TO, -this.getAmountTo())){
                    if (_useDBTransaction) Kman.getDB().rollbackTransaction();

                    System.err.println("Unable to update transactions' balance after '" +
                            this.getId() + "' for accountID: " + this.getAccountToId());

                    return false;
                }
                //decrease accounts balance
                account = this.getAccount(true);
                account.increaseBalanceCurrent(new BigDecimal(-this.getAmountTo()));
                if (!account.update()){
                    if (_useDBTransaction) Kman.getDB().rollbackTransaction();

                    System.err.println("Unable to save current balance for " + account + " account");

                    return false;
                }

                break;
            case TransactionType.ACCOUNT_TYPES_WITHDRAWAL:
                if (!Transaction.increaseBalance(this, AccountTake.FROM, this.getAmountFrom())){
                    if (_useDBTransaction) Kman.getDB().rollbackTransaction();

                    System.err.println("Unable to update transactions' balance after '" +
                            this.getId() + "' for accountID: " + this.getAccountFromId());

                    return false;
                }
                //increase accounts balance
                account = this.getAccount(false);
                account.increaseBalanceCurrent(new BigDecimal(this.getAmountFrom()));
                if (!account.update()){
                    if (_useDBTransaction) Kman.getDB().rollbackTransaction();

                    System.err.println("Unable to save current balance for " + account + " account");

                    return false;
                }

                break;
            case TransactionType.ACCOUNT_TYPES_TRANSFER:
                //in case of transfer, transaction touches both accounts
                if (!Transaction.increaseBalance(this, AccountTake.FROM, this.getAmountFrom())){
                    if (_useDBTransaction) Kman.getDB().rollbackTransaction();

                    System.err.println("Unable to update transactions' balance after '" +
                            this.getId() + "' for accountID: " + this.getAccountFromId());

                    return false;
                }
                //increase accounts balance
                account = this.getAccount(false);
                account.increaseBalanceCurrent(new BigDecimal(this.getAmountFrom()));
                if (!account.update()){
                    if (_useDBTransaction) Kman.getDB().rollbackTransaction();

                    System.err.println("Unable to save current balance for " + account + " account");

                    return false;
                }

                if (!Transaction.increaseBalance(this, AccountTake.TO, -this.getAmountTo())){
                    if (_useDBTransaction) Kman.getDB().rollbackTransaction();

                    System.err.println("Unable to update transactions' balance after '" +
                            this.getId() + "' for accountID: " + this.getAccountToId());

                    return false;
                }
                //decrese accounts balance
                account = this.getAccount(true);
                account.increaseBalanceCurrent(new BigDecimal(-this.getAmountTo()));
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
        params.put("where", "id = " + this.getId());

        if (!Kman.getDB().deleteData(params)){
            if (_useDBTransaction) Kman.getDB().rollbackTransaction();

            System.err.println("Unable to detele transaction with id: " + this.getId());

            return false;
        }

        if (_useDBTransaction){
            if (!Kman.getDB().commitTransaction()){
                Kman.getDB().rollbackTransaction();
                System.err.println("Unable to commit SQL transaction");
                return false;
            }
        }

        //  the last step is to delete transaction from account(s)
        //  has to be done AFTER DB operations to make sure that there is no DB error(s)
        deleteFromAccounts();

        return true;
    }

    /**
     * Adds transaction to account(s) (AccountTo and/or AccountFrom) which depends upon
     * transaction type
     *
     * @return
     */
    public boolean addToAccounts(){
        Account account;
        switch (this.getTypeId()){
            case TransactionType.ACCOUNT_TYPES_DEPOSIT:
                this.accountForTransaction.addTransaction(this);

                break;
            case TransactionType.ACCOUNT_TYPES_WITHDRAWAL:
                this.accountForTransaction.addTransaction(this);

                break;
            case TransactionType.ACCOUNT_TYPES_TRANSFER:
                this.accountForTransaction.addTransaction(this);

                account = this.getAccount(true);
                if (this.accountForTransaction.getId() == account.getId()){
                    account = this.getAccount(false);
                    //  transaction with the same id should be added to the account
                    //  but instance should be different
                    //  this is why let's drop transactions cache so it will be re-created then user goes there
                    account.dropTransactions();
                }else{
                    //  accountForTransaction == getAccount(false)
                    account.dropTransactions();
                }

                break;
            default:
        }

        return true;
    }

    /**
     * Deletes current transaction from account(s) (AccountTo and/or AccountFrom) which depends upon
     * transaction type
     *
     * @return
     */
    private boolean deleteFromAccounts(){
        Account account;
        switch (this.getTypeId()){
            case TransactionType.ACCOUNT_TYPES_DEPOSIT:
                this.accountForTransaction.deleteTransaction(this);

                break;
            case TransactionType.ACCOUNT_TYPES_WITHDRAWAL:
                this.accountForTransaction.deleteTransaction(this);

                break;
            case TransactionType.ACCOUNT_TYPES_TRANSFER:
                //  this is where transaction is attached
                this.accountForTransaction.deleteTransaction(this);

                account = this.getAccount(true);
                if (this.accountForTransaction.getId() == account.getId()){
                    account = this.getAccount(false);
                    //  transaction with the same id should be in this account also BUT instance is different
                    //  can create cycle to find transaction by id
                    //  but easier to drop cache so then user goes to the account cache will be re-created
                    account.dropTransactions();
                }else{
                    //  means that this.accountForTransaction.getId() == this.getAccount(false).getId()
                    account.dropTransactions();

                }

                break;
            default:
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
        //  keep only two digits after point
        _delta = Strings.formatFloat(_delta);

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
        //  keep only two digits after point
        _delta = Strings.formatFloat(_delta);

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
                account_from_id = String.valueOf(_transaction.getAccountFromId());
                account_to_id = account_from_id;
                break;
            case TO:
                // for deposit
                account_from_id = String.valueOf(_transaction.getAccountToId());
                account_to_id = account_from_id;
                break;
            case BOTH:
                // for transfer
                account_from_id = String.valueOf(_transaction.getAccountToId()) + ", " + String.valueOf(_transaction.getAccountToId());
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
        params.put("where", "(account_from_id in (" + account_from_id + ") or account_to_id in (" + account_to_id + ") ) and ( (date > '" + _transaction.getDate() + "') or ( date = '" + _transaction.getDate() + "' and id > " + _transaction.getId() + " ) )");
        
        return (Kman.getDB().updateData(false, params) != 0);
    }

    /**
     * 
     * @param _tvTransactions
     * @param _accountFilter filter transactions for the account. No filter if _accountFilter is null or its ID < 1
     */
    public static void populateTransactionsTable(javafx.scene.control.TableView<Transaction> _tvTransactions, Account _accountFilter){
        if (_accountFilter == null || _accountFilter.getId() == 0){
            //  show no transactions for root account
            return;
        }

        _tvTransactions.setItems(_accountFilter.getTransactions());
        //  scroll to the bottom of the list
        _tvTransactions.scrollTo(_tvTransactions.getItems().size() - 1);
    }
}
