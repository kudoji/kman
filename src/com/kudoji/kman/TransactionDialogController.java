/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kudoji.kman;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;

/**
 * FXML Controller class
 *
 * @author kudoji
 */
public class TransactionDialogController extends Controller {
    private java.util.HashMap<String, Object> formObject;
    
    private Category category = null; //selected by user category
    
    /**
     * Keeps instance of new(updated) transaction
     * When it is a new transaction value is null
     * Otherwise, it is filled by loadFields() method
     */
    private Transaction transaction = null;
    /**
     * contains full list of accounts' instances
     */
    private ArrayList<Account> cacheAccounts = new ArrayList<>();
    /**
     * True when OK button is pressed
     */
    @FXML private Label tAccount, tPayee;
    @FXML private DatePicker dpDate;
    @FXML private ComboBox cbType, cbAccountFrom, cbPayee, cbAccountTo;
    @FXML private CheckBox chbAdvanced;
    @FXML private TextField tfId, tfAmountFrom, tfAmountTo;
    @FXML private Button btnCategory;
    @FXML private TextArea taNotes;
    
    private String errorMessage;

    @FXML
    private void cbTypeOnAction(ActionEvent event){
        TransactionType atSelected = (TransactionType)cbType.getSelectionModel().getSelectedItem();
        int atSelectedIndex = atSelected.getID();

        switch (atSelectedIndex) {
            case TransactionType.ACCOUNT_TYPES_DEPOSIT:
                tAccount.setText("account:");
                tPayee.setText("from:");
                cbPayee.setVisible(true);
                cbAccountTo.setVisible(false);
                
                chbAdvanced.setDisable(true);
                chbAdvanced.setSelected(false);
                chbAdvancedOnAction(null);
                
                break;
            case TransactionType.ACCOUNT_TYPES_WITHDRAWAL:
                tAccount.setText("account:");
                tPayee.setText("payee:");
                cbPayee.setVisible(true);
                cbAccountTo.setVisible(false);
                
                chbAdvanced.setDisable(true);
                chbAdvanced.setSelected(false);
                chbAdvancedOnAction(null);
                
                break;
            case TransactionType.ACCOUNT_TYPES_TRANSFER:
                tAccount.setText("from:");
                tPayee.setText("to:");
                cbPayee.setVisible(false);
                cbAccountTo.setVisible(true);
                chbAdvanced.setDisable(false);
                populateAccountsComboBox(cbAccountTo, (Account)cbAccountFrom.getSelectionModel().getSelectedItem());
                
                break;
            default: //error
                
                break;
        }
    }
    
    @FXML
    private void cbAccountFromOnAction(ActionEvent event){
        int atIndex = cbType.getSelectionModel().getSelectedIndex();
        if (atIndex == TransactionType.ACCOUNT_TYPES_TRANSFER){ //in case of transfer need to update to account list
            populateAccountsComboBox(cbAccountTo, (Account)cbAccountFrom.getSelectionModel().getSelectedItem());
        }
    }
    
    @FXML
    private void btnCategoryOnAction(ActionEvent event){
        java.util.HashMap<String, Category> params = new java.util.HashMap<>(); //selected category
        //since method's parameters sent by value, collection type can be useful here
        if (Kman.showAndWaitForm("CategoriesDialog.fxml", "Select Category...", params)){
            //value were selected
            this.category = params.get("object");
            btnCategory.setText(this.category.getFullPath());
        }
    }
    
    @FXML
    private void btnOKOnAction(ActionEvent event){
        if (!validateFields()){
            Kman.showErrorMessage(this.errorMessage);
        }else{
            if (!saveData()){
                Kman.showErrorMessage("Unable to save transaction data");
            }else{
                super.setChanged();
                this.formObject.put("object", this.transaction);
                super.closeStage();
            }
        }
    }
    
    @FXML
    private void chbAdvancedOnAction(ActionEvent event){
        tfAmountTo.setDisable(!chbAdvanced.isSelected());
    }
    
