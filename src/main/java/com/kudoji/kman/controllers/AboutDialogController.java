package com.kudoji.kman.controllers;

import com.kudoji.kman.Kman;
import com.kudoji.kman.utils.Urls;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class AboutDialogController extends Controller {
    @FXML
    private ImageView iLogo;
    @FXML
    private Text tVersion;
    @FXML
    private Hyperlink hlGitHub;

    @Override
    public void btnEnterOnAction(){
        btnOKOnAction(null);
    }

    @FXML
    private void btnOKOnAction(ActionEvent action){
        super.closeStage();
    }

    @FXML
    private void hlGitHubOnAction(ActionEvent action){
        //  after clicking make it unvisited again
        hlGitHub.setVisited(false);
        //  open url
        Urls.openUrl(Kman.KMAN_GH_URL);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tVersion.setText("version " + Kman.KMAN_VERSION);
        hlGitHub.setTooltip(new Tooltip(Kman.KMAN_GH_URL));
        //  remove border for visited link
        hlGitHub.setBorder(Border.EMPTY);
        iLogo.setImage(new javafx.scene.image.Image(Kman.class.getResourceAsStream("/images/icon.png")));
    }
}
