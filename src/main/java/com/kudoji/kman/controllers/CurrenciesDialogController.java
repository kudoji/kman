package com.kudoji.kman.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.kudoji.kman.models.Currency;
import com.kudoji.kman.Kman;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javax.xml.soap.Node;

/**
 * FXML Controller class
 *
 * @author kudoji
 */
public class CurrenciesDialogController extends Controller {
    @FXML private javafx.scene.control.TextField tfFilter;
    @FXML private javafx.scene.control.TableView<Currency> tvCurrencies;
    @FXML private javafx.scene.control.TableColumn<Currency, String> tcName;
    @FXML private javafx.scene.control.TableColumn<Currency, String> tcSample;
    @FXML private javafx.scene.control.TableColumn<Currency, String> tcRate;

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
    
    @FXML
    private void btnCurrencyUpdateOnAction(ActionEvent event){
        java.util.HashMap<String, Float> currenciesData = new java.util.HashMap<>();
        //all currencies' rates are relative to EUR, but base currency is Currency.BASE_CURRENCY_CODE (USD)
        Float baseCurrencyRateToEUR = 0.0f;
                    
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
                        System.err.println("Cannot load currency: " + code + ", rate: " + rate);
                    }
                }
            }
        }catch (Exception e){
            System.err.println(e.getClass() + ": " + e.getMessage());
        }
        
        if (currenciesData.isEmpty()){
            Kman.showErrorMessage("Error to load currencies data");
            return;
        }
        
        if (baseCurrencyRateToEUR == 0){ //couldn't find base rate
            Kman.showErrorMessage("Base currency (" + Currency.BASE_CURRENCY_CODE + ") is not found");
            return;
        }
        
        boolean resultUpdate = true;
        for (java.util.Map.Entry<String, Float> currency : currenciesData.entrySet()) {
            java.util.HashMap<String, String> row = new java.util.HashMap<>();
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
                Currency currencyCurrent = (Currency)currency;
                if ( currencyCurrent.getName().toLowerCase().contains(valueNewLC)){
                    //value matches currency name
                    return true;
                }else if (currencyCurrent.getSample().toLowerCase().contains(valueNewLC)){
                    //value mathces sample, which contains currency code
                    return true;
                }
                
                //value doen't match
                return false;
            });
        });
        
//        tvCurrencies.setItems(Currency.getCurrencies());
        tvCurrencies.setItems(dataFiltered);
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
