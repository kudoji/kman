/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kudoji.kman;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author kudoji
 */
public class AccountDialogController extends Controller {
    //used by validate function
    private String errorMessage;
    private final int MAX_ACCOUNT_NAMELENGTH = 15;
    
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
        if (Kman.showAndWaitForm("CurrenciesDialog.fxml", "Select Currency...", params)){
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
        boolean isAccountNew = this.account == null;
        Account tmpAcc = new Account();
        tmpAcc.setName(tfName.getText());
        tmpAcc.setBalanceInitial(Float.valueOf(tfBalanceInitial.getText()));
        if (this.account == null){
            //  current balance for new account is the same
            tmpAcc.setBalanceCurrent(Float.valueOf(tfBalanceInitial.getText()));
        }else{
            tmpAcc.setBalanceCurrent(Float.valueOf(tfBalanceCurrent.getText()));
        }
        tmpAcc.setCurrencyId(this.currency.getID());

        if (this.account != null){ //edit existed account, need to put its id to update sql query
            tmpAcc.setId(this.account.getId());
        }

        if (tmpAcc.update()){
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
    @Override
    public void setFormObject(Object _formObject){
        this.formObject = (java.util.HashMap<String, Account>)_formObject;
        this.account = this.formObject.get("object");
        
        if (this.account != null){//edit account form is opened
            tfId.setText(String.valueOf(this.account.getId()));
            tfName.setText(this.account.getName());
            tfBalanceInitial.setText(String.valueOf(this.account.getBalanceInitial()));
            tfBalanceInitial.setDisable(true);
            tfBalanceCurrent.setText(String.valueOf(this.account.getBalanceCurrent()));
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