    /**
     * Form is opened for editing, load all fields with data from the transaction 
     * @param _formObject
     */
    @Override
    public void setFormObject(Object _formObject){
        this.formObject = (java.util.HashMap<String, Object>)_formObject;
        this.transaction = (Transaction)this.formObject.get("object");
        Account account = (Account)this.formObject.get("account");
        
        if (this.transaction != null){
            //an edit form
            tfId.setText(Integer.toString(this.transaction.getID()));
            dpDate.setValue(LocalDate.parse(this.transaction.getDate()));

            int transactionTypeID = this.transaction.getTypeID();
            Kman.selectItemInCombobox(cbType, transactionTypeID);
            cbTypeOnAction(null); //make necessary fiels visible

            switch (transactionTypeID){
                case TransactionType.ACCOUNT_TYPES_DEPOSIT:
                    Kman.selectItemInCombobox(cbAccountFrom, this.transaction.getAccountToID());
                    Kman.selectItemInCombobox(cbPayee, this.transaction.getPayeeID());
                    tfAmountFrom.setText(String.valueOf(this.transaction.getAmountTo()));
                    tfAmountTo.setText("0.00");
                    chbAdvanced.setSelected(false);

                    break;
                case TransactionType.ACCOUNT_TYPES_WITHDRAWAL:
                    Kman.selectItemInCombobox(cbAccountFrom, this.transaction.getAccountFromID());
                    Kman.selectItemInCombobox(cbPayee, this.transaction.getPayeeID());
                    tfAmountFrom.setText(String.valueOf(this.transaction.getAmountFrom()));
                    tfAmountTo.setText("0.00");
                    chbAdvanced.setSelected(false);

                    break;
                case TransactionType.ACCOUNT_TYPES_TRANSFER:
                    Kman.selectItemInCombobox(cbAccountFrom, this.transaction.getAccountFromID());
                    Kman.selectItemInCombobox(cbAccountTo, this.transaction.getAccountToID());
                    tfAmountFrom.setText(String.valueOf(this.transaction.getAmountFrom()));
                    tfAmountTo.setText(String.valueOf(this.transaction.getAmountTo()));

                    chbAdvanced.setSelected(this.transaction.getAmountFrom() != this.transaction.getAmountTo());

                    break;
                default: //something is wrong
            }
            //set amount to visibility
            chbAdvancedOnAction(null);
            this.category = Category.getCategory(this.transaction.getCategoryID());
            btnCategory.setText(this.category.getFullPath());
            taNotes.setText(this.transaction.getNotes());
        }else{
            //new element dialog
            Kman.selectItemInCombobox(cbAccountFrom, account.getID());
        }
    }
    
    public void setAccount(Account _account){
        for (Object item: cbAccountFrom.getItems()){
            Account aItem = (Account)item;
            if (aItem.getID() == _account.getID()){
                cbAccountFrom.getSelectionModel().select(item);
                break;
            }
        }
    }
    
    /**
     * 
     * @param _compobox
     * @param _accountToSkip don't add this account to _combobox
     */
    private void populateAccountsComboBox(ComboBox _combobox, Account _accountToSkip){
        _combobox.getItems().clear();
        
        if (this.cacheAccounts.isEmpty()){ //cache is empty
            java.util.HashMap<String, String> params = new java.util.HashMap<>();
            params.put("table", "accounts");

            ArrayList<java.util.HashMap<String, String>> rows = Kman.getDB().selectData(params);
            for (java.util.HashMap<String, String> row: rows){
                Account aItem = new Account(row);
                this.cacheAccounts.add(aItem);

                if (_accountToSkip != null){
                    if (aItem.getID() == _accountToSkip.getID()){
                        continue;
                    }
                }
                _combobox.getItems().add(aItem);
            }
        }else{ //don't ask DB but take evething from cache
            for (Account account: this.cacheAccounts){
                if (_accountToSkip != null){
                    if (account.getID() == _accountToSkip.getID()){
                        continue;
                    }
                }
                _combobox.getItems().add(account);
            }
        }
    }
    
    private void populatePayeesComboBox(){
        cbPayee.getItems().clear();
        
        java.util.HashMap<String, String> params = new java.util.HashMap<>();
        params.put("table", "payees");

        ArrayList<java.util.HashMap<String, String>> rows = Kman.getDB().selectData(params);
        for (java.util.HashMap<String, String> row: rows){
            Payee pItem = new Payee(row);
            cbPayee.getItems().add(pItem);
        }
    }

    private boolean saveData(){
        java.util.HashMap<String, String> params = new java.util.HashMap<>();
        params.put("table", "transactions");
        
        String transactionDate = dpDate.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);
        params.put("date", transactionDate);
        TransactionType ttSelected = (TransactionType)cbType.getSelectionModel().getSelectedItem();
        params.put("transaction_types_id", Integer.toString(ttSelected.getID()));
        params.put("categories_id", Integer.toString(category.getID()));
        
        int payeeID = 0;
        if ( (ttSelected.getID() == TransactionType.ACCOUNT_TYPES_DEPOSIT) || (ttSelected.getID() == TransactionType.ACCOUNT_TYPES_WITHDRAWAL) ){
            Payee payee = (Payee)cbPayee.getSelectionModel().getSelectedItem();
            payeeID = payee.getID();
        }
        if (payeeID == 0){
            params.put("payees_id", null); //to avoid foreign_key constain error
        }else{
            params.put("payees_id", Integer.toString(payeeID));
        }
        
