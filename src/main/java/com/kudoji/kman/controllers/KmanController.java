package com.kudoji.kman.controllers;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;

import com.kudoji.kman.enums.ReportPeriod;
import com.kudoji.kman.models.Account;
import com.kudoji.kman.Kman;
import com.kudoji.kman.models.Category;
import com.kudoji.kman.models.Payee;
import com.kudoji.kman.models.Transaction;
import com.kudoji.kman.reports.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 *
 * @author kudoji
 */
public class KmanController implements Initializable {
    private TreeItem<Account> tiAccounts; //root item for all accounts

    @FXML
    private SplitPane spMainContainer;
    @FXML private VBox vbMenu;
    @FXML private MenuBar mbApplication;
    @FXML
    private TreeView<Account> tvNavigation;
    @FXML
    private TableView<Transaction> tvTransactions;
    @FXML
    private javafx.scene.control.TextArea taTransactionNote;

    @FXML
    private Label lbTransactions; //    shows amount of filtered or total transactions
    @FXML
    private TextField tfFilter;
    //  used along with tfFilter
    private FilteredList<Transaction> transactionsFiltered;

    //**************************************  reports tab  **************************************//
    @FXML
    private Tab tabReports, tabStats, tabAccounts, tabPayees, tabCategories;
    @FXML
    private ComboBox<ReportPeriod> cbReportsPeriod;
    @FXML
    private DatePicker dpReportsFrom;
    @FXML
    private DatePicker dpReportsTo;
    /**
     * Container for statistics report
     */
    @FXML
    private AnchorPane apReportsStats;
    @FXML
    private CheckBox cbxReportsAccountFilter, cbxReportsPayeeFilter, cbxReportsCategoryFilter;
    @FXML
    private ComboBox<Account> cbReportsAccounts;
    @FXML
    private ComboBox<Payee> cbReportsPayees;
    @FXML
    private ComboBox<Category> cbReportsCategories;
    @FXML
    private TreeTableView<ReportRow> ttvReportsAccounts, ttvReportsPayees, ttvReportsCategories;
    //**************************************  /reports tab  **************************************//

    
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
    private void miPreferencesOnAction(ActionEvent event){
        Kman.showAndWaitForm("/views/PreferencesDialog.fxml", "kman preferences", null);
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

            //  drop transactions' filter
            tfFilter.setText("");

            Transaction.populateTransactionsTable(tvTransactions, aSelected);

            //  re-create filtered list based on transactions list
            transactionsFiltered = new FilteredList<>(tvTransactions.getItems());
            //  set transactions list based on filtered which is based on filter
            tvTransactions.setItems(transactionsFiltered);

            //  show current amount of transactions
            lbTransactions.setText(Integer.toString(tvTransactions.getItems().size()));
            transactionsFiltered.addListener((ListChangeListener<Transaction>) c -> {
                //  update transactions' amount any time changes occur
                lbTransactions.setText(Integer.toString(tvTransactions.getItems().size()));
            });

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
     * Used to prepare app's window for another DB file
     */
    private void clearAppScreen(){
        tiAccounts = Account.populateAccountsTree(tvNavigation);
        tvNavigation.getSelectionModel().select(tiAccounts);
        //  bug #58
        //  cannot be used here since tvTransactions contains FilteredList which doesn't have clear()
        // method implementation
//        tvTransactions.getItems().clear();
        tvTransactions.setItems(null);
        taTransactionNote.setText("");
    }

    //**************************************  reports tab  **************************************//
    @FXML
    private void tabReportsOnSelectionChanged(javafx.event.Event value){
        if (tabReports.isSelected()){
            //  reports tab is selected
            if (cbReportsPeriod.getItems().isEmpty()){
                cbReportsPeriod.setItems(FXCollections.observableArrayList(ReportPeriod.values()));
                cbReportsPeriod.getSelectionModel().select(ReportPeriod.THISMONTH);
            }

            if (cbReportsAccounts.getItems().isEmpty()){
                cbReportsAccounts.setItems(Account.getAccounts());
                //  select first value to make sure that ComboBox has selected value
                if (cbReportsAccounts.getItems().size() > 0)
                    cbReportsAccounts.getSelectionModel().select(0);
            }

            if (cbReportsPayees.getItems().isEmpty()){
                cbReportsPayees.setItems(Payee.getPayees());
                //  select first value to make sure that ComboBox has selected value
                if (cbReportsPayees.getItems().size() > 0)
                    cbReportsPayees.getSelectionModel().select(0);
            }

            if (cbReportsCategories.getItems().isEmpty()){
                cbReportsCategories.setItems(Category.getCategories());
                //  select first value to make sure that ComboBox has a selected value
                if (cbReportsCategories.getItems().size() > 0)
                    cbReportsCategories.getSelectionModel().select(0);
            }
        }
    }

    @FXML
    private void cbReportsPeriodOnAction(ActionEvent event){
        if (cbReportsPeriod.getItems().isEmpty()){
            return;
        }

        java.util.HashMap<String, java.time.LocalDate> period = cbReportsPeriod.getSelectionModel().getSelectedItem().getPeriod();

        EventHandler<ActionEvent> onAction = dpReportsFrom.getOnAction();
        //  avoid calling dpReportsFromOnAction event
        dpReportsFrom.setOnAction(null);
        dpReportsFrom.setValue(period.get("start"));
        dpReportsFrom.setOnAction(onAction);

        onAction = dpReportsTo.getOnAction();
        //  avoid calling dpReportsToOnAction event
        dpReportsTo.setOnAction(null);
        dpReportsTo.setValue(period.get("end"));
        dpReportsTo.setOnAction(onAction);
    }

    @FXML
    private void dpReportsFromOnAction(ActionEvent event){
        EventHandler<ActionEvent> onAction = cbReportsPeriod.getOnAction();
        //  avoid calling cbReportsPeriodOnAction() event when date is changing
        cbReportsPeriod.setOnAction(null);

        cbReportsPeriod.getSelectionModel().select(ReportPeriod.CUSTOM);

        cbReportsPeriod.setOnAction(onAction);
    }

    @FXML
    private void dpReportsToOnAction(ActionEvent event){
        EventHandler<ActionEvent> onAction = cbReportsPeriod.getOnAction();
        //  avoid calling cbReportsPeriodOnAction() event when date is changing
        cbReportsPeriod.setOnAction(null);

        cbReportsPeriod.getSelectionModel().select(ReportPeriod.CUSTOM);

        cbReportsPeriod.setOnAction(onAction);
    }

    @FXML
    private void btnReportsGenerateOnAction(ActionEvent event){
        //  is reports tab selected?
        if (tabReports.isSelected()){
            //  is stats tab selected
            if (tabStats.isSelected()){
                //  generate statistics
                StatsReport sr = new StatsReport(dpReportsFrom.getValue(), dpReportsTo.getValue());

                final TreeTableView<ReportRow> ttvContent = new TreeTableView<>();
                apReportsStats.getChildren().add(ttvContent);
                AnchorPane.setTopAnchor(ttvContent, 0.0);
                AnchorPane.setBottomAnchor(ttvContent, 0.0);
                AnchorPane.setLeftAnchor(ttvContent, 0.0);
                AnchorPane.setRightAnchor(ttvContent, 0.0);

                sr.generate(ttvContent);
            }else if (tabAccounts.isSelected()) {
                //  generate accounts report
                AccountsReport ar = new AccountsReport(
                        dpReportsFrom.getValue(),
                        dpReportsTo.getValue(),
                        cbxReportsAccountFilter.isSelected() ? cbReportsAccounts.getValue() : null
                );

                ar.generate(ttvReportsAccounts);
            }else if (tabPayees.isSelected()){
                //  generate payees report
                PayeesReport payeesReport = new PayeesReport(
                        dpReportsFrom.getValue(),
                        dpReportsTo.getValue(),
                        cbxReportsPayeeFilter.isSelected() ? cbReportsPayees.getValue() : null
                );

                payeesReport.generate(ttvReportsPayees);
            }else if (tabCategories.isSelected()){
                //  generate categories report
                CaterogiesReport caterogiesReport = new CaterogiesReport(
                        dpReportsFrom.getValue(),
                        dpReportsTo.getValue(),
                        cbxReportsCategoryFilter.isSelected() ? cbReportsCategories.getValue() : null
                );

                caterogiesReport.generate(ttvReportsCategories);
            }
        }
    }
    //**************************************  /reports tab  **************************************//

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
            }else if (event.getCode() == KeyCode.ENTER){
                btnTransactionEditOnAction(null);
            }
        });

        spMainContainer.getDividers().get(0).positionProperty().addListener((observable, oldValue, newValue) -> {
            Kman.getSettings().setWindowDividerPosition(newValue.doubleValue());
        });

        //  transaction filter (feature #9)
        transactionsFiltered = new FilteredList<>(tvTransactions.getItems());
        tfFilter.textProperty().addListener((observable, oldValue, newValue) -> {
            transactionsFiltered.setPredicate(transaction -> {
                //  filter is empty. Thus, show all transactions
                if ( (newValue == null) || (newValue.isEmpty()) ){
                    return true;
                }

                //  value in text filter
                String filterValue = newValue.toLowerCase();
                //
                String transactionValue = transaction.toSearchString().toLowerCase();

                if (transactionValue.contains(filterValue)){
                    return true;
                }

                return false;
            });
        });

        tfFilter.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.ESCAPE){
                tfFilter.setText("");
                tvTransactions.requestFocus();
            }
        });
    }

    public void setDividerPosition(double position){
        spMainContainer.setDividerPosition(0, position);
    }
}