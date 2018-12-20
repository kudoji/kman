package com.kudoji.kman.controllers;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import com.kudoji.kman.*;
import com.kudoji.kman.models.*;
import com.kudoji.kman.utils.Strings;
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
    private final static Logger log = Logger.getLogger(TransactionDialogController.class.getName());

    private java.util.HashMap<String, Object> formObject;
    
    private Category category = null; //selected by user category
    
    /**
     * Keeps instance of new(updated) transaction
     * When it is a new transaction value is null
     * Otherwise, it is filled by loadFields() method
     */
    private Transaction transaction = null;

    /**
     * Keeps instance for which account transaction is created/updated
     */
    private Account account = null;

    @FXML private Label tAccount, tPayee;
    @FXML private DatePicker dpDate;
    @FXML private ComboBox<TransactionType> cbType;
    @FXML
    private ComboBox<Account> cbAccountFrom, cbAccountTo;
    @FXML
    private ComboBox<Payee> cbPayee;

    @FXML private CheckBox chbAdvanced;
    @FXML private TextField tfId, tfAmountFrom, tfAmountTo;
    @FXML private Button btnCategory;
    @FXML private TextArea taNotes;
    
    private String errorMessage;

    @FXML
    private void cbTypeOnAction(ActionEvent event){
        TransactionType atSelected = (TransactionType)cbType.getSelectionModel().getSelectedItem();
        int atSelectedIndex = atSelected.getId();

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

                if (this.category == null){
                    //  this is a transfer and category is not currently selected
                    //  get transfer category
                    setCategory(Category.getCategory(Category.CATEGORY_TRANSFER_ID));
                }

                break;
            default: //error
                
                break;
        }
    }

    @FXML
    private void cbPayeeOnAction(ActionEvent event){
        if (this.category == null){
            //  category is not selected yet
            Payee payee = this.cbPayee.getSelectionModel().getSelectedItem();
            if (payee != null){
                //  payee is selected
                TransactionType atSelected = cbType.getSelectionModel().getSelectedItem();

                setCategory(payee.getCategory(atSelected));
            }
        }
    }

    @FXML
    private void btnCategoryOnAction(ActionEvent event){
        java.util.HashMap<String, Category> params = new java.util.HashMap<>(); //selected category
        //since method's parameters sent by value, collection type can be useful here
        //  send current (if any) to the category form
        params.put("object", this.category);
        if (Kman.showAndWaitForm("/views/CategoriesDialog.fxml", "Select Category...", params)){
            //value were selected
            setCategory(params.get("object"));
        }
    }

    @Override
    public void btnEnterOnAction(){
        btnOKOnAction(null);
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

    private void setCategory(Category _category){
        this.category = _category;
        if (_category != null){
            btnCategory.setText(this.category.getFullPath());
        }else{
            btnCategory.setText("category");
        }
    }

    /**
     * Form is opened for editing, load all fields with data from the transaction 
     * @param _formObject
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setFormObject(Object _formObject){
        this.formObject = (java.util.HashMap<String, Object>)_formObject;
        this.transaction = (Transaction)this.formObject.get("object");
        this.account = (Account)this.formObject.get("account");

        if (this.transaction != null){
            //an edit form
            tfId.setText(Integer.toString(this.transaction.getId()));
            dpDate.setValue(LocalDate.parse(this.transaction.getDate()));

            int transactionTypeId = this.transaction.getTypeId();
            Kman.selectItemInCombobox(cbType, transactionTypeId);
            cbTypeOnAction(null); //make necessary fields visible

            switch (transactionTypeId){
                case TransactionType.ACCOUNT_TYPES_DEPOSIT:
                    Kman.selectItemInCombobox(cbAccountFrom, this.transaction.getAccountToId());
                    Kman.selectItemInCombobox(cbPayee, this.transaction.getPayeeId());
                    tfAmountFrom.setText(Strings.userFormat(this.transaction.getAmountTo()));
                    tfAmountTo.setText("0.00");
                    chbAdvanced.setSelected(false);

                    break;
                case TransactionType.ACCOUNT_TYPES_WITHDRAWAL:
                    Kman.selectItemInCombobox(cbAccountFrom, this.transaction.getAccountFromId());
                    Kman.selectItemInCombobox(cbPayee, this.transaction.getPayeeId());
                    tfAmountFrom.setText(Strings.userFormat(this.transaction.getAmountFrom()));
                    tfAmountTo.setText("0.00");
                    chbAdvanced.setSelected(false);

                    break;
                case TransactionType.ACCOUNT_TYPES_TRANSFER:
                    Kman.selectItemInCombobox(cbAccountFrom, this.transaction.getAccountFromId());
                    Kman.selectItemInCombobox(cbAccountTo, this.transaction.getAccountToId());
                    tfAmountFrom.setText(Strings.userFormat(this.transaction.getAmountFrom()));
                    tfAmountTo.setText(Strings.userFormat(this.transaction.getAmountTo()));

                    chbAdvanced.setSelected(this.transaction.getAmountFrom() != this.transaction.getAmountTo());

                    break;
                default: //something is wrong
            }
            //set amount to visibility
            chbAdvancedOnAction(null);

            setCategory(Category.getCategory(this.transaction.getCategoryId()));

            taNotes.setText(this.transaction.getNotes());
        }else{
            //new element dialog
            Kman.selectItemInCombobox(cbAccountFrom, account.getId());
        }
    }
    
    /**
     *  Auxiliary method for additional calculations and committing necessary changes
     *  Used in case of new transaction
     */
    private boolean saveDataTransactionNew(java.util.HashMap<String, String> _params){
        String tdNew = _params.get("date");
        int ttIDNew = Integer.parseInt(_params.get("transaction_types_id"));

        Account account;
        boolean accountSaved = false;
        boolean transactionsUpdated = false;

        int accountFromIDNew = 0, accountToIDNew = 0;
        BigDecimal amountFromNew = BigDecimal.ZERO;
        BigDecimal amountToNew = BigDecimal.ZERO;
        BigDecimal balanceFromNew = BigDecimal.ZERO;
        BigDecimal balanceToNew = BigDecimal.ZERO;

        if (ttIDNew == TransactionType.ACCOUNT_TYPES_DEPOSIT){
            // It is important!
            // Take data from cbAccountFrom not cbAccountTo
            account = (Account)cbAccountFrom.getSelectionModel().getSelectedItem();
            accountToIDNew = account.getId();

            amountToNew = new BigDecimal(tfAmountFrom.getText());
            // new transaction, get balance for the end of transaction date
            balanceToNew = amountToNew.add(account.getBalanceDate(tdNew, -1));

            //  without 100f (f) it would devide without digits (.00)® after point
            account.setBalanceCurrent(amountToNew.add(account.getBalanceCurrent()));
            accountSaved = account.save();
            if (!accountSaved){
                log.warning("Unable to save current balance for " + account + " account");
                return false;
            }

            transactionsUpdated = Transaction.increaseBalance(tdNew, accountToIDNew, amountToNew.floatValue());
            if (!transactionsUpdated) {
                log.warning("Unable to update transactions' balance after '" +
                        tdNew + "' for accountID: " + accountToIDNew);
                return false;
            }
        }else if (    (ttIDNew == TransactionType.ACCOUNT_TYPES_WITHDRAWAL) ||
                (ttIDNew == TransactionType.ACCOUNT_TYPES_TRANSFER) ) {
            account = (Account) cbAccountFrom.getSelectionModel().getSelectedItem();
            accountFromIDNew = account.getId();
            amountFromNew = new BigDecimal(tfAmountFrom.getText());

            balanceFromNew = account.getBalanceDate(tdNew, -1).subtract(amountFromNew);

            account.setBalanceCurrent(account.getBalanceCurrent().subtract(amountFromNew));
            accountSaved = account.save();
            if (!accountSaved) {
                log.warning("Unable to save current balance for " + account + " account");
                return false;
            }

            transactionsUpdated = Transaction.increaseBalance(tdNew, accountFromIDNew, -amountFromNew.floatValue());
            if (!transactionsUpdated) {
                log.warning("Unable to update transactions' balance after '" +
                        tdNew + "' for accountID: " + accountFromIDNew);
                return false;
            }
        }

        //  and additional condition for transfer (part about to account)
        if (ttIDNew == TransactionType.ACCOUNT_TYPES_TRANSFER){
            //  this part is almost identical to DEPOSIT section with few changes
            account = (Account)cbAccountTo.getSelectionModel().getSelectedItem();
            accountToIDNew = account.getId();
            if (chbAdvanced.isSelected()){ //advanced value is set
                amountToNew = new BigDecimal(tfAmountTo.getText());
            }else{
                amountToNew = amountFromNew;
            }

            balanceToNew = account.getBalanceDate(tdNew, -1).add(amountToNew);
            account.setBalanceCurrent(account.getBalanceCurrent().add(amountToNew));

            accountSaved = account.save();
            if (!accountSaved){
                log.warning("Unable to save current balance for " + account + " account");
                return false;
            }

            transactionsUpdated = Transaction.increaseBalance(tdNew, accountToIDNew, amountToNew.floatValue());
            if (!transactionsUpdated){
                log.warning("Unable to update transactions' balance after '" +
                        tdNew + "' for accountID: " + accountToIDNew);
                return false;
            }
        }

        if (accountFromIDNew == 0){
            _params.put("account_from_id", null); //to avoid foreign_key constrain error
        }else{
            _params.put("account_from_id", Integer.toString(accountFromIDNew));
        }
        if (accountToIDNew == 0){
            _params.put("account_to_id", null); //to avoid foreign_key constrain error
        }else{
            _params.put("account_to_id", Integer.toString(accountToIDNew));
        }
        _params.put("amount_from", Strings.formatFloatToString(amountFromNew.floatValue()));
        _params.put("amount_to", Strings.formatFloatToString(amountToNew.floatValue()));

        _params.put("balance_from", Strings.formatFloatToString(balanceFromNew.floatValue()));
        _params.put("balance_to", Strings.formatFloatToString(balanceToNew.floatValue()));

        return true;
    }

    /**
     *  Auxiliary method for additional calculations and committing necessary changes
     *  Used in case of existed transaction
     */
    private boolean saveDataTransactionExisted(java.util.HashMap<String, String> _params){
        //  instead of writting logic for transaction editing,
        //  easier to treat it as deletion and insertion of a new transaction
        //
        //  Agree, it is not real fun but much easier to implement and DB calculations are going to be almost the same

        //  delete current transaction
        //  don't use DB transaction because this method is already wrapped inside DB transaction block
        int tid = this.transaction.getId();
        if (!this.transaction.delete(false)){
            return false;
        }

        //  after deleting,
        //  insert it as a new transaction with the same id to keep transactions in the same order
        _params.put("id", String.valueOf(tid));
        if (!saveDataTransactionNew(_params)){
            return false;
        }

        return true;
    }


    /**
     * Saves transaction data into a database
     *
     * @return  True means data successfully saved,
     *          False - otherwise
     */
    private boolean saveData(){
        boolean isError = false;
        if (!Kman.getDB().startTransaction()) return false; //    something went wrong

        String tdNew = dpDate.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);
        int ttIDNew = cbType.getSelectionModel().getSelectedItem().getId();
        int payeeIDNew = 0;
        if (    (ttIDNew == TransactionType.ACCOUNT_TYPES_DEPOSIT) ||
                (ttIDNew == TransactionType.ACCOUNT_TYPES_WITHDRAWAL)){
            Payee payee = cbPayee.getSelectionModel().getSelectedItem();
            //  set category for payee
            payee.setCategory(this.category, ttIDNew, false);
            //  increment payee usage
            payee.incUsageFreq(1);
            //  save changes to DB
            payee.save();
            payeeIDNew = payee.getId();
        }

        java.util.HashMap<String, String> params = new java.util.HashMap<>();
        params.put("table", "transactions");
        params.put("date", tdNew);
        params.put("transaction_types_id", Integer.toString(ttIDNew));
        params.put("categories_id", Integer.toString(category.getId()));
        if (payeeIDNew == 0){
            params.put("payees_id", null); //to avoid foreign_key constrain error
        }else{
            params.put("payees_id", Integer.toString(payeeIDNew));
        }

        if (this.transaction == null){
            //  this is a new transaction
            if (!saveDataTransactionNew(params)) isError = true;
        }else{
            // this is an existed transaction
            if (!saveDataTransactionExisted(params)) isError = true;
        }

        params.put("notes", taNotes.getText());

        //  always insert new record
        //
        int transactionID = Kman.getDB().updateData(true, params);

        if (isError || !Kman.getDB().commitTransaction()){ //trying to commit changes
            //  unable to commit transaction, rolling it back
            Kman.getDB().rollbackTransaction();

            return false;
        }

        if (transactionID > 0) {
            //  create new instance or update existed one to get it back to the class which called this controller
            params.put("id", Integer.toString(transactionID));
        }
        if (this.transaction == null){ //new transaction is created
            if (transactionID > 0){
                this.transaction = new Transaction(params);
                this.transaction.setAccountForTransaction(this.account);

                //  add newly created transaction to the account(s)
                this.transaction.addToAccounts();
            }else{ //possible DB error
                return false;
            }
        }else{ //need to save new data to the instance
            this.transaction.setFields(params);
            //  add edited transaction to the account(s)
            this.transaction.addToAccounts();
        }

        return (transactionID > 0);
    }

    private boolean validateFields(){
        if (dpDate.getValue() == null){
            this.errorMessage = "Please, pick the transaction date";
            return false;
        }
        
        TransactionType typeSelected = cbType.getSelectionModel().getSelectedItem();
        if (typeSelected == null){
            this.errorMessage = "Please, select transaction type";
            return false;
        }
        
        if (cbAccountFrom.getSelectionModel().getSelectedItem() == null){
            if ( (typeSelected.getId() == TransactionType.ACCOUNT_TYPES_DEPOSIT) || (typeSelected.getId() == TransactionType.ACCOUNT_TYPES_WITHDRAWAL) ){
                this.errorMessage = "Please, select account field";
            }else{//transfer
                this.errorMessage = "Please, select account you want to transfer money from";
            }
            
            return false;
        }
        
        if (typeSelected.getId() == TransactionType.ACCOUNT_TYPES_DEPOSIT){
            if (cbPayee.getSelectionModel().getSelectedItem() == null){
                this.errorMessage = "Please, select who you received money from";
                return false;
            }
        }else if (typeSelected.getId() == TransactionType.ACCOUNT_TYPES_WITHDRAWAL){
            if (cbPayee.getSelectionModel().getSelectedItem() == null){
                this.errorMessage = "Please, select payee field";
                return false;
            }
        }else if (typeSelected.getId() == TransactionType.ACCOUNT_TYPES_TRANSFER){
            if (cbAccountTo.getSelectionModel().getSelectedItem() == null){
                this.errorMessage = "Please, select account you want to transfer money to";
                return false;
            }

            if (cbAccountFrom.getSelectionModel().getSelectedItem().equals(cbAccountTo.getSelectionModel().getSelectedItem())){
                this.errorMessage = "Please, select different account from and account to";
                return false;
            }
        }else{ //there is no third account type available
            this.errorMessage = "Something is wrong...";
            return false;
        }

        //  remove user format so later strings can be easily converted to float
        tfAmountFrom.setText(Strings.userFormatRemove(tfAmountFrom.getText()));

        if (tfAmountFrom.getText().length() == 0){
            tfAmountFrom.setText("0.00");
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
            //  remove user format so later strings can be easily converted to float
            tfAmountTo.setText(Strings.userFormatRemove(tfAmountTo.getText()));

            if (tfAmountTo.getText().length() == 0){
                tfAmountTo.setText("0.00");
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
        cbTypeOnAction(null); //make necessary fields visible

        cbAccountFrom.setItems(Account.getAccounts());
        cbAccountTo.setItems(Account.getAccounts());
        cbPayee.setItems(Payee.getPayees());

        tfAmountFrom.setText("0.00");
        tfAmountTo.setText("0.00");
    }
}