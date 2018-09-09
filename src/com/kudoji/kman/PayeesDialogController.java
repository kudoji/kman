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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;

/**
 * FXML Controller class
 *
 * @author kudoji
 */
public class PayeesDialogController extends Controller {
    @FXML private javafx.scene.control.TableView<Payee> tvPayees;
    @FXML private javafx.scene.control.TableColumn<Payee, String> tcName;
        
    @FXML
    private void btnPayeeInsertOnAction(ActionEvent action){
        Kman.showAndWaitForm("PayeeDialog.fxml", "Add new payee...", null);
    }
    
    @FXML
    private void btnPayeeEditOnAction(ActionEvent action){
        Payee payeeSelected = tvPayees.getSelectionModel().getSelectedItem();
        if (payeeSelected == null){
            Kman.showErrorMessage("Please, select a payee first");
            
            return;
        }
        
        Kman.showAndWaitForm("PayeeDialog.fxml", "Edit payee...", payeeSelected);
    }
    
    @FXML
    private void btnPayeeDeleteOnAction(ActionEvent action){
        Payee payeeSelected = tvPayees.getSelectionModel().getSelectedItem();
        if (payeeSelected == null){
            Kman.showErrorMessage("Please, select a payee first");
            
            return;
        }

        if (!Kman.showConfirmation("Transactions with the payee will be also deleted.", "Are you sure?")){
            return;
        }
        
        java.util.HashMap<String, String> params = new java.util.HashMap<>();
        params.put("table", "payees");
        params.put("where", "id = " + payeeSelected.getID());
        
        if (Kman.getDB().deleteData(params)){
            Payee.getPayees().remove(payeeSelected);
        }
    }
    
    @FXML
    private void btnCloseOnAction(ActionEvent action){
        super.closeStage();
    }
    
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        tvPayees.setEditable(false);
        
        tcName.setCellValueFactory(new PropertyValueFactory<>("name"));
//        tcName.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.<Payee>forTableColumn());
//        tcName.setOnEditCommit((javafx.scene.control.TableColumn.CellEditEvent<Payee, String> ceEvent) -> {
//            ((Payee)ceEvent.getTableView().getItems().get(ceEvent.getTablePosition().getRow()) ).setName(ceEvent.getNewValue());
//        });

        tvPayees.setItems(Payee.getPayees());

        tvPayees.setOnMouseClicked((event) -> {
            if ( (event.getButton() == MouseButton.PRIMARY) && (event.getClickCount() == 2) ){
                btnPayeeEditOnAction(null);
            }
        });
    }   
}