/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kudoji.kman;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 *
 * @author kudoji
 */
public class KmanController implements Initializable {
    private TreeItem<Account> tiAccounts; //root item for all accounts
    
    @FXML
    private TreeView<Account> tvNavigation;
    @FXML
    private TreeTableView<Transaction> ttvTransactions;
    @FXML
    private javafx.scene.control.TextArea taTransactionNote;
    
    //menu actions
    @FXML
    private void miExitOnAction(ActionEvent event){
        System.exit(0);
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
        Kman.showAndWaitForm("CurrenciesDialog.fxml", "Manage currencies", null);
    }
    
    @FXML
    private void miManageCategoriesOnAction(ActionEvent event){
        Kman.showAndWaitForm("CategoriesDialog.fxml", "Manage Categories", null);
    }
    
    @FXML
    private void miPayeesManageOnAction(ActionEvent event){
        Kman.showAndWaitForm("PayeesDialog.fxml", "Manage Payees", null);
    }
    
    @FXML
    private void miImportMMEXOnAction(ActionEvent event){
        if (Kman.showConfirmation("All data will be deleted.", "Are you sure?")){
            //currencies will be kept...
            //categories will be kept...
            //transaction_types will be kept...
            Kman.showAndWaitForm("MMEXImportDialog.fxml", "mmex import", null);
        }
    }
    
    @FXML
    private void miAboutOnAction(ActionEvent event){
        System.gc();
        System.out.println("trying gc...");
    }
    
    @FXML
    private void btnTransactionInsertOnAction(ActionEvent event){
        TreeItem<Account> tiSelected = (TreeItem<Account>)tvNavigation.getSelectionModel().getSelectedItem();
        if (tiSelected == null){ //nothing is selected
            Kman.showErrorMessage("Please, select account first");
            return;
        }
        
        Account aSelected = tiSelected.getValue();
        if (aSelected.getID() < 1){ //root is selected
            Kman.showErrorMessage("Please, select particular account first");
            return;
        }
        
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        params.put("transaction", null);
        params.put("account", aSelected);
        if (Kman.showAndWaitForm("TransactionDialog.fxml", "New Transaction...", params)){
            //re-read transactions for the current account
            Transaction.populateTransactionsTable(ttvTransactions, aSelected);
            //re-read accounts objects
            Account.populateAccountsTree(tvNavigation);
        }
    }
    
    @FXML
    private void btnTransactionEditOnAction(ActionEvent event) {
        TreeItem<Transaction> tiSelected = ttvTransactions.getSelectionModel().getSelectedItem();
        if (tiSelected == null){ //nothing is selected
            Kman.showErrorMessage("Please, select a transaction first");
            return;
        }
        
        Transaction transactionSelected = tiSelected.getValue();
        if (transactionSelected.getID() < 1){ //root is selected
            Kman.showErrorMessage("Please, select a particular transaction first");
            return;
        }
        
        java.util.HashMap<String, Transaction> params = new java.util.HashMap<>();
        params.put("object", transactionSelected);
        if (Kman.showAndWaitForm("TransactionDialog.fxml", "Edit Transaction...", params)){
            //re-read transactions for the current account
            //update transations tree
            //ttvTransactions.refresh(); //this doesn't update other transactions...
            //re-read transactions for the current account
            TreeItem<Account> tiAccountSelected = (TreeItem<Account>)tvNavigation.getSelectionModel().getSelectedItem();
            if (tiAccountSelected != null){ //nothing is selected
                Account aSelected = tiAccountSelected.getValue();
                if (aSelected.getID() < 1){ //root is selected
                    Transaction.populateTransactionsTable(ttvTransactions, null);
                }else{
                    Transaction.populateTransactionsTable(ttvTransactions, aSelected);
                }
            }
            //re-read transaction note as well
            ttvTransactionsOnSelect();
            //re-read accounts objects
            Account.populateAccountsTree(tvNavigation);
        }
    }
    