        int accountFromID = 0, accountToID = 0;
        float amountFrom = 0, amountTo = 0, balanceFrom = 0, balanceTo = 0;
        if (ttSelected.getID() == TransactionType.ACCOUNT_TYPES_DEPOSIT){
            accountFromID = 0;
            amountFrom = 0;
            balanceFrom = 0;
            
            //it is important! Take data from cbAccountFrom not cbAccountTo
            Account account = (Account)cbAccountFrom.getSelectionModel().getSelectedItem();
            accountToID = account.getID();
            
            amountTo = Float.parseFloat(tfAmountFrom.getText());

            float delta = amountTo;
            balanceTo = account.getBalanceDate(transactionDate);
            if (this.transaction != null){
                //in case of editing, increase amount by delta
                delta -= this.transaction.getAmountTo();
            }
            balanceTo += delta;
            account.setBalanceCurrent(account.getBalanceCurrent() + delta);

            boolean accountSaved = account.updateDB();
            if (!accountSaved){
                System.err.println("Unable to save current balance for " + account + " account");
                return false;
            }
            boolean transactionsUpdated = Transaction.increaseBalance(transactionDate, accountToID, delta);
            if (!transactionsUpdated){
                System.err.println("Unable to update transactions' balance after '" + transactionDate + "' for accountID: " + accountToID);
                return false;
            }
        }else if (ttSelected.getID() == TransactionType.ACCOUNT_TYPES_WITHDRAWAL){
            Account account = (Account)cbAccountFrom.getSelectionModel().getSelectedItem();
            accountFromID = account.getID();
            amountFrom = Float.parseFloat(tfAmountFrom.getText());

            float delta = -amountFrom;
            balanceFrom = account.getBalanceDate(transactionDate);
            if (this.transaction != null){
                //in case of editing, increase amount by delta
                delta += this.transaction.getAmountFrom();
            }
            balanceFrom += delta;
            account.setBalanceCurrent(account.getBalanceCurrent() + delta);

            boolean accountSaved = account.updateDB();
            if (!accountSaved){
                System.err.println("Unable to save current balance for " + account + " account");
                return false;
            }

            boolean transactionsUpdated = Transaction.increaseBalance(transactionDate, accountFromID, delta);
            if (!transactionsUpdated){
                System.err.println("Unable to update transactions' balance after '" + transactionDate + "' for accountID: " + accountFromID);
                return false;
            }

            accountToID = 0;
            amountTo = 0;
            balanceTo = 0;
        }else if (ttSelected.getID() == TransactionType.ACCOUNT_TYPES_TRANSFER){
            Account account = (Account)cbAccountFrom.getSelectionModel().getSelectedItem();
            accountFromID = account.getID();
            amountFrom = Float.parseFloat(tfAmountFrom.getText());

            float delta = -amountFrom;
            balanceFrom = account.getBalanceDate(transactionDate);
            if (this.transaction != null){
                //in case of editing, increase amount by delta
                delta += this.transaction.getAmountFrom();
            }
            balanceFrom += delta;
            account.setBalanceCurrent(account.getBalanceCurrent() + delta);

            boolean accountSaved = account.updateDB();
            if (!accountSaved){
                System.err.println("Unable to save current balance for " + account + " account");
                return false;
            }
            
            boolean transactionsUpdated = Transaction.increaseBalance(transactionDate, accountFromID, delta);
            if (!transactionsUpdated){
                System.err.println("Unable to update transactions' balance after '" + transactionDate + "' for accountID: " + accountFromID);
                return false;
            }
            
            account = (Account)cbAccountTo.getSelectionModel().getSelectedItem();
            accountToID = account.getID();
            if (chbAdvanced.isSelected()){ //advanced value is set
                amountTo = Float.parseFloat(tfAmountTo.getText());
            }else{
                amountTo = amountFrom;
            }
            
            delta = amountTo;
            balanceTo = account.getBalanceDate(transactionDate);
            if (this.transaction != null){
                //in case of editing, increase amount by delta
                delta -= this.transaction.getAmountTo();
            }
            balanceTo += delta;
            account.setBalanceCurrent(account.getBalanceCurrent() + delta);

            accountSaved = account.updateDB();
            if (!accountSaved){
                System.err.println("Unable to save current balance for " + account + " account");
                return false;
            }
            
            transactionsUpdated = Transaction.increaseBalance(transactionDate, accountToID, delta);
            if (!transactionsUpdated){
                System.err.println("Unable to update transactions' balance after '" + transactionDate + "' for accountID: " + accountToID);
                return false;
            }
        }
        if (accountFromID == 0){
            params.put("account_from_id", null); //to avoid foreign_key constrain error
        }else{
            params.put("account_from_id", Integer.toString(accountFromID));
        }
        if (accountToID == 0){
            params.put("account_to_id", null); //to avoid foreign_key constrain error
        }else{
            params.put("account_to_id", Integer.toString(accountToID));
        }
        params.put("amount_from", Float.toString(amountFrom));
        params.put("amount_to", Float.toString(amountTo));
        
