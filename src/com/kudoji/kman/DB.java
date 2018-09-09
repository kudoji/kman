/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kudoji.kman;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author kudoji
 */
public class DB {
    private Connection dbConnection = null;
    private String dbFile = null;
    private String dbUrl = null;
    private boolean debug = false;
    
    public DB(String _dbFile) {
        dbFile = _dbFile;
        dbUrl = "jdbc:sqlite:" + dbFile;
    }
    
    public Connection getConnection(){
        return this.dbConnection;
    }
    
    public void setDebugMode(boolean _debug){
        this.debug = _debug;
    }
    
    public boolean connect(){
        boolean result = false;
        try{
            dbConnection = DriverManager.getConnection(dbUrl);
            //force using koreign key constrain
            dbConnection.createStatement().execute("PRAGMA foreign_keys = ON;");
            if (debug){
                System.out.println("sqlite connected");
            }
            result = true;
        }catch (SQLException e){
            if (debug){
                System.out.println(e.getMessage());
            }
            result = false;
        }
        
        return result;
    }
    
    public boolean close(){
        boolean result = true;
        
        try{
            if (dbConnection != null){
                dbConnection.close();
            }
        }catch (SQLException ex){
            result = false;
            if (debug){
                System.out.println(ex.getMessage());
            }
        }
        
        return result;
    }

    @Override
    protected void finalize() throws Throwable{
        if (this.dbConnection != null){
            this.dbConnection.close();
        }
        this.dbConnection = null;
        
        super.finalize();
    }
    
    /**
     * converts ResultSet to Dictionary
     * 
     * @param _rs 
     */
    private HashMap<String, String> convertRS2Dic(ResultSet _rs) throws SQLException{
        HashMap<String, String> result = new HashMap<>();
        
        ResultSetMetaData rsmd = _rs.getMetaData();
        int numColumns = rsmd.getColumnCount();
        
        for (int i = 1; i <= numColumns; i++){ //strange to face indexes start from 1
            String col_name = rsmd.getColumnName(i);
            
            switch (rsmd.getColumnType(i)) {
                case java.sql.Types.INTEGER:
                    result.put(col_name, Integer.toString(_rs.getInt(i)));
                    break;
                case java.sql.Types.VARCHAR:
                    if (_rs.getString(i) == null){ //can return null if value in DB is null
                        result.put(col_name, "");
                    }else{
                        result.put(col_name, _rs.getString(i));
                    }
                    break;
                case java.sql.Types.REAL:
                    result.put(col_name, Float.toString(_rs.getFloat(i)));
                    break;
                case java.sql.Types.BLOB:
                    result.put(col_name, new String(_rs.getBlob(i).getBytes(1l, (int) _rs.getBlob(i).length())) );
                    break;
                case java.sql.Types.NUMERIC:
                    if (_rs.getBigDecimal(i) == null){
                        result.put(col_name, "");
                    }else{
                        result.put(col_name, _rs.getBigDecimal(i).toString());
                    }
                    break;
                default:
                    System.out.println(rsmd.getColumnType(i));
                    break;
            }
        }
        
        return result;
    }
    