    @FXML
    private void btnTransactionDeleteOnAction(ActionEvent event){
        TreeItem<Transaction> tiSelected = ttvTransactions.getSelectionModel().getSelectedItem();
        if (tiSelected == null){ //nothing is selected
            Kman.showErrorMessage("Please, select a transaction first");
            return;
        }
        
        Transaction transactionSelected = tiSelected.getValue();
        if (transactionSelected.getID() < 1){ //root is selected
            Kman.showErrorMessage("Please, select a particular transaction first");
            return;
        }

        if (!Kman.showConfirmation("The selected transaction will be deleted.", "Are you sure?")){
            return;
        }
        
        if (!Kman.getDB().startTransaction()){
            Kman.showErrorMessage("Couldn't start SQL transaction...");
            return;
        }
        
        Account account;
        switch (transactionSelected.getTypeID()){
            case TransactionType.ACCOUNT_TYPES_DEPOSIT:
                //increase all transactions after the current one
                if (!Transaction.increaseBalance(transactionSelected, Transaction.AccountTake.TO, -transactionSelected.getAmountTo())){
                    Kman.getDB().rollbackTransaction();
                    System.err.println("Unable to update transactions' balance after '" + transactionSelected.getID() + "' for accountID: " + transactionSelected.getAccountToID());
                    return;
                }
                //decrease accounts balance
                account = transactionSelected.getAccount(true);
                account.increaseBalanceCurrent(-transactionSelected.getAmountTo());
                if (!account.updateDB()){
                    Kman.getDB().rollbackTransaction();
                    System.err.println("Unable to save current balance for " + account + " account");
                    return;
                }

                break;
            case TransactionType.ACCOUNT_TYPES_WITHDRAWAL:
                if (!Transaction.increaseBalance(transactionSelected, Transaction.AccountTake.FROM, transactionSelected.getAmountFrom())){
                    Kman.getDB().rollbackTransaction();
                    System.err.println("Unable to update transactions' balance after '" + transactionSelected.getID() + "' for accountID: " + transactionSelected.getAccountFromID());
                    return;
                }
                //increase accounts balance
                account = transactionSelected.getAccount(false);
                account.increaseBalanceCurrent(transactionSelected.getAmountFrom());
                if (!account.updateDB()){
                    Kman.getDB().rollbackTransaction();
                    System.err.println("Unable to save current balance for " + account + " account");
                    return;
                }

                break;
            case TransactionType.ACCOUNT_TYPES_TRANSFER:
                //in case of transfer, transaction touches both accounts
                if (!Transaction.increaseBalance(transactionSelected, Transaction.AccountTake.FROM, transactionSelected.getAmountFrom())){
                    Kman.getDB().rollbackTransaction();
                    System.err.println("Unable to update transactions' balance after '" + transactionSelected.getID() + "' for accountID: " + transactionSelected.getAccountFromID());
                    return;
                }
                //increase accounts balance
                account = transactionSelected.getAccount(false);
                account.increaseBalanceCurrent(transactionSelected.getAmountFrom());
                if (!account.updateDB()){
                    Kman.getDB().rollbackTransaction();
                    System.err.println("Unable to save current balance for " + account + " account");
                    return;
                }

                if (!Transaction.increaseBalance(transactionSelected, Transaction.AccountTake.TO, -transactionSelected.getAmountTo())){
                    Kman.getDB().rollbackTransaction();
                    System.err.println("Unable to update transactions' balance after '" + transactionSelected.getID() + "' for accountID: " + transactionSelected.getAccountToID());
                    return;
                }
                //decrese accounts balance
                account = transactionSelected.getAccount(true);
                account.increaseBalanceCurrent(-transactionSelected.getAmountTo());
                if (!account.updateDB()){
                    Kman.getDB().rollbackTransaction();
                    System.err.println("Unable to save current balance for " + account + " account");
                    return;
                }

                break;
            default: //something is wrong
        }

        //finally, delete transaction
        java.util.HashMap<String, String> params = new java.util.HashMap<>();
        params.put("table", "transactions");
        params.put("where", "id = " + transactionSelected.getID());

        if (!Kman.getDB().deleteData(params)){
            Kman.getDB().rollbackTransaction();
            System.err.println("Unable to detele transaction with id: " + transactionSelected.getID());
            return;
        }

        TreeItem<Account> tiAccountSelected = (TreeItem<Account>)tvNavigation.getSelectionModel().getSelectedItem();
        if (tiAccountSelected != null){ //nothing is selected
            Account aSelected = tiAccountSelected.getValue();
            if (aSelected.getID() < 1){ //root is selected
                Transaction.populateTransactionsTable(ttvTransactions, null);
            }else{
                Transaction.populateTransactionsTable(ttvTransactions, aSelected);
            }
        }
        //re-read transaction note as well
        ttvTransactionsOnSelect();
        //update accounts tree (balance to accounts)
        Account.populateAccountsTree(tvNavigation);

        Kman.getDB().commitTransaction();
    }
    
    private void miAccountInsertEvent(ActionEvent _event){
        java.util.HashMap<String, Account> params = new java.util.HashMap<>();
        params.put("object", null);
        
        if (Kman.showAndWaitForm("AccountDialog.fxml", "Add Account...", params)){
            //new account is inserted
            tiAccounts.getChildren().add(new TreeItem(params.get("object")));
        }
    }
    
