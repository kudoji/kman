package com.kudoji.kman.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.kudoji.kman.models.Category;
import com.kudoji.kman.models.Controller;
import com.kudoji.kman.Kman;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * FXML Controller class
 *
 * @author kudoji
 */
public class CategoriesDialogController extends Controller {
    private java.util.HashMap<String, Category> formObject = null; //keeps instance of selected category
    
    @FXML private TreeView<Category> tvCategories;
    
    @FXML
    private void btnCategoryInsertOnAction(ActionEvent event){
        java.util.HashMap<String, Category> params = new java.util.HashMap<>();

        TreeItem<Category> tiParent = tvCategories.getSelectionModel().getSelectedItem();
        if (tiParent != null){
            Category category = tiParent.getValue();
            params.put("parent", category);
        }else{ //nothing is selected
            tiParent = tvCategories.getRoot();
            params.put("parent", tiParent.getValue());
        }
        params.put("object", null);
        
        if (Kman.showAndWaitForm("views/CategoryDialog.fxml", "Add category...", params)){
            //form changed which means in this case that new category is created
            //new pategory can be retreived from formObject
            //parent is still the same (it cannot be changed in the CategotyDialog form
            tiParent.getChildren().add(new TreeItem(params.get("object")));
        }
    }
    
    @FXML
    private void btnCategoryEditOnAction(ActionEvent event){
        TreeItem<Category> tiCategory = tvCategories.getSelectionModel().getSelectedItem();
        if (tiCategory == null){
            Kman.showErrorMessage("Please, select a category first");
            
            return;
        }
        
        Category category = tiCategory.getValue();
        if (category.getId() < 1){
            Kman.showErrorMessage("Please, select a particular category (root one is not editable)");
            
            return;
        }

        java.util.HashMap<String, Category> params = new java.util.HashMap<>();
        params.put("parent", tiCategory.getParent().getValue());
        params.put("object", category);

        if (Kman.showAndWaitForm("views/CategoryDialog.fxml", "Edit category...", params)){
            //category is updated, let's force TreeItem to be updated also
            //dirty trick, I know...
            tiCategory.setValue(null);
            tiCategory.setValue(params.get("object"));
        }
    }
    
    @FXML
    private void btnCategoryDeleteOnAction(ActionEvent event){
        TreeItem<Category> tiCategory = tvCategories.getSelectionModel().getSelectedItem();
        if (tiCategory == null){
            Kman.showErrorMessage("Please, select a category first");
            
            return;
        }
        
        Category category = tiCategory.getValue();
        if (category.getId() < 1){
            Kman.showErrorMessage("Please, select a particular category (root one cannot be deleted)");
            
            return;
        }
        
        if (!Kman.showConfirmation("All sub-categories and transactions with the category will also be deleted.", "Are you sure?")){
            return;
        }
        
        java.util.HashMap<String, String> params = new java.util.HashMap<>();
        params.put("table", "categories");
        params.put("where", "id = " + category.getId());
        
        if (Kman.getDB().deleteData(params)){
            category = null;
            tiCategory.getParent().getChildren().remove(tiCategory); //funny
        }
        
    }
    
    @Override
    public void setFormObject(Object _formObject){
        this.formObject = (java.util.HashMap<String, Category>)_formObject;

        int cid = 0;
        if (this.formObject != null && this.formObject.get("object") != null){
            //  need to select this category in the list
            cid = this.formObject.get("object").getId();
        }
        Category.populateCategoriesTree(tvCategories, cid);

        tvCategories.setOnMouseClicked((MouseEvent event) -> {
            if ( (event.getButton() == MouseButton.PRIMARY) && (event.getClickCount() == 2) ){//double click
                TreeItem<Category> tiSelected = (TreeItem<Category>)tvCategories.getSelectionModel().getSelectedItem();
                if (tiSelected != null){
                    Category category = tiSelected.getValue();
                    if (category.getId() > 0){//selected NOT root
                        if (this.formObject != null){//opened as a select form
                            this.formObject.put("object", category);
                            //mark the form as being changed
                            super.setChanged();
                            //close stage
                            super.closeStage();
                        }else{//opened as a regular form
                            btnCategoryEditOnAction(null);
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }
    
}