    /**
     * 
     * @param _insert
     * @param _parameters
     * - "table" - table name working with
     * - "id" - update this id only. Cannot be used with insert
     * - "set" - used with update only
     * - "where" - used with update only
     * - "order" - used with update only
     * 
     * @return 0 in case of error; if update, returns 1, if insert, returns generated id
     */
    public int updateData(boolean _insert, HashMap<String, String> _parameters){
        int insertedId = 0;
        String sqlText = "";
        Object param = _parameters.get("table");
        if (param == null){ //table is not set
            System.err.println(this.getClass().getName() + ": " + "table is not set");
            return 0;
        }

        if (_insert){ //insert into statement
            sqlText = "insert into " + param.toString() + " ";
        }else{ //update statement
            sqlText = "update " + param.toString() + " set ";
        }
        _parameters.remove("table");
        
        Object paramID = _parameters.get("id");
        Object paramWhere = _parameters.get("where");
        if ( (paramWhere == null) && (paramID == null) && (!_insert) ){ //id must be set for update statement
            System.err.println(this.getClass().getName() + ": " + "id field or where condition is not set");
            return 0;
        }
        if (!_insert){
            //for insert statement will keep id
            _parameters.remove("id");
        }
        
        _parameters.remove("where");
        
        Object paramOrder = _parameters.get("order");
        if ( (paramOrder != null) && (_insert) ){
            System.err.println(this.getClass().getName() + ": " + "order by cannot be used with insert");
            return 0;
        }
        _parameters.remove("order");

        if (_parameters.isEmpty()){ //no fields to update/insert
            System.err.println(this.getClass().getName() + ": " + "no fields are set for update/insert");
            return 0;
        }
        
        Object paramSet = _parameters.get("set");
        if ( (paramSet != null) && (!_insert) ){
            sqlText = sqlText + "" + paramSet.toString(); //keywork set is already injected earlier
        }else{
            String sqlValues = " values (";
            if (_insert){
                sqlText = sqlText + " (";
            }

            for (Map.Entry<String, String> parameter : _parameters.entrySet()) {
                String value = parameter.getValue();
                if (!_insert){
                    sqlText = sqlText + parameter.getKey() + " = ";
                    if (value == null){ //null value must be null but not "null"
                        sqlText += value;
                    }else{
                        sqlText += "\"" + value + "\"";
                    }
                    sqlText += ", ";
                }else{
                    sqlText = sqlText + parameter.getKey() + ", ";
                    if (value == null){ //null value must be null but not "null"
                        sqlValues += value;
                    }else{
                        sqlValues += "\"" + value + "\"";
                    }
                    sqlValues += ", ";
                }
            }
            //remove last comma with space
            sqlText = sqlText.substring(0, sqlText.length() - 2 );
            if (_insert){
                sqlValues = sqlValues.substring(0, sqlValues.length() - 2 );

                sqlText = sqlText + ")" + sqlValues + ")";
            }
        }
        
        if (!_insert){ //must specify which row to update
            if (paramID != null){
                sqlText = sqlText + " where id = " + paramID.toString();
            }else if (paramWhere != null){
                sqlText = sqlText + " where " + paramWhere.toString();
            }
            
            if (paramOrder != null){
                sqlText += "order by " + paramOrder.toString();
            }
        }
        
        sqlText = sqlText + ";";
        
        try{
            try (PreparedStatement st = this.dbConnection.prepareStatement(sqlText, Statement.RETURN_GENERATED_KEYS)) {
                st.executeUpdate();
                
                if (_insert){ //get the inserted id
                    ResultSet rs = st.getGeneratedKeys();

                    if (rs.next()){
                        insertedId = rs.getInt(1);
                    }
                }
            }
        }catch (SQLException e){
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            return 0;
        }
        
        if (!_insert){ //in case of update return 1 otherwise - inserted id
            insertedId = 1;
        }
        
        return insertedId;
    }
    
    /**
     * Executes raw sql sentence
     * @param _sqlText sql sentence
     * @return 
     */
    public boolean execSQL(String _sqlText){
        try
        (
            Statement statement = this.dbConnection.createStatement();
        ){
            statement.execute(_sqlText);
        }catch (SQLException e){
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            return false;
        }
        
        return true;
    }
    
    public boolean deleteData(HashMap<String, String> _params){
        String sqlText = "delete from ";
        Object param = _params.get("table");
        if (param == null){ //table is not set
            System.err.println(this.getClass().getName() + ": " + "table is not set");
            return false;
        }
        sqlText += param.toString();
        
        param = _params.get("where");
        if (param == null){ //table is not set
            System.err.println(this.getClass().getName() + ": " + "where condition is not set");
            return false;
        }
        
        sqlText += " where " + param.toString() + ";";
        
        try
        (
            Statement statement = this.dbConnection.createStatement();
        ){
            statement.execute(sqlText);
        }catch (SQLException e){
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            return false;
        }
        
        return true;
    }
    