    private void editAccountEvent(ActionEvent event){
        TreeItem<Account> tiSelected = (TreeItem<Account>)tvNavigation.getSelectionModel().getSelectedItem();
        if (tiSelected != null){
            Account aSelected = tiSelected.getValue();
            
            if (aSelected.getID() > 0){ //real account is selected
                java.util.HashMap<String, Account> params = new java.util.HashMap<>();
                params.put("object", aSelected);
                if (Kman.showAndWaitForm("AccountDialog.fxml", "Edit Account...", params)){
                    tiSelected.setValue(null);
                    tiSelected.setValue(params.get("object"));
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
        if (selected.getID() < 1){
            Kman.showErrorMessage("Please, select a particular account (root one cannot be deleted)");
            
            return;
        }

        if (!Kman.showConfirmation("All transactions for the account will also be deleted.", "Are you sure?")){
            return;
        }
        
        java.util.HashMap<String, String> params = new java.util.HashMap<>();
        params.put("table", "accounts");
        params.put("where", "id = " + selected.getID());
        
        if (Kman.getDB().deleteData(params)){
            selected = null;
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
            
            Transaction.populateTransactionsTable(ttvTransactions, aSelected);
        }
    }
    
    /**
     * Called every time ttvTransactions is clicked
     */
    private void ttvTransactionsOnSelect(){
        TreeItem<Transaction> tiSelected = ttvTransactions.getSelectionModel().getSelectedItem();
        if (tiSelected != null){
            Transaction transactionSelected = tiSelected.getValue();
            
            taTransactionNote.setText(transactionSelected.getNotes());
        }
    }
    
    private void createDesignForTreeTableView(){
        final TreeItem<Transaction> root = new TreeItem<>(new Transaction("root"));
        root.setExpanded(true);
        
        TreeTableColumn<Transaction, String> ttcDate = new TreeTableColumn<>("date");
        ttcDate.setPrefWidth(80);
        ttcDate.setCellValueFactory((TreeTableColumn.CellDataFeatures<Transaction, String> param) -> new ReadOnlyStringWrapper(param.getValue().getValue().getDate()));
        ttvTransactions.getColumns().add(ttcDate);
        
        TreeTableColumn<Transaction, String> ttcType = new TreeTableColumn<>("type");
        ttcType.setPrefWidth(80);
        ttcType.setCellValueFactory((TreeTableColumn.CellDataFeatures<Transaction, String> param) -> new ReadOnlyStringWrapper(param.getValue().getValue().getTypeString()));
        ttvTransactions.getColumns().add(ttcType);
        
        TreeTableColumn<Transaction, String> ttcAccount = new TreeTableColumn<>("account");
        ttcAccount.setPrefWidth(150);
        ttcAccount.setCellValueFactory((TreeTableColumn.CellDataFeatures<Transaction, String> param) -> new ReadOnlyStringWrapper(param.getValue().getValue().getAccountString()));
        ttvTransactions.getColumns().add(ttcAccount);
        
        TreeTableColumn<Transaction, String> ttcCategory = new TreeTableColumn<>("category");
        ttcCategory.setPrefWidth(150);
        ttcCategory.setCellValueFactory((TreeTableColumn.CellDataFeatures<Transaction, String> param) -> new ReadOnlyStringWrapper(param.getValue().getValue().getCategoryString()));
        ttvTransactions.getColumns().add(ttcCategory);

        TreeTableColumn<Transaction, String> ttcAmount = new TreeTableColumn<>("amount");
        ttcAmount.setPrefWidth(150);
        ttcAmount.setStyle("-fx-alignment: center-right;");
        ttcAmount.setCellValueFactory((TreeTableColumn.CellDataFeatures<Transaction, String> param) -> new ReadOnlyStringWrapper(param.getValue().getValue().getAmountString()));
        ttvTransactions.getColumns().add(ttcAmount);

        TreeTableColumn<Transaction, String> ttcBalance = new TreeTableColumn<>("balance");
        ttcBalance.setPrefWidth(150);
        ttcBalance.setStyle("-fx-alignment: center-right;");
        ttcBalance.setCellValueFactory((TreeTableColumn.CellDataFeatures<Transaction, String> param) -> new ReadOnlyStringWrapper(param.getValue().getValue().getBalanceString()));
        ttvTransactions.getColumns().add(ttcBalance);

        ttvTransactions.setRoot(root);
        ttvTransactions.setShowRoot(false);
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
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        tiAccounts = Account.populateAccountsTree(tvNavigation);
        tvNavigation.getSelectionModel().select(tiAccounts);
        tvNavigation.setCellFactory((TreeView<Account> param) -> {
            return new TreeViewCellFactory();
        });
        
        createDesignForTreeTableView();
        Transaction.populateTransactionsTable(ttvTransactions, null); //show all transactions
        
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
        
        ttvTransactions.setOnMouseClicked((MouseEvent event) ->{
            if (event.getButton() == MouseButton.PRIMARY){
                switch (event.getClickCount()){
                    case 1: //single click
                        ttvTransactionsOnSelect();
                        break;
                    case 2: //double click
                        btnTransactionEditOnAction(null);
                        break;
                }
            }
        });
    }    
}