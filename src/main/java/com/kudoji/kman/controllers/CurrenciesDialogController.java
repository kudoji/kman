package com.kudoji.kman.controllers;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.kudoji.kman.models.Currency;
import com.kudoji.kman.Kman;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javax.xml.soap.Node;

/**
 * FXML Controller class
 *
 * @author kudoji
 */
public class CurrenciesDialogController extends Controller {
    private static final Logger log = Logger.getLogger(CurrenciesDialogController.class.getName());

    @FXML private javafx.scene.control.TextField tfFilter;
    @FXML private javafx.scene.control.TableView<Currency> tvCurrencies;
    @FXML private javafx.scene.control.TableColumn<Currency, String> tcName;
    @FXML private javafx.scene.control.TableColumn<Currency, String> tcSample;
    @FXML private javafx.scene.control.TableColumn<Currency, String> tcRate;
    @FXML
    private Label lbStatus;

    @FXML
    private void btnCurrencyInsertOnAction(ActionEvent event){
        Kman.showAndWaitForm("/views/CurrencyDialog.fxml", "Add new currency...", null);
    }

    @FXML
    private void btnCurrencyEditOnAction(ActionEvent event){
        Currency currencySelected = tvCurrencies.getSelectionModel().getSelectedItem();
        if (currencySelected == null){
            Kman.showErrorMessage("Please, select a currency first");
            
            return;
        }

        Kman.showAndWaitForm("/views/CurrencyDialog.fxml", "Edit currency...", currencySelected);
    }

    @FXML
    private void btnCurrencyDeleteOnAction(ActionEvent event){
        Currency currencySelected = tvCurrencies.getSelectionModel().getSelectedItem();
        if (currencySelected == null){
            Kman.showErrorMessage("Please, select a currency first");
            
            return;
        }

        if (!Kman.showConfirmation("All accounts (and, possibly, transactions) with the currency will be also deleted.", "Are you sure?")){
            return;
        }
        
        java.util.HashMap<String, String> params = new java.util.HashMap<>();
        params.put("table", "currencies");
        params.put("where", "id = " + currencySelected.getID());
        
        if (Kman.getDB().deleteData(params)){
            Currency.getCurrencies().remove(currencySelected);
        }
    }

    /**
     *
     * @param currenciesData
     * @return
     */
    private void downloadCurrenciesToMap(Map<String, Float> currenciesData){
        //all currencies' rates are relative to EUR, but base currency is Currency.BASE_CURRENCY_CODE (USD)
        float baseCurrencyRateToEUR = 0.0f;

        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        factory.setValidating(false);
        try{
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document xml = builder.parse(Currency.URL_CURRENCY_RATES);

            xml.getDocumentElement().normalize();
            org.w3c.dom.NodeList nodeList = xml.getElementsByTagName("Cube");

            for (int i = 0; i < nodeList.getLength(); i++){
                org.w3c.dom.Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE){
                    org.w3c.dom.Element element = (org.w3c.dom.Element)node;
                    String code = element.getAttribute("currency");
                    String rate = element.getAttribute("rate");
                    if (code.isEmpty() || rate.isEmpty()) continue;

                    try{
                        if (code.toLowerCase().equals(Currency.BASE_CURRENCY_CODE.toLowerCase())){
                            baseCurrencyRateToEUR = Float.parseFloat(rate);
                        }
                        currenciesData.put(code, Float.parseFloat(rate));
                    }catch (NumberFormatException ne){
                        log.log(Level.WARNING, ne.getMessage() + " (Cannot load currency: " + code + ", rate: " + rate + ")", ne);
                    }
                }
            }
        }catch (Exception e){
            log.log(Level.WARNING, e.getMessage(), e);
        }

