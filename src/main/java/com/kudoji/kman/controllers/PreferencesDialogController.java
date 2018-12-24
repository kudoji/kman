package com.kudoji.kman.controllers;

import com.kudoji.kman.Kman;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

import java.net.URL;
import java.util.ResourceBundle;

public class PreferencesDialogController extends Controller {
    @FXML
    private CheckBox cbSaveWindowPosition;

    @FXML
    private void btnOKOnAction(ActionEvent action){
        Kman.getSettings().setSaveWindowPosition(cbSaveWindowPosition.isSelected());
        super.closeStage();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbSaveWindowPosition.setSelected(Kman.getSettings().getSaveWindowPosition());
    }
}