        params.put("balance_from", Float.toString(balanceFrom));
        params.put("balance_to", Float.toString(balanceTo));
        
        params.put("notes", taNotes.getText());
        
        if (this.transaction != null){//update existed transaction
            params.put("id", Integer.toString(this.transaction.getID()));
        }
        
        int transactionID = Kman.getDB().updateData(this.transaction == null, params);
        
        if (this.transaction == null){ //new transaction is created
            if (transactionID > 0){
                //create new instance to get it back to the class which called this controller
                params.put("id", Integer.toString(transactionID));
                this.transaction = new Transaction(params);
            }else{ //possible DB error
                return false;
            }
        }else{ //need to save new data to the instance
            this.transaction.setFields(params);
        }
        
        return (transactionID > 0);
    }
    
    private boolean validateFields(){
        if (dpDate.getValue() == null){
            this.errorMessage = "Please, pick the transaction date";
            return false;
        }
        
        TransactionType typeSelected = (TransactionType)cbType.getSelectionModel().getSelectedItem();
        if (typeSelected == null){
            this.errorMessage = "Please, select transaction type";
            return false;
        }
        
        if (cbAccountFrom.getSelectionModel().getSelectedItem() == null){
            if ( (typeSelected.getID() == TransactionType.ACCOUNT_TYPES_DEPOSIT) || (typeSelected.getID() == TransactionType.ACCOUNT_TYPES_WITHDRAWAL) ){
                this.errorMessage = "Please, select account field";
            }else{//transfer
                this.errorMessage = "Please, select account you want to transfer money from";
            }
            
            return false;
        }
        
        if (typeSelected.getID() == TransactionType.ACCOUNT_TYPES_DEPOSIT){
            if (cbPayee.getSelectionModel().getSelectedItem() == null){
                this.errorMessage = "Please, select who you received money from";
                return false;
            }
        }else if (typeSelected.getID() == TransactionType.ACCOUNT_TYPES_WITHDRAWAL){
            if (cbPayee.getSelectionModel().getSelectedItem() == null){
                this.errorMessage = "Please, select payee field";
                return false;
            }
        }else if (typeSelected.getID() == TransactionType.ACCOUNT_TYPES_TRANSFER){
            if (cbAccountTo.getSelectionModel().getSelectedItem() == null){
                this.errorMessage = "Please, select account you want to transfer money to";
                return false;
            }
        }else{ //there is no third account type available
            this.errorMessage = "Something is wrong...";
            return false;
        }
        
        if (tfAmountFrom.getText().trim().length() == 0){
            tfAmountFrom.setText("0.0");
        }
        
        if (!tfAmountFrom.getText().matches("[0-9]+\\.*[0-9]*")){
            this.errorMessage = "Please, set the value for amount correctly";
            return false;
        }
        
        if (Float.parseFloat(tfAmountFrom.getText()) <= 0.0){
            this.errorMessage = "Please, set the value for amount more than 0";
            return false;
        }
        
        if (chbAdvanced.isSelected()){
            if (tfAmountTo.getText().trim().length() == 0){
                tfAmountTo.setText("0.0");
            }

            if (!tfAmountTo.getText().matches("[0-9]+\\.*[0-9]*")){
                this.errorMessage = "Please, set the value for amount you want to transfer to account correctly";
                return false;
            }

            if (Float.parseFloat(tfAmountTo.getText()) <= 0.0){
                this.errorMessage = "Please, set the value for amount you want to transfer to more than 0";
                return false;
            }
        }
        
        if (category == null){
            this.errorMessage = "Please, select a category";
            return false;
        }
        
        taNotes.setText(taNotes.getText().trim());
        
        this.errorMessage = "";
        return true;
    }
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        dpDate.setValue(LocalDate.now());
        
        java.util.HashMap<String, String> params = new java.util.HashMap<>();
        params.put("table", "transaction_types");
        java.util.ArrayList<java.util.HashMap<String, String>> rows = Kman.getDB().selectData(params);
        for (int i = 0; i < rows.size(); i++){
            TransactionType tt = new TransactionType(rows.get(i));
            cbType.getItems().add(tt);
        }
        cbType.getSelectionModel().selectFirst();
        cbTypeOnAction(null); //make necessary fiels visible
        
        populateAccountsComboBox(cbAccountFrom, null);
        populatePayeesComboBox();
        
        tfAmountFrom.setText("0.0");
        tfAmountTo.setText("0.0");
    }
}