package com.kudoji.kman;

import com.kudoji.kman.controllers.Controller;
import com.kudoji.kman.controllers.KmanController;
import com.kudoji.kman.models.*;
import com.kudoji.kman.utils.DB;
import com.kudoji.kman.utils.Settings;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kudoji
 */
public class Kman extends Application {
    public final static String KMAN_NAME = "kman";
    public final static String KMAN_DB_NAME_DEFAULT = "kman.kmd";
    //  current app version
    public final static String KMAN_VERSION = "0.9-SNAPSHOT";
    //  github repository url
    public final static String KMAN_GH_URL = "https://github.com/kudoji/kman/";

    private static final Logger log = Logger.getLogger(Kman.class.getName());

    private static DB kmanDB;
    private static Stage kmanStage;
    //  save settings
    private static Settings settings;

    public Kman(){
    }
    
    public static DB getDB(){
        return Kman.kmanDB;
    }

    public static Settings getSettings(){
        return Kman.settings;
    }
    
    /**
     * Shows alert error message
     * @param _errorMessage 
     */
    public static void showErrorMessage(String _errorMessage){
        javafx.scene.control.Alert aMessage = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        aMessage.setTitle("Error");
        aMessage.setHeaderText(null);
        aMessage.setContentText(_errorMessage);
        aMessage.showAndWait();
    }
    
    /**
     * Shows confirmation dialog
     * @param _header
     * @param _content
     * @return true is OK button was pressed, otherwise, false is returned
     */
    public static boolean showConfirmation(String _header, String _content){
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Please, confirm the action");
        alert.setHeaderText(_header);
        alert.setContentText(_content);
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        
        return (result.get() == javafx.scene.control.ButtonType.OK);
    }
    
    /**
     * Shows information dialog
     * @param _message
     * @return true is OK button was pressed, otherwise, false is returned
     */
    public static boolean showInformation(String _message){
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(_message);
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        
        return (result.get() == javafx.scene.control.ButtonType.OK);
    }
    
    /**
     * Creates and shows a form
     * @param _fxmlFile
     * @param _title 
     * @param _formObject null if form is new, otherwise send object you edit form for
     * @return 
     */
    public static boolean showAndWaitForm(String _fxmlFile, String _title, Object _formObject){
        boolean result = false;
        
        Stage stage = new Stage();
        try{
            FXMLLoader loader = new FXMLLoader(Kman.class.getResource(_fxmlFile));
            Parent root = loader.load();
            Controller controller = loader.getController();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setResizable(false);
            controller.setStage(stage);
            controller.setFormObject(_formObject);

            stage.setTitle(_title);
            controller.setHeader(_title);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
//            stage.initOwner(stage);
            stage.showAndWait();
            
            result = controller.isChanged();
        }catch (Exception e){
            log.log(Level.WARNING, e.getMessage(), e);
            result = false;
        }
        
        return result;
    }

    /**
     * Selects item with _id in combobox
     *
     * @param _combobox
     * @param _id
     */
    @SuppressWarnings("unchecked")
    public static void selectItemInCombobox(javafx.scene.control.ComboBox _combobox, int _id){
        if (_id <= 0) return;

        for (Object item: _combobox.getItems()){
            if (item instanceof Currency){//looks like it's not possible to cast dynamically....
                Currency itemClass = (Currency)item;

                if (itemClass.getID() == _id){
                    _combobox.getSelectionModel().select(item);
                    break;
                }
            }else if (item instanceof TransactionType){
                TransactionType itemClass = (TransactionType)item;

                if (itemClass.getId() == _id){
                    _combobox.getSelectionModel().select(item);
                    break;
                }
            }else if (item instanceof Account){
                Account itemClass = (Account)item;

                if (itemClass.getId() == _id){
                    _combobox.getSelectionModel().select(item);
                    break;
                }
            }else if (item instanceof Payee){
                Payee itemClass = (Payee)item;

                if (itemClass.getId() == _id){
                    _combobox.getSelectionModel().select(item);
                    break;
                }
            }else if (item instanceof Category){
                Category itemClass = (Category)item;

                if (itemClass.getId() == _id){
                    _combobox.getSelectionModel().select(item);
                    break;
                }
            }
        }
    }

    public static void setWindowTitle(){
        Kman.kmanStage.setTitle(KMAN_NAME + " [" + Kman.getDB().getFile() + "]");
    }

    @Override
    public void start(Stage stage) throws Exception {
        Kman.kmanStage = stage;

        settings = new Settings(stage);
        settings.readSettings();

        kmanDB = new DB(settings.getDBName());
        kmanDB.setLogLevel(Level.ALL);
        kmanDB.connect();
        kmanDB.createAllTables(true);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Kman.fxml"));
        Parent root = loader.load();
        KmanController kmanController = loader.getController();
        Scene scene = new Scene(root);

        setWindowTitle();
        stage.setScene(scene);
        stage.getIcons().add(new javafx.scene.image.Image(Kman.class.getResourceAsStream("/images/icon.png")));
        stage.show();

        //  have to apply divider position after form is shown...
        kmanController.setDividerPosition(settings.getWindowDividerPosition());
    }

    @Override
    public void stop() throws Exception{
        settings.saveSettings();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}