        currenciesData.put("baseCurrencyRateToEUR", baseCurrencyRateToEUR);
    }

    private void loadCurrenciesToDB(Map<String, Float> currenciesData){
        //currenciesData.isEmpty() ||
        if (currenciesData.size() == 1){ // it contains "baseCurrencyRateToEUR" key
            Kman.showErrorMessage("Error to load currencies data");
            return;
        }

        float baseCurrencyRateToEUR = currenciesData.get("baseCurrencyRateToEUR");

        if (baseCurrencyRateToEUR == 0f){ //couldn't find base rate
            Kman.showErrorMessage("Base currency (" + Currency.BASE_CURRENCY_CODE + ") is not found");
            return;
        }

        boolean resultUpdate = true;
        for (java.util.Map.Entry<String, Float> currency : currenciesData.entrySet()) {
            java.util.HashMap<String, String> row = new HashMap<>();
            row.put("table", "currencies");
            row.put("where", "code = '" + currency.getKey() + "'");
            row.put("rate", Float.toString(currency.getValue() / baseCurrencyRateToEUR));

            if (Kman.getDB().updateData(false, row) != 1){
                resultUpdate = false;
            }
        }

        //also need to update EUR
        java.util.HashMap<String, String> row = new java.util.HashMap<>();
        row.put("table", "currencies");
        row.put("where", "code = 'EUR'");
        row.put("rate", Float.toString(1 / baseCurrencyRateToEUR));

        if (Kman.getDB().updateData(false, row) != 1){
            resultUpdate = false;
        }

        if (!resultUpdate){
            Kman.showErrorMessage("Couldn't update rate for some currencies...");
        }else{
            Kman.showInformation("currencies successfully updated");
        }
    }

    @FXML
    private void btnCurrencyUpdateOnAction(ActionEvent event){
        final Map<String, Float> currenciesData = new ConcurrentHashMap<>();

        Task <Void> task = new Task<Void>(){
            @Override
            public Void call(){
                updateMessage("Please wait, currencies are loading...");

                downloadCurrenciesToMap(currenciesData);

                return null;
            }
        };

        lbStatus.textProperty().bind(task.messageProperty());
        task.setOnSucceeded(e -> {
            loadCurrenciesToDB(currenciesData);
            lbStatus.textProperty().unbind();
            lbStatus.setText("");
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        tvCurrencies.setEditable(false);
        
        tcName.setCellValueFactory(new PropertyValueFactory<>("name"));
        tcSample.setCellValueFactory(new PropertyValueFactory<>("sample"));
        tcRate.setCellValueFactory(new PropertyValueFactory<>("rateString"));

        tvCurrencies.setOnMouseClicked((event) -> {
            if ((event.getButton() == MouseButton.PRIMARY) && (event.getClickCount() == 2) ){
                btnCurrencyEditOnAction(null);
            }
        });
        
        FilteredList<Currency> dataFiltered = new FilteredList<>(Currency.getCurrencies(), p -> true);
        tfFilter.textProperty().addListener((observable, valueOld, valueNew) -> {
            dataFiltered.setPredicate((currency) -> {
                //text is empty, show all values
                if ( (valueNew == null) || (valueNew.isEmpty()) ){
                    return true;
                }
                
                String valueNewLC = valueNew.toLowerCase();
                Currency currencyCurrent = currency;
                if ( currencyCurrent.getName().toLowerCase().contains(valueNewLC)){
                    //value matches currency name
                    return true;
                }else if (currencyCurrent.getSample().toLowerCase().contains(valueNewLC)){
                    //value mathces sample, which contains currency code
                    return true;
                }
                
                //value doesn't match
                return false;
            });
        });
        
//        tvCurrencies.setItems(Currency.getCurrencies());
        tvCurrencies.setItems(dataFiltered);

        tfFilter.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.ESCAPE){
                tfFilter.setText("");
                tvCurrencies.requestFocus();
            }
        });
    }   

    @SuppressWarnings("unchecked")
    @Override
    public void setFormObject(Object _formObject){
        if (_formObject != null){
            //opened as a select currency form

            java.util.HashMap<String, Currency> formObject = (java.util.HashMap<String, Currency>)_formObject;

            tvCurrencies.setOnMouseClicked((event) -> {

                if ( (event.getButton() == MouseButton.PRIMARY) && (event.getClickCount() == 2) ){//double click
                    formObject.put("object", tvCurrencies.getSelectionModel().getSelectedItem());
                    super.setChanged();
                    super.closeStage();
                }
            });
        }
    }
}    
