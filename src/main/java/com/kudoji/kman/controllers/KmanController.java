package com.kudoji.kman.controllers;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;

import com.kudoji.kman.models.Account;
import com.kudoji.kman.Kman;
import com.kudoji.kman.models.Transaction;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 *
 * @author kudoji
 */
public class KmanController implements Initializable {
    private TreeItem<Account> tiAccounts; //root item for all accounts

    @FXML private VBox vbMenu;
    @FXML private MenuBar mbApplication;
    @FXML
    private TreeView<Account> tvNavigation;
    @FXML
    private TableView<Transaction> tvTransactions;
    @FXML
    private javafx.scene.control.TextArea taTransactionNote;
    
    //  menu actions
    @FXML
    private void miNewDatabase(ActionEvent event){
        FileChooser fcKman = new FileChooser();
        fcKman.setTitle("Choose database file to create...");
        fcKman.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("kman database", "*.kmd")
        );
        java.io.File fSelected = fcKman.showSaveDialog(null);
        if (fSelected != null){
            if (fSelected.exists()){
                //  user selected file which is already existed
                if (!fSelected.delete()){
                    Kman.showErrorMessage("Cannot re-write file: " + fSelected.getPath());
                    return;
                }
            }

            try{
                fSelected.createNewFile();

                Kman.getDB().close();
                Kman.getDB().connect(fSelected.getPath());
                Kman.getDB().createAllTables(true);

                clearAppScreen();
                Kman.setWindowTitle();
            }catch (IOException e){
                Kman.showErrorMessage("Cannot create file: " + fSelected.getPath());
            }
        }
    }

    @FXML
    private void miOpenDatabase(ActionEvent event){
        FileChooser fcKman = new FileChooser();
        fcKman.setTitle("Choose database file to open...");
        fcKman.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("kman database", "*.kmd")
        );
        java.io.File fSelected = fcKman.showOpenDialog(null);
        if (fSelected != null && fSelected.exists()){
            Kman.getDB().close();
            if (!Kman.getDB().connect(fSelected.getPath())){
                Kman.showErrorMessage("Cannot open file: " + fSelected.getPath());
            }

            clearAppScreen();
            Kman.setWindowTitle();
        }
    }

    @FXML
    private void miSaveDatabaseAs(ActionEvent event){
        FileChooser fcKman = new FileChooser();
        fcKman.setTitle("Save database file as...");
        fcKman.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("kman database", "*.kmd")
        );
        java.io.File fSelected = fcKman.showSaveDialog(null);
        if (fSelected != null){
            try{
                Files.copy(java.nio.file.Paths.get(Kman.getDB().getFile()), fSelected.toPath(), StandardCopyOption.REPLACE_EXISTING);

                Kman.getDB().close();
                Kman.getDB().connect(fSelected.getPath());

                clearAppScreen();
                Kman.setWindowTitle();
            }catch (IOException e){
                Kman.showErrorMessage("Cannot create file: " + fSelected.getPath());
            }
        }
    }

    @FXML
    private void miExitOnAction(ActionEvent event){
        //  System.exit(0); is very hard which causes skipping Application.stop() method
        Platform.exit();
    }
    
    @FXML
    private void miAccountInsertOnAction(ActionEvent event){
        miAccountInsertEvent(event);
    }
    
    @FXML
    private void miAccountEditOnAction(ActionEvent event){
        editAccountEvent(event);
    }
    
    @FXML
    private void miDeleteAccountOnAction(ActionEvent event){
        deleteAccountEvent(event);
    }
    
    @FXML
    private void miCurrenciesManageOnAction(ActionEvent event){
        Kman.showAndWaitForm("/views/CurrenciesDialog.fxml", "Manage currencies", null);
    }
    
    @FXML
    private void miManageCategoriesOnAction(ActionEvent event){
        Kman.showAndWaitForm("/views/CategoriesDialog.fxml", "Manage Categories", null);
    }
    
    @FXML
    private void miPayeesManageOnAction(ActionEvent event){
        Kman.showAndWaitForm("/views/PayeesDialog.fxml", "Manage Payees", null);
    }
    
    @FXML
    private void miImportMMEXOnAction(ActionEvent event){
        if (Kman.showConfirmation("All data will be deleted.", "Are you sure?")){
            //currencies will be kept...
            //categories will be kept...
            //transaction_types will be kept...
            Kman.showAndWaitForm("/views/MMEXImportDialog.fxml", "mmex import", null);
        }
    }
    
    @FXML
    private void miAboutOnAction(ActionEvent event){
        Kman.showAndWaitForm("/views/AboutDialog.fxml", "About kman", null);
    }
    
    @FXML
    private void btnTransactionInsertOnAction(ActionEvent event){
        TreeItem<Account> tiSelected = (TreeItem<Account>)tvNavigation.getSelectionModel().getSelectedItem();
        if (tiSelected == null){ //nothing is selected
            Kman.showErrorMessage("Please, select account first");
            return;
        }
        
        Account aSelected = tiSelected.getValue();
        if (aSelected.getId() < 1){ //root is selected
            Kman.showErrorMessage("Please, select particular account first");
            return;
        }
        
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        params.put("transaction", null);
        params.put("account", aSelected);
        if (Kman.showAndWaitForm("/views/TransactionDialog.fxml", "New Transaction...", params)){
            //  transaction successfully inserted
            //  nothing else is needed to be done here
        }
    }
    
    @FXML
    private void btnTransactionEditOnAction(ActionEvent event) {
        Transaction transactionSelected = tvTransactions.getSelectionModel().getSelectedItem();
        if (transactionSelected == null){ //nothing is selected
            Kman.showErrorMessage("Please, select a transaction first");
            return;
        }
        
        java.util.HashMap<String, Transaction> params = new java.util.HashMap<>();
        params.put("object", transactionSelected);
        if (Kman.showAndWaitForm("/views/TransactionDialog.fxml", "Edit Transaction...", params)){
            //re-read transaction note as well
            tvTransactionsOnSelect();
        }
    }
    
    @FXML
    private void btnTransactionDeleteOnAction(ActionEvent event){
        Transaction transactionSelected = tvTransactions.getSelectionModel().getSelectedItem();
        if (transactionSelected == null){ //nothing is selected
            Kman.showErrorMessage("Please, select a transaction first");
            return;
        }

        if (!Kman.showConfirmation("The selected transaction will be deleted.", "Are you sure?")){
            return;
        }

        //  find selected account
        TreeItem<Account> tiAccountSelected = (TreeItem<Account>)tvNavigation.getSelectionModel().getSelectedItem();
        if (tiAccountSelected == null) { // no account is selected
            //  this error should never happen...
            //  due to transactions exist for selected account only
            Kman.showErrorMessage("Unable to delete the transaction for particular account");
        }

        if (!transactionSelected.delete(true)){
            Kman.showErrorMessage("Unable to delete the transaction");
            return;
        }
        //re-read transaction note as well
        tvTransactionsOnSelect();
    }
    
    private void miAccountInsertEvent(ActionEvent _event){
        java.util.HashMap<String, Account> params = new java.util.HashMap<>();
        params.put("object", null);

        if (Kman.showAndWaitForm("/views/AccountDialog.fxml", "Add Account...", params)){
            //new account is inserted
            tiAccounts.getChildren().add(new TreeItem<>(params.get("object")));
        }
    }
    
    private void editAccountEvent(ActionEvent event){
        TreeItem<Account> tiSelected = tvNavigation.getSelectionModel().getSelectedItem();
        if (tiSelected != null){
            Account aSelected = tiSelected.getValue();
            
            if (aSelected.getId() > 0){ //real account is selected
                java.util.HashMap<String, Account> params = new java.util.HashMap<>();
                params.put("object", aSelected);
                if (Kman.showAndWaitForm("/views/AccountDialog.fxml", "Edit Account...", params)){
                    //  TreeItem will be automatically updated due to listeners

                    //  the silly code below not needed anymore
//                    tiSelected.setValue(null);
//                    tiSelected.setValue(params.get("object"));
                }
            }
        }
    }
    
    private void deleteAccountEvent(ActionEvent event){
        TreeItem<Account> tiSelected = tvNavigation.getSelectionModel().getSelectedItem();
        if (tiSelected == null){
            Kman.showErrorMessage("Please, select an account first");
            
            return;
        }
        
        Account selected = tiSelected.getValue();
        if (selected.getId() < 1){
            Kman.showErrorMessage("Please, select a particular account (root one cannot be deleted)");
            
            return;
        }

        if (!Kman.showConfirmation("All transactions for the account will also be deleted.", "Are you sure?")){
            return;
        }

        if (selected.delete()){
            tiAccounts.getChildren().remove(tiSelected);
            //select the root node
            tvNavigation.getSelectionModel().select(tiAccounts);
            //update corresponded transactions
            tvNavigationOnSelect();
        }
    }
    
    /**
     * Called every time tvNavigation is clicked
     */
    private void tvNavigationOnSelect(){
        TreeItem<Account> tiSelected = (TreeItem<Account>)tvNavigation.getSelectionModel().getSelectedItem();
        if (tiSelected != null){
            Account aSelected = tiSelected.getValue();
            
            Transaction.populateTransactionsTable(tvTransactions, aSelected);
            //  update transaction note as well
            tvTransactionsOnSelect();
        }
    }
    
    /**
     * Called every time tvTransactions is selected by mouse or keyboard
     */
    private void tvTransactionsOnSelect(){
        Transaction transactionSelected = tvTransactions.getSelectionModel().getSelectedItem();
        if (transactionSelected != null){
            taTransactionNote.setText(transactionSelected.getNotes());
        }else{
            taTransactionNote.setText("");
        }
    }
    
    private void createDesignForTableView(){
        TableColumn<Transaction, String> tcDate = new TableColumn<>("date");
        tcDate.setPrefWidth(100);
        tcDate.setCellValueFactory(new PropertyValueFactory<Transaction, String>("date"));
        tvTransactions.getColumns().add(tcDate);
        
        TableColumn<Transaction, String> tcType = new TableColumn<>("type");
        tcType.setPrefWidth(80);
        tcType.setCellValueFactory(new PropertyValueFactory<Transaction, String>("typeUserFormat"));
        tvTransactions.getColumns().add(tcType);
        
        TableColumn<Transaction, String> tcAccount = new TableColumn<>("account");
        tcAccount.setPrefWidth(150);
        tcAccount.setCellValueFactory(new PropertyValueFactory<Transaction, String>("accountUserFormat"));
        tvTransactions.getColumns().add(tcAccount);
        
        TableColumn<Transaction, String> tcCategory = new TableColumn<>("category");
        tcCategory.setPrefWidth(150);
        tcCategory.setCellValueFactory(new PropertyValueFactory<Transaction, String>("categoryUserFormat"));
        tvTransactions.getColumns().add(tcCategory);

        TableColumn<Transaction, String> tcAmount = new TableColumn<>("amount");
        tcAmount.setPrefWidth(150);
        tcAmount.setStyle("-fx-alignment: center-right;");
        tcAmount.setCellValueFactory(new PropertyValueFactory<Transaction, String>("amountUserFormat"));
        tvTransactions.getColumns().add(tcAmount);

        TableColumn<Transaction, String> tcBalance = new TableColumn<>("balance");
        tcBalance.setPrefWidth(150);
        tcBalance.setStyle("-fx-alignment: center-right;");
        tcBalance.setCellValueFactory(new PropertyValueFactory<Transaction, String>("balanceUserFormat"));
        tvTransactions.getColumns().add(tcBalance);
    }
    
    private final class TreeViewCellFactory extends TreeCell<Account>{
        private final ContextMenu cmMenu = new ContextMenu();

        public TreeViewCellFactory(){
            MenuItem miAccountInsert = new MenuItem("add new account...");
            cmMenu.getItems().add(miAccountInsert);
            miAccountInsert.setOnAction((ActionEvent event) -> {
                miAccountInsertEvent(event);
            });
        }

        @Override
        public void updateItem(Account item, boolean empty){
            super.updateItem(item, empty);

            if (empty){
                setText(null);
            }else{
                setText(item.toString());
                
                if (!isEditing()){
                    if (getTreeItem().isLeaf() && (cmMenu.getItems().size() == 1) ) { //avoid adding menus twice
                        //particular account
                        MenuItem miAccountEdit = new MenuItem("edit account...");
                        miAccountEdit.setOnAction((event) -> {
                            editAccountEvent(event);
                        });

                        MenuItem miAccountDelete = new MenuItem("delete account...");
                        miAccountDelete.setOnAction((event) -> {
                            deleteAccountEvent(event);
                        });

                        cmMenu.getItems().addAll(miAccountEdit, miAccountDelete);
                        
                    }
                    setContextMenu(cmMenu);
                }
            }
        }
    }

    /**
     * Does all necessary operations for clearing app's screen
     */
    private void clearAppScreen(){
        tiAccounts = Account.populateAccountsTree(tvNavigation);
        tvNavigation.getSelectionModel().select(tiAccounts);
        tvTransactions.getItems().clear();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //  check whether is it Mac OS X or not
        if (System.getProperty("os.name").toLowerCase().contains("mac")){
            //  it is, use global menu
            mbApplication.setUseSystemMenuBar(true);
            //  hide the container the menu is
            vbMenu.setMaxHeight(0);
        }
        tiAccounts = Account.populateAccountsTree(tvNavigation);
        tvNavigation.getSelectionModel().select(tiAccounts);
        tvNavigation.setCellFactory((TreeView<Account> param) -> {
            return new TreeViewCellFactory();
        });
        
        createDesignForTableView();
        Transaction.populateTransactionsTable(tvTransactions, null); //show all transactions
        
        tvNavigation.setOnMouseClicked((MouseEvent event) -> {
            if (event.getButton() == MouseButton.PRIMARY){
                switch (event.getClickCount()){
                    case 1: //single click - simple select
                        tvNavigationOnSelect();
                        break;
                    case 2:
                        editAccountEvent(null); //double click
                        break;
                }
            }
        });

        //  update transactions any time user pressed navigation key
        tvNavigation.setOnKeyReleased(event -> {
            if (event.getCode().isNavigationKey()){
                tvNavigationOnSelect();
            }
        });
        
        tvTransactions.setOnMouseClicked((MouseEvent event) ->{
            if (event.getButton() == MouseButton.PRIMARY){
                switch (event.getClickCount()){
                    case 1: //single click
                        tvTransactionsOnSelect();
                        break;
                    case 2: //double click
                        btnTransactionEditOnAction(null);
                        break;
                }
            }
        });

        //  update note every time user use navigational keys
        //  OnKeyPressed cannot be user because current transaction is not selected yet
        //  and note data is taken from previous transaction
        tvTransactions.setOnKeyReleased(event -> {
            if (event.getCode().isNavigationKey()){
                tvTransactionsOnSelect();
            }
        });
    }
}