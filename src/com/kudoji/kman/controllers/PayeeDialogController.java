package com.kudoji.kman.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.kudoji.kman.models.Controller;
import com.kudoji.kman.Kman;
import com.kudoji.kman.models.Payee;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

/**
 * FXML Controller class
 *
 * @author kudoji
 */
public class PayeeDialogController extends Controller {
    //used by validate function
    private String errorMessage;
    /**
     * Set if form is opened for editing
     */
    private Payee payee = null;
    
    @FXML
    private javafx.scene.control.TextField tfId, tfName;
    private final int MAX_PAYEE_NAMELENGTH = 15;
    
    @FXML
    private void btnOKOnAction(ActionEvent event){
        if (!validateFields()){
            Kman.showErrorMessage(this.errorMessage);
        }else{
            if (!saveData()){
                Kman.showErrorMessage("Unable to save payee data");
            }else{
                super.closeStage();
            }
        }
    }
    
    private boolean validateFields(){
        tfName.setText(tfName.getText().trim());
        
        if ("".equals(tfName.getText().trim())){
            this.errorMessage = "Please, set payee name";
            return false;
        }
        
        if (tfName.getText().length() > MAX_PAYEE_NAMELENGTH){
            this.errorMessage = "Please, set payee name less than " + MAX_PAYEE_NAMELENGTH + " characters";
            return false;
        }
        
        this.errorMessage = "";
        return true;
    }
    
    private boolean saveData(){
        java.util.HashMap<String, String> params = new java.util.HashMap<>();
        params.put("table", "payees");
        params.put("name", tfName.getText());
        //add categories later...
        
        if (this.payee != null){
            //update the DB record
            params.put("id", Integer.toString(payee.getID()));
        }
        int payeeID;
        payeeID = Kman.getDB().updateData((this.payee == null), params);
        
        if (this.payee == null){
            if (payeeID > 0){
                //new object
                params.put("id", Integer.toString(payeeID));

                Payee.getPayees().add(new Payee(params));
            }else{ //possibly, DB error
                return false;
            }
        }else{
            //updated old one
            this.payee.setFields(params);
        }
        
        return (payeeID > 0);
    }
    
    @Override
    public void setFormObject(Object _formObject){
//        super.setFormObject(_formObject);
        
        this.payee = (Payee)_formObject;
        if (this.payee != null){
            //this is an edit form
            tfId.setText(Integer.toString(payee.getID()));
            tfName.setText(payee.getName());
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
