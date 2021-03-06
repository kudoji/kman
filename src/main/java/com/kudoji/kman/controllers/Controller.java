package com.kudoji.kman.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 *
 * @author kudoji
 */
public class Controller implements Initializable {
    private boolean isChanged = false;
    private Stage stage;
    private Object formObject;
    @FXML private javafx.scene.text.Text tHeader;
    
    public void setHeader(String _header){
        tHeader.setText(_header);
    }
    
    public String getHeader(){
        return tHeader.getText();
    }
    
    public void setFormObject(Object _formObject){
        this.formObject = _formObject;
    }
    
    public Object getFormObject(){
        return this.formObject;
    }
    
    public void closeStage(){
        this.stage.close();
    }
    
    /**
     * Sets form's changed flag
     */
    public void setChanged(){
        this.isChanged = true;
    }
    
    /**
     * Returns form's changed flag. If form changed returns true, otherwise -false
     * 
     * @return 
     */
    public boolean isChanged(){
        return this.isChanged;
    }

    @FXML
    private void btnCancelOnAction(ActionEvent action){
        stage.close();
    }

    /**
     * Called any time Enter button is hit.
     * Can be overridden in sub-classes
     */
    public void btnEnterOnAction(){
    }
    
    /**
     * Set the stage for the controller which makes possible to attach listeners
     * Unfortunately, tHeader.getScene() in controller's initialize() return null because stage is not set at this point
     * @param _stage 
     */
    public void setStage(Stage _stage){
        if (_stage == null){
            throw new IllegalArgumentException("Stage cannot be null");
        }

        this.stage = _stage;
        this.stage.addEventHandler(
                KeyEvent.KEY_RELEASED,
                (KeyEvent ke) -> {
                    if (ke.getCode() == KeyCode.ESCAPE){
                        this.stage.close();
                    }else if (ke.getCode() == KeyCode.ENTER){
                        btnEnterOnAction();
                    }
                }
        );
    }
    
    public Stage getStage(){
        return this.stage;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }
}
