package com.kudoji.kman.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.kudoji.kman.models.Category;
import com.kudoji.kman.models.Controller;
import com.kudoji.kman.Kman;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

/**
 * FXML Controller class
 *
 * @author kudoji
 */
public class CategoryDialogController extends Controller {
    private java.util.HashMap<String, Category> formObject;
    private Category categoryParent, category;
    //used by validate function
    private String errorMessage;
    private final int MAX_CATEGORY_NAME_LENGTH = 15;
    
    @FXML private javafx.scene.control.TextField tfId, tfName;
    @FXML private javafx.scene.control.ComboBox<Category> cbParent;
    
    @FXML
    public void btnOKOnAction(ActionEvent event){
        if (!validateFields()){
            Kman.showErrorMessage(this.errorMessage);
        }else{
            if (!saveData()){
                Kman.showErrorMessage("Unable to save category data");
            }else{
                this.formObject.put("parent", this.categoryParent);
                this.formObject.put("object", this.category);
                super.setChanged();
                super.closeStage();
            }
        }
    }
    
    private boolean validateFields(){
        tfName.setText(tfName.getText().trim());
        
        if ("".equals(tfName.getText().trim())){
            this.errorMessage = "Please, specify category name";
            return false;
        }
        
        if (tfName.getText().length() > MAX_CATEGORY_NAME_LENGTH){
            this.errorMessage = "Please, set category name less than " + MAX_CATEGORY_NAME_LENGTH + " characters";
            return false;
        }
        
        this.errorMessage = "";
        return true;
    }
    
    private boolean saveData(){
        java.util.HashMap<String, String> params = new java.util.HashMap<>();
        params.put("table", "categories");
        params.put("name", tfName.getText());
        String parentID = (Integer.toString(this.categoryParent.getID()));
        if (parentID.equals("0")){
            //to avoid "FOREIGN KEY constraint failed" sql error
            parentID = null;
        }
        params.put("categories_id", parentID);
        
        if (this.category != null){ //edit existed account, need to put its id to update sql query
            params.put("id", Integer.toString(this.category.getID()));
        }
        int categoryID;
        //use insert since the account is new (this.isNew = true)
        //use update since the account is existed (this.isNew = false)
        categoryID = Kman.getDB().updateData(this.category == null, params);

        if (this.category == null){
            if (categoryID > 0) {//new category have been created
                params.put("id", Integer.toString(categoryID));
                this.category = new Category(params);
            }else{ //possible DB error
                return false;
            }
        }else{
            this.category.setFields(params); //update object variables
        }
        
        return (categoryID > 0);
    }
    
    @Override
    public void setFormObject(Object _formObject){
        //in this case _formObject is a hashmap
        this.formObject = (java.util.HashMap<String, Category>)_formObject;
        
        this.categoryParent = formObject.get("parent");
        this.category = formObject.get("object");
        
        cbParent.getItems().add(this.categoryParent);
        cbParent.getSelectionModel().select(this.categoryParent);
        cbParent.setDisable(true);
        if (this.category != null){ //edit category form
            tfId.setText(Integer.toString(this.category.getID()));
            tfName.setText(this.category.getName());
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