    /**
     * Selects data from a table
     * 
     * @param _parameters can have these keys:
     * - "table" - table name to select data from;
     * - "id"(optional) - in case of getting one row only;
     * - "where" (optional) - in case of using where condition;
     * - "order"(optional) - in case of sorting data.
     * - "limit"(optional) - in case of using limit
     * 
     * "id" and "where" cannot be used together
     * 
     * @return Array of HashMap where each HashMap is a table row 
     */
    public ArrayList<HashMap<String, String>> selectData(HashMap<String, String> _parameters){
        ArrayList<HashMap<String, String>> result = new ArrayList<>();
        
        String sqlText = "select * from ";
        Object param = _parameters.get("table");
        if (param == null){
            System.err.println(this.getClass().getName() + ": " + " table is not set");
        }else{
            sqlText = sqlText + param.toString();
        }
        
        param = _parameters.get("id");
        if (param != null){ //find a particular row
            sqlText = sqlText + " where id = " + param.toString();
        }
        
        param = _parameters.get("where");
        if (param != null){
            sqlText = sqlText + " where " + param.toString();
        }
        
        param = _parameters.get("order");
        if (param != null){ //add order by
            sqlText = sqlText + " order by " + param.toString();
        }
        
        param = _parameters.get("limit");
        if (param != null){ //add order by
            sqlText = sqlText + " limit " + param.toString();
        }
        
        sqlText = sqlText + ";";

        try
        (
            Statement st = this.dbConnection.createStatement();
            ResultSet rs = st.executeQuery(sqlText);
        )
        {
            while (rs.next()){
                HashMap<String, String> row = convertRS2Dic(rs);

                result.add(row);
            }
        }catch (SQLException e){
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
        
        return result;
    }
    
    public boolean startTransaction(){
        boolean result = true;
        try{
            this.dbConnection.setAutoCommit(false);
        }catch (SQLException e){
            result = false;
            System.err.println(e.getClass() + ": " + e.getMessage() + " (couldn't start transaction)");
        }
        
        return result;
    }
    
    public boolean rollbackTransaction(){
        boolean result = true;
        try{
            this.dbConnection.rollback();
        }catch (SQLException e){
            result = false;
            System.err.println(e.getClass() + ": " + e.getMessage() + " (couldn't start transaction)");
        }
        
        return result;
    }
    
    public boolean commitTransaction(){
        boolean result = true;
        try{
            this.dbConnection.commit();
        }catch (SQLException e){
            result = false;
            System.err.println(e.getClass() + ": " + e.getMessage() + " (couldn't start transaction)");
        }
        
        return result;
    }
    
    /**
     * Populate tables with necessary data
     * 
     * @param _statement 
     */
    private void fillUpTables(Statement _statement, boolean _truncate) throws SQLException{
        String sqlText = "";
        
        //fill up transaction_types table
        if (_truncate){
            sqlText = "delete from transaction_types;";
            _statement.executeUpdate(sqlText);
        }
        sqlText = "insert into transaction_types (id, name) values (1, 'Deposit'), (2, 'Withdrawal'), (3, 'Transfer');";
        _statement.executeUpdate(sqlText);
        
        //fill up categories table
        if (_truncate){
            sqlText = "delete from categories;";
            _statement.executeUpdate(sqlText);
        }
        
        sqlText = "insert into categories (id, name, categories_id) values " +
                  "(1, 'Transfer', null)," +
                  "(2, 'Unknown', null)," +
                  "(3, 'Deposit', null)," +
                  "(4, 'Withdrawal', null);";
        _statement.executeUpdate(sqlText);

        sqlText = "insert into categories (id, name, categories_id) values " +
                  "(5, 'Automobile', 4)," +
                  "(6, 'Bills', 4)," +
                  "(7, 'Transport', 4)," +
                  "(8, 'Education', 4)," +
                  "(9, 'Food', 4)," +
                  "(10, 'Healthcare', 4)," +
                  "(11, 'Homeneeds', 4)," +
                  "(12, 'Insurance', 4)," +
                  "(13, 'Leisure', 4)," +
                  "(14, 'Taxes', 4)," +
                  "(15, 'Vacation', 4)," +
                  "(16, 'Salary', 3)," +
                  "(17, 'Investments', 3)," +
                  "(18, 'Refunds', 3)," +
                  "(19, 'Other', 3);";
        _statement.executeUpdate(sqlText);

        sqlText = "insert into categories (name, categories_id) values " +
                  "('Gas', 5)," +
                  "('Maintenance', 5)," +
                  "('Parking', 5)," +
                  "('Registration', 5)," +
                  "('Cable TV', 6)," +
                  "('Electricity', 6)," +
                  "('Gas', 6)," +
                  "('Internet', 6)," +
                  "('Rent', 6)," +
                  "('Telephone', 6)," +
                  "('Cellphone', 6)," +
                  "('Water', 6)," +
                  "('Car', 7)," +
                  "('Taxi', 7)," +
                  "('Bus', 7)," +
                  "('Train', 7)," +
                  "('Plane', 7)," +
                  "('Books', 8)," +
                  "('Tuition', 8)," +
                  "('Other', 8)," +
                  "('Dining out', 9)," +
                  "('Groceries', 9)," +
                  "('Dental', 10)," +
                  "('Eyecare', 10)," +
                  "('Health', 10)," +
                  "('Prescriptions', 10)," +
                  "('Clothing', 11)," +
                  "('Furnishing', 11)," +
                  "('Other', 11)," +
                  "('Auto', 12)," +
                  "('Health', 12)," +
                  "('Home', 12)," +
                  "('Life', 12)," +
                  "('Gifts', 13)," +
                  "('Magazines', 13)," +
                  "('Movies', 13)," +
                  "('House', 14)," +
                  "('Income', 14)," +
                  "('Other', 14)," +
                  "('Travelling', 15)," +
                  "('Excursion', 15)," +
                  "('Taxi', 8);";
        _statement.executeUpdate(sqlText);
        
        sqlText = "insert into currencies (code, name, starts_with_code) values ";
        sqlText += "('AED', 'United Arab Emirates dirham', 0),";
        sqlText += "('AFN', 'Afghan afghani', 0),";
        sqlText += "('ALL', 'Albanian lek', 0),";
        sqlText += "('AMD', 'Armenian dram', 0),";
        sqlText += "('ANG', 'Netherlands Antillean guilder', 0),";
        sqlText += "('AOA', 'Angolan kwanza', 0),";
        sqlText += "('ARS', 'Argentine peso', 0),";
        sqlText += "('AUD', 'Australian dollar', 0),";
        sqlText += "('AWG', 'Aruban florin', 0),";
        sqlText += "('AZN', 'Azerbaijani manat', 0),";
        sqlText += "('BAM', 'Bosnia and Herzegovina convertible mark', 0),";
        sqlText += "('BBD', 'Barbados dollar', 0),";
        sqlText += "('BDT', 'Bangladeshi taka', 0),";
        sqlText += "('BGN', 'Bulgarian lev', 0),";
        sqlText += "('BHD', 'Bahraini dinar', 0),";
        sqlText += "('BIF', 'Burundian franc', 0),";
        sqlText += "('BMD', 'Bermudian dollar', 0),";
        sqlText += "('BND', 'Brunei dollar', 0),";
        sqlText += "('BOB', 'Boliviano', 0),";
        sqlText += "('BRL', 'Brazilian real', 0),";
        sqlText += "('BSD', 'Bahamian dollar', 0),";
        sqlText += "('BTN', 'Bhutanese ngultrum', 0),";
        sqlText += "('BWP', 'Botswana pula', 0),";
        sqlText += "('BYN', 'New Belarusian ruble', 0),";
        sqlText += "('BYR', 'Belarusian ruble', 0),";
        sqlText += "('BZD', 'Belize dollar', 0),";
        sqlText += "('CAD', 'Canadian dollar', 0),";
        sqlText += "('CDF', 'Congolese franc', 0),";
        sqlText += "('CHF', 'Swiss franc', 0),";
        sqlText += "('CLF', 'Unidad de Fomento', 0),";
        sqlText += "('CLP', 'Chilean peso', 0),";
        sqlText += "('CNY', 'Renminbi|Chinese yuan', 0),";
        sqlText += "('COP', 'Colombian peso', 0),";
        sqlText += "('CRC', 'Costa Rican colon', 0),";
        sqlText += "('CUC', 'Cuban convertible peso', 0),";
        sqlText += "('CUP', 'Cuban peso', 0),";
        sqlText += "('CVE', 'Cape Verde escudo', 0),";
        sqlText += "('CZK', 'Czech koruna', 0),";
        sqlText += "('DJF', 'Djiboutian franc', 0),";
        sqlText += "('DKK', 'Danish krone', 0),";
        sqlText += "('DOP', 'Dominican peso', 0),";
        sqlText += "('DZD', 'Algerian dinar', 0),";
        sqlText += "('EGP', 'Egyptian pound', 0),";
        sqlText += "('ERN', 'Eritrean nakfa', 0),";
        sqlText += "('ETB', 'Ethiopian birr', 0),";
        sqlText += "('EUR', 'Euro', 0),";
        sqlText += "('FJD', 'Fiji dollar', 0),";
        sqlText += "('FKP', 'Falkland Islands pound', 0),";
        sqlText += "('GBP', 'Pound sterling', 0),";
        sqlText += "('GEL', 'Georgian lari', 0),";
        sqlText += "('GHS', 'Ghanaian cedi', 0),";
        sqlText += "('GIP', 'Gibraltar pound', 0),";
        sqlText += "('GMD', 'Gambian dalasi', 0),";
        sqlText += "('GNF', 'Guinean franc', 0),";
        sqlText += "('GTQ', 'Guatemalan quetzal', 0),";
        sqlText += "('GYD', 'Guyanese dollar', 0),";
        sqlText += "('HKD', 'Hong Kong dollar', 0),";
        sqlText += "('HNL', 'Honduran lempira', 0),";
        sqlText += "('HRK', 'Croatian kuna', 0),";
        sqlText += "('HTG', 'Haitian gourde', 0),";
        sqlText += "('HUF', 'Hungarian forint', 0),";
        sqlText += "('IDR', 'Indonesian rupiah', 0),";
        sqlText += "('ILS', 'Israeli new shekel', 0),";
        sqlText += "('INR', 'Indian rupee', 0),";
        sqlText += "('IQD', 'Iraqi dinar', 0),";
        sqlText += "('IRR', 'Iranian rial', 0),";
        sqlText += "('ISK', 'Icelandic króna', 0),";
        sqlText += "('JMD', 'Jamaican dollar', 0),";
        sqlText += "('JOD', 'Jordanian dinar', 0),";
        sqlText += "('JPY', 'Japanese yen', 0),";
        sqlText += "('KES', 'Kenyan shilling', 0),";
        sqlText += "('KGS', 'Kyrgyzstani som', 0),";
        sqlText += "('KHR', 'Cambodian riel', 0),";
        sqlText += "('KMF', 'Comoro franc', 0),";
        sqlText += "('KPW', 'North Korean won', 0),";
        sqlText += "('KRW', 'South Korean won', 0),";
        sqlText += "('KWD', 'Kuwaiti dinar', 0),";
        sqlText += "('KYD', 'Cayman Islands dollar', 0),";
        sqlText += "('KZT', 'Kazakhstani tenge', 0),";
        sqlText += "('LAK', 'Lao kip', 0),";
        sqlText += "('LBP', 'Lebanese pound', 0),";
        sqlText += "('LKR', 'Sri Lankan rupee', 0),";
        sqlText += "('LRD', 'Liberian dollar', 0),";
        sqlText += "('LSL', 'Lesotho loti', 0),";
        sqlText += "('LYD', 'Libyan dinar', 0),";
        sqlText += "('MAD', 'Moroccan dirham', 0),";
        sqlText += "('MDL', 'Moldovan leu', 0),";
        sqlText += "('MGA', 'Malagasy ariary', 0),";
        sqlText += "('MKD', 'Macedonian denar', 0),";
        sqlText += "('MMK', 'Myanmar kyat', 0),";
        sqlText += "('MNT', 'Mongolian tögrög', 0),";
        sqlText += "('MOP', 'Macanese pataca', 0),";
        sqlText += "('MRO', 'Mauritanian ouguiya', 0),";
        sqlText += "('MUR', 'Mauritian rupee', 0),";
        sqlText += "('MVR', 'Maldivian rufiyaa', 0),";
        sqlText += "('MWK', 'Malawian kwacha', 0),";
        sqlText += "('MXN', 'Mexican peso', 0),";
        sqlText += "('MXV', 'Mexican Unidad de Inversion', 0),";
        sqlText += "('MYR', 'Malaysian ringgit', 0),";
        sqlText += "('MZN', 'Mozambican metical', 0),";
        sqlText += "('NAD', 'Namibian dollar', 0),";
        sqlText += "('NGN', 'Nigerian naira', 0),";
        sqlText += "('NIO', 'Nicaraguan córdoba', 0),";
        sqlText += "('NOK', 'Norwegian krone', 0),";
        sqlText += "('NPR', 'Nepalese rupee', 0),";
        sqlText += "('NZD', 'New Zealand dollar', 0),";
        sqlText += "('OMR', 'Omani rial', 0),";
        sqlText += "('PAB', 'Panamanian balboa', 0),";
        sqlText += "('PEN', 'Peruvian Sol', 0),";
        sqlText += "('PGK', 'Papua New Guinean kina', 0),";
        sqlText += "('PHP', 'Philippine peso', 0),";
        sqlText += "('PKR', 'Pakistani rupee', 0),";
        sqlText += "('PLN', 'Polish złoty', 0),";
        sqlText += "('PYG', 'Paraguayan guaraní', 0),";
        sqlText += "('QAR', 'Qatari riyal', 0),";
        sqlText += "('RON', 'Romanian leu', 0),";
        sqlText += "('RSD', 'Serbian dinar', 0),";
        sqlText += "('RUB', 'Russian ruble', 0),";
        sqlText += "('RWF', 'Rwandan franc', 0),";
        sqlText += "('SAR', 'Saudi riyal', 0),";
        sqlText += "('SBD', 'Solomon Islands dollar', 0),";
        sqlText += "('SCR', 'Seychelles rupee', 0),";
        sqlText += "('SDG', 'Sudanese pound', 0),";
        sqlText += "('SEK', 'Swedish krona', 0),";
        sqlText += "('SGD', 'Singapore dollar', 0),";
        sqlText += "('SHP', 'Saint Helena pound', 0),";
        sqlText += "('SLL', 'Sierra Leonean leone', 0),";
        sqlText += "('SOS', 'Somali shilling', 0),";
        sqlText += "('SRD', 'Surinamese dollar', 0),";
        sqlText += "('SSP', 'South Sudanese pound', 0),";
        sqlText += "('STD', 'São Tomé and Príncipe dobra', 0),";
        sqlText += "('SVC', 'Salvadoran colón', 0),";
        sqlText += "('SYP', 'Syrian pound', 0),";
        sqlText += "('SZL', 'Swazi lilangeni', 0),";
        sqlText += "('THB', 'Thai baht', 0),";
        sqlText += "('TJS', 'Tajikistani somoni', 0),";
        sqlText += "('TMT', 'Turkmenistani manat', 0),";
        sqlText += "('TND', 'Tunisian dinar', 0),";
        sqlText += "('TOP', 'Tongan paʻanga', 0),";
        sqlText += "('TRY', 'Turkish lira', 0),";
        sqlText += "('TTD', 'Trinidad and Tobago dollar', 0),";
        sqlText += "('TWD', 'New Taiwan dollar', 0),";
        sqlText += "('TZS', 'Tanzanian shilling', 0),";
        sqlText += "('UAH', 'Ukrainian hryvnia', 0),";
        sqlText += "('UGX', 'Ugandan shilling', 0),";
        sqlText += "('USD', 'United States dollar', 0),";
        sqlText += "('UYI', 'Uruguay Peso en Unidades Indexadas', 0),";
        sqlText += "('UYU', 'Uruguayan peso', 0),";
        sqlText += "('UZS', 'Uzbekistan som', 0),";
        sqlText += "('VEF', 'Venezuelan bolívar', 0),";
        sqlText += "('VND', 'Vietnamese đồng', 0),";
        sqlText += "('VUV', 'Vanuatu vatu', 0),";
        sqlText += "('WST', 'Samoan tala', 0),";
        sqlText += "('XAF', 'Central African CFA franc', 0),";
        sqlText += "('XCD', 'East Caribbean dollar', 0),";
        sqlText += "('XOF', 'West African CFA franc', 0),";
        sqlText += "('XPF', 'CFP franc', 0),";
        sqlText += "('YER', 'Yemeni rial', 0),";
        sqlText += "('ZAR', 'South African rand', 0),";
        sqlText += "('ZMW', 'Zambian kwacha', 0),";
        sqlText += "('ZWL', 'Zimbabwean dollar', 0);";        
        _statement.executeUpdate(sqlText);
    }
    
    /**
     * 
     * @param dropTables adds drop table sql statement
     * @return 
     */
    public boolean createAllTables(boolean dropTables){
        boolean result = true;
        
        try{
            Statement sqlStatement = this.dbConnection.createStatement();
            
            //get the amount of table inside db
            String sqlText = "select count(*) as c from sqlite_master where type='table';";
            ResultSet rs = sqlStatement.executeQuery(sqlText);
            rs.next();
            
            Integer tablesAmount = rs.getInt("c");
            if (tablesAmount == 0){//there are no tables in db; let's create them...
                if (dropTables){
                    sqlText = "drop table if exists currencies;";
                    sqlStatement.execute(sqlText);
                }
                
                sqlText = "create table currencies\n" +
                "(\n" +
                "       id integer primary key,\n" +
                "       name text not null unique,\n" +
                "       code text not null unique collate nocase,\n" + //make the column case-insensitive
                "       starts_with_code integer check(starts_with_code in (0,1)),\n" +
                "       rate real\n" +
                ");";
                sqlStatement.execute(sqlText);
                
                if (dropTables){
                    sqlText = "drop table if exists accounts;";
                    sqlStatement.execute(sqlText);
                }
                
                sqlText = "create table accounts\n" +
                "(\n" +
                "       id integer primary key,\n" +
		"	name text not null,\n" +
		"	balance_initial real,\n" +
		"	balance_current real,\n" +
		"	currencies_id integer not null,\n" +
		"	foreign key(currencies_id) references currencies(id) on delete cascade\n" + 
                ");";
                sqlStatement.execute(sqlText);
         
                if (dropTables){
                    sqlText = "drop table if exists payees;";
                    sqlStatement.execute(sqlText);
                }
                
                sqlText = "create table payees\n" +
                "(\n" +
                "       id integer primary key,\n" +
		"	name text not null,\n" +
                "	category_deposit int,\n" +
                "	category_withdrawal int,\n" +
                "       foreign key(category_deposit) references categories(id) on delete set null,\n" +
                "       foreign key(category_withdrawal) references categories(id) on delete set null\n" +
                ");";
                sqlStatement.execute(sqlText);
         
                if (dropTables){
                    sqlText = "drop table if exists categories;";
                    sqlStatement.execute(sqlText);
                }
                
                sqlText = "create table categories\n" +
                "(\n" +
                "       id integer primary key,\n" +
		"	name text not null,\n" +
                "       categories_id integer,\n" +
                "       foreign key(categories_id) references categories(id) on delete cascade\n" +
                ");";
                sqlStatement.execute(sqlText);
         
                if (dropTables){
                    sqlText = "drop table if exists transaction_types;";
                    sqlStatement.execute(sqlText);
                }
                
                sqlText = "create table transaction_types\n" +
                "(\n" +
                "       id integer primary key,\n" +
		"	name text not null\n" +
                ");";
                sqlStatement.execute(sqlText);
         
                if (dropTables){
                    sqlText = "drop table if exists transactions;";
                    sqlStatement.execute(sqlText);
                }
                
                sqlText = "create table transactions\n" +
                "(\n" +
                "       id integer primary key,\n" +
		"	date text not null,\n" +
                "	transaction_types_id integer not null,\n" +
                "	categories_id integer not null,\n" +
                "	payees_id integer,\n" + //can be null in case of transfer
                "	account_from_id integer,\n" + //can be null in case of deposit
                "	amount_from real not null,\n" +
                "	balance_from real,\n" +
                "	account_to_id integer,\n" + //can be null in case of withdrawal
                "	amount_to real,\n" +
                "	balance_to real,\n" +
                "	notes text not null,\n" +                        
                "	foreign key(transaction_types_id) references transaction_types(id) on delete cascade,\n" +
                "	foreign key(categories_id) references categories(id) on delete cascade,\n" +
                "	foreign key(payees_id) references payees(id) on delete cascade,\n" +
                "	foreign key(account_from_id) references accounts(id) on delete cascade,\n" +
                "	foreign key(account_to_id) references accounts(id) on delete cascade\n" +
                ");";
                sqlStatement.execute(sqlText);
                
                //tables already empty - no need to delete rows
                this.fillUpTables(sqlStatement, false);
            }
            //for debug puproses
//            this.fillUpTables(sqlStatement, true);
            
            rs.close();
            sqlStatement.close();
        }catch (SQLException e){
            result = false;
            if (this.debug){
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
            }
        }
        
        return result;
    }
    
}
