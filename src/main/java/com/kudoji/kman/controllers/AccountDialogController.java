package com.kudoji.kman.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.kudoji.kman.models.Account;
import com.kudoji.kman.models.Currency;
import com.kudoji.kman.Kman;
import com.kudoji.kman.utils.Strings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import java.math.BigDecimal;

/**
 * FXML Controller class
 *
 * @author kudoji
 */
public class AccountDialogController extends Controller {
    //used by validate function
    private String errorMessage;
    private final int MAX_ACCOUNT_NAMELENGTH = 25;
    
    /**
     * keeps account object
     */
    private Account account;
    /**
     * Keeps selected by user currency
     */
    private Currency currency = null;
    private java.util.HashMap<String, Account> formObject;
    
    @FXML private TextField tfId, tfName, tfBalanceInitial, tfBalanceCurrent;
    @FXML private javafx.scene.control.Button btnCurrency;

    @Override
    public void btnEnterOnAction(){
        btnOKOnAction(null);
    }

    @FXML
    private void btnOKOnAction(ActionEvent action){
        if (!validateFields()){
            Kman.showErrorMessage(this.errorMessage);
        }else{
            if (!saveData()){
                Kman.showErrorMessage("Unable to save account data");
            }else{
                super.setChanged();
                this.formObject.put("object", this.account);
                
                super.closeStage();
            }
        }
    }
    
    @FXML
    private void btnCurrencyOnAction(ActionEvent event){
        java.util.HashMap<String, Currency> params = new java.util.HashMap<>(); //selected category
        //since method's parameters sent by value, collection type can be useful here
        if (Kman.showAndWaitForm("/views/CurrenciesDialog.fxml", "Select Currency...", params)){
            //value were selected
            this.currency = params.get("object");
            btnCurrency.setText(this.currency.getName());
        }
    }
    
    private boolean validateFields(){
        tfName.setText(tfName.getText().trim());
        
        if (tfBalanceInitial.getText().trim().length() == 0){
            tfBalanceInitial.setText("0.0");
        }
        
        if ("".equals(tfName.getText().trim())){
            this.errorMessage = "Please, set account name";
            return false;
        }
        
        if (tfName.getText().length() > MAX_ACCOUNT_NAMELENGTH){
            this.errorMessage = "Please, set account name less than " + MAX_ACCOUNT_NAMELENGTH + " characters";
            return false;
        }

        //  remove user format so later strings can be easily converted to float
        tfBalanceCurrent.setText(Strings.userFormatRemove(tfBalanceCurrent.getText()));
        tfBalanceInitial.setText(Strings.userFormatRemove(tfBalanceInitial.getText()));

        if (!tfBalanceInitial.getText().matches("[0-9]+\\.*[0-9]*")){
            this.errorMessage = "Please, set value for balance correctly";
            return false;
        }
        
        if (this.currency == null){
            this.errorMessage = "Please, select currency";
            return false;
        }
        
        this.errorMessage = "";
        return true;
    }
    
    /**
     * saves account date to DB
     */
    private boolean saveData(){
        Account tmpAcc = new Account();
        tmpAcc.setName(tfName.getText());
        tmpAcc.setBalanceInitial(new BigDecimal(tfBalanceInitial.getText()));
        if (this.account == null){
            //  current balance for new account is the same
            tmpAcc.setBalanceCurrent(new BigDecimal(tfBalanceInitial.getText()));
        }else{
            tmpAcc.setBalanceCurrent(new BigDecimal(tfBalanceCurrent.getText()));
        }
        tmpAcc.setCurrencyId(this.currency.getID());

        if (this.account != null){ //edit existed account, need to put its id to save sql query
            tmpAcc.setId(this.account.getId());
        }

        if (tmpAcc.save()){
            //  account is updated
            if (this.account == null){
                this.account = tmpAcc;
            }else{
                this.account.copyFrom(tmpAcc);
            }
        }else{
            return false;
        }

        return true;
    }
    
    /**
     * load visible fields with data from Account class
     * @param _formObject 
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setFormObject(Object _formObject){
        this.formObject = (java.util.HashMap<String, Account>)_formObject;
        this.account = this.formObject.get("object");
        
        if (this.account != null){//edit account form is opened
            tfId.setText(String.valueOf(this.account.getId()));
            tfName.setText(this.account.getName());
            tfBalanceInitial.setText(Strings.userFormat(this.account.getBalanceInitial().floatValue()));
            tfBalanceInitial.setDisable(true);
            tfBalanceCurrent.setText(Strings.userFormat(this.account.getBalanceCurrent().floatValue()));
            tfBalanceCurrent.setDisable(true);

            this.currency = this.account.getCurrency();
                    
            btnCurrency.setText(this.currency.getName());
            btnCurrency.setDisable(true);
        }
    }
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }
}