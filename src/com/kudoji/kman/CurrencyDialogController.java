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
import javafx.scene.input.KeyEvent;

/**
 * FXML Controller class
 *
 * @author kudoji
 */
public class CurrencyDialogController extends Controller {
    //used by validate function
    private String errorMessage;
    private final int MAX_CURRENCY_NAME_LENGTH = 35;
    private final int MAX_CURRENCY_CODE_LENGTH = 5;
    /**
     * Set if form is opened for editing
     */
    private Currency currency = null;
    
    
    @FXML
    private TextField tfId, tfName, tfCode, tfRate;
    @FXML private javafx.scene.control.Label lRate;
    @FXML private javafx.scene.control.CheckBox cbStarts;
    @FXML private javafx.scene.text.Text tSample;
    
    private void setSample(){
        tSample.setText(Currency.getSample(tfCode.getText(), cbStarts.isSelected()));
    }
    
    @FXML
    private void tfCodeOnKeyReleased(KeyEvent event){
        setSample();
    }
    
    @FXML
    private void cbStartsOnAction(ActionEvent event){
        setSample();
    }
    
    @FXML
    private void tfRateOnKeyReleased(KeyEvent event){
        setRateLabel();
    }
    
    @FXML
    private void btnOKOnAction(ActionEvent event){
        if (!validateFields()){
            Kman.showErrorMessage(this.errorMessage);
        }else{
            if (!saveData()){
                Kman.showErrorMessage("Unable to save currency data");
            }else{
                super.closeStage();
            }
        }
    }
    
    private void setRateLabel(){
        if (validateRate()){
            lRate.setText("rate (" + Currency.getRateString(tfCode.getText(), Float.parseFloat(tfRate.getText())) + "):");
        }else{
            lRate.setText("rate:");
        }
    }
    
    private boolean validateRate(){
        return tfRate.getText().matches("[0-9]+\\.*[0-9]*");
    }
    
    private boolean validateFields(){
        tfName.setText(tfName.getText().trim());
        
        if ("".equals(tfName.getText().trim())){
            this.errorMessage = "Please, set currency name";
            return false;
        }
        
        if (tfName.getText().length() > MAX_CURRENCY_NAME_LENGTH){
            this.errorMessage = "Please, set currency name less than " + MAX_CURRENCY_NAME_LENGTH + " characters";
            return false;
        }
        
        tfCode.setText(tfCode.getText().trim());
        
        if ("".equals(tfCode.getText().trim())){
            this.errorMessage = "Please, set currency code";
            return false;
        }
        
        if (tfCode.getText().length() > MAX_CURRENCY_CODE_LENGTH){
            this.errorMessage = "Please, set currency code less than " + MAX_CURRENCY_CODE_LENGTH + " characters";
            return false;
        }
        
        if (tfRate.getText().trim().length() == 0){
            tfRate.setText("0.0");
        }
        
        if (!tfRate.getText().matches("[0-9]+\\.*[0-9]*")){
            this.errorMessage = "Please, set value for rate correctly";
            return false;
        }
        
        this.errorMessage = "";
        return true;
    }

    private boolean saveData(){
        java.util.HashMap<String, String> params = new java.util.HashMap<>();
        params.put("table", "currencies");
        params.put("name", tfName.getText());
        params.put("code", tfCode.getText());
        if (cbStarts.isSelected()){
            params.put("starts_with_code", "1");
        }else{
            params.put("starts_with_code", "0");
        }
        params.put("rate", tfRate.getText());
        
        
        if (this.currency != null){
            //update the DB record
            params.put("id", String.valueOf(currency.getID()));
        }
        int currencyID;
        currencyID = Kman.getDB().updateData((this.currency == null), params);
        
        if (this.currency == null){
            if (currencyID > 0){
                //new object
                params.put("id", String.valueOf(currencyID));

                Currency.getCurrencies().add(new Currency(params));
            }else{
                //DB error
                return false;
            }
        }else{
            //updated old one
            this.currency.setFields(params);
        }
        
        return (currencyID > 0);
    }
    
    @Override
    public void setFormObject(Object _formObject){
//        super.setFormObject(_formObject);

        this.currency = (Currency)_formObject;
        if (this.currency != null){
            //this is an edit form
            tfName.setText(currency.getName());
            tfCode.setText(currency.getCode());
            cbStarts.setSelected(currency.getStartsWithCode());
            if (currency.getCode().equals(Currency.BASE_CURRENCY_CODE)){
                tfRate.setEditable(false);
                if (currency.getRate() != 1){
                    currency.setRate(1f); //base currency must be 1
                }
            }
            tfRate.setText(String.valueOf(currency.getRate()));
            lRate.setText("rate (" + currency.getRateString() + "):");
            
            //update sample text
            setSample();
        }
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }    
    
}
