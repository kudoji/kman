package com.kudoji.kman.models;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import com.kudoji.kman.Kman;
import com.kudoji.kman.utils.Strings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import java.math.BigDecimal;

/**
 *
 * @author kudoji
 */
public class Account {
    private IntegerProperty id, currencyId;
    private ObjectProperty<BigDecimal> balanceInitial, balanceCurrent;
    private StringProperty name;
    //  account's user name which depends on name and current balance
    private StringProperty userName = new SimpleStringProperty("");

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

    /**
     * Keeps transactions for current account
     */
    private final ObservableList<Transaction> olTransactions = FXCollections.observableArrayList();

    public Account(){
        this.id = new SimpleIntegerProperty(0);
        this.name = new SimpleStringProperty("<<accounts>>");
        this.balanceInitial = new SimpleObjectProperty(BigDecimal.ZERO);
        this.balanceCurrent = new SimpleObjectProperty(BigDecimal.ZERO);
        this.currencyId = new SimpleIntegerProperty(0);

        this.setUserName();
    }
    
    public Account(int _id, String _name){
        this.id = new SimpleIntegerProperty(_id);
        this.name = new SimpleStringProperty(_name);

        this.setUserName();
    }
    
    public Account(HashMap<String, String> _params){
        this.id = new SimpleIntegerProperty(Integer.parseInt(_params.get("id")));
        this.name = new SimpleStringProperty(_params.get("name"));
        this.balanceInitial = new SimpleObjectProperty(new BigDecimal(_params.get("balance_initial")));
        this.balanceCurrent = new SimpleObjectProperty(new BigDecimal(_params.get("balance_current")));
        this.currencyId = new SimpleIntegerProperty(Integer.parseInt(_params.get("currencies_id")));

        this.setUserName();
    }

    public final int getId(){
        return this.id.get();
    }

    public final void setId(int _value)
    {
        this.id.set(_value);

        //  update account's user name as well
        this.setUserName();
    }

    public final String getName(){
        return this.name.get();
    }

    public final void setName(String _name){
        this.name.set(_name);

        //  update account's user name as well
        this.setUserName();
    }

    public StringProperty nameProperty(){
        return this.name;
    }

    public final BigDecimal getBalanceInitial(){
        return this.balanceInitial.get();
    }

    public final void setBalanceInitial(BigDecimal _value){
        this.balanceInitial.set(_value);
    }

    public ObjectProperty balanceInitialProperty(){
        return this.balanceInitial;
    }

    public final BigDecimal getBalanceCurrent(){
        return this.balanceCurrent.get();
    }

    public final void setBalanceCurrent(BigDecimal _value){
        this.balanceCurrent.set(_value);

        //  update account's user name as well
        this.setUserName();
    }

    public ObjectProperty balanceCurrentProperty(){
        return this.balanceCurrent;
    }

    public final String getUserName(){
        return this.userName.get();
    }

    /**
     * Sets accoun's user name based on its current data
     */
    private final void setUserName(){
        if (this.id.get() > 0){
            this.userName.set(this.name.get() + " (" + Strings.userFormat(this.balanceCurrent.get().floatValue()) + ")");
        }else{
            this.userName.set(this.name.get()); //don't show balance for the fake account
        }
    }

    public StringProperty userNameProperty(){
        return this.userName;
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

    public final void increaseBalanceCurrent(BigDecimal _delta){
        setBalanceCurrent(this.balanceCurrent.get().add(_delta));
    }

    public Currency getCurrency() {
        return Currency.getCurrency(this.getCurrencyId());
    }

    @Override
    public String toString(){
        return this.getUserName();
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
    public BigDecimal getBalanceDate(String _date, int _tid){
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
                return new BigDecimal(row.get("balance_from"));
            }else if (this.id.get() == account_to_id){
                return new BigDecimal(row.get("balance_to"));
            }else{
                //something is wrong
                return BigDecimal.ZERO;
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
            tiAccounts.getChildren().add(addTreeItem(account));
        }

        return tiAccounts;
    }

    /**
     * Wrap Account instance into TreeItem<Account> with necessary listeners
     * Good article is here https://stackoverflow.com/questions/32478383/updating-treeview-items-from-textfield?lq=1
     *
     * @param _account
     * @return
     */
    private static TreeItem<Account> addTreeItem(Account _account){
        TreeItem<Account> tiAccount = new TreeItem<>(_account);

        ChangeListener<String> userNameListener = (obs, oldName, newName) -> {
            TreeItem.TreeModificationEvent<Account> event = new TreeItem.TreeModificationEvent<>(TreeItem.valueChangedEvent(), tiAccount);
            Event.fireEvent(tiAccount, event);
        };
        //  any time userName changes fire TreeModificationEvent on tiAccount
        _account.userName.addListener(userNameListener);

        //  if value wrapped by TreeItem is changed need to remove listener
        tiAccount.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null){
                oldValue.userNameProperty().removeListener(userNameListener);
            }

            if (newValue != null){
                newValue.userNameProperty().addListener(userNameListener);
            }
        });

        return tiAccount;
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
     * Builds transactions cache for the account
     * Cache re-builds any time method called
     */
    private void buildTransactionsCache(){
        olTransactions.clear();
        int accountId = this.getId();

        if (accountId == 0){
            //  root account has no transactions
            return;
        }

        HashMap<String, String> params = new HashMap<>();
        params.put("table", "transactions");
        params.put("order", "date asc, id asc");
        params.put("where", "account_from_id = " + accountId + " or account_to_id = " + accountId);

        java.util.ArrayList<HashMap<String, String>> rows = Kman.getDB().selectData(params);
        for (HashMap<String, String> row: rows) {
            Transaction transaction = new Transaction(row);
            transaction.setAccountForTransaction(this);
            olTransactions.add(transaction);
        }
    }

    /**
     * Returns transactions for the account
     * @return
     */
    public ObservableList<Transaction> getTransactions(){
        if (olTransactions.isEmpty()){
            buildTransactionsCache();
        }

        return olTransactions;
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
        params.put("balance_initial", Strings.formatFloatToString(this.getBalanceInitial().floatValue()));
        params.put("balance_current", Strings.formatFloatToString(this.getBalanceCurrent().floatValue()));
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
            this.dropTransactions();
            Account.getAccounts().remove(this);

            return true;
        }

        return false;
    }

    /**
     * Drops transaction cache
     */
    public void dropTransactions(){
        this.olTransactions.clear();
    }

    /**
     * Adds transaction to the account
     * @param _transaction
     * @return
     */
    public boolean addTransaction(Transaction _transaction){
        //  do not use getTransactions() method here because
        //  cache is going to be re-built in case it is empty
        return this.olTransactions.add(_transaction);
    }

    /**
     * Deletes transaction from account only
     * Does not delete transaction from DB
     *
     * @param _transaction Transaction to be deleted
     * @return
     */
    public boolean deleteTransaction(Transaction _transaction){
        return this.olTransactions.remove(_transaction);
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
