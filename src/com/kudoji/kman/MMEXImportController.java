/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kudoji.kman;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

/**
 * FXML Controller class
 *
 * @author kudoji
 */
public class MMEXImportController extends Controller {
    @FXML private TextField tfMMEXFile;
    
    @FXML
    private void btnMMEXFileOnAction(ActionEvent event){
        FileChooser fcMMEXFile = new FileChooser();
        fcMMEXFile.setTitle("Open mmex database file...");
        fcMMEXFile.setInitialDirectory(new File(System.getProperty("user.home")));
        fcMMEXFile.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("mmex file", "*.mmb"),
                new FileChooser.ExtensionFilter("all files", "*.*"));
        File fSelected = fcMMEXFile.showOpenDialog(null);
        if (fSelected != null){
            tfMMEXFile.setText(fSelected.getPath());
            
            MMEXImport mmexImport = new MMEXImport(fSelected.getPath(), true);
            mmexImport.loadData();
            mmexImport.close();
            
            if (mmexImport.getStatus()){
                //no errors
                Kman.showInformation("Data moved sucessfully. Restart application, please");
            }else{
                Kman.showErrorMessage("Cannot move data");
            }
            mmexImport = null;
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
