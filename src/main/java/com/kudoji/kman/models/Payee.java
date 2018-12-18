package com.kudoji.kman.models;

import java.util.HashMap;

import com.kudoji.kman.Kman;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

/**
 *
 * @author kudoji
 */
public class Payee {
    private IntegerProperty id;
    private StringProperty name;
    private IntegerProperty categoryDepositId;
    private IntegerProperty categoryWithdrawalId;
    private IntegerProperty usageFreq; //   keeps usage frequency
    
    /**
     * Keeps information about payees which allows reduce DB usage
     */
    private final static javafx.collections.ObservableList<Payee> payeesCache = FXCollections.observableArrayList();
    
    public Payee(HashMap<String, String> _params){
        if (_params == null) throw new IllegalArgumentException();

        this.id = new SimpleIntegerProperty(Integer.parseInt(_params.get("id")));
        this.name = new SimpleStringProperty(_params.get("name"));

        String categoryId = _params.get("category_deposit");
        this.categoryDepositId = new SimpleIntegerProperty(categoryId == null ? 0 : Integer.parseInt(categoryId));
        categoryId = _params.get("category_withdrawal");
        this.categoryWithdrawalId = new SimpleIntegerProperty(categoryId == null? 0 : Integer.parseInt(categoryId));

        this.usageFreq = new SimpleIntegerProperty(Integer.parseInt(_params.get("usage_freq")));

        this.usageFreq.addListener((observable, oldValue, newValue) -> {
            if (oldValue != newValue){
                //  sort Payees list by usage frequency (bug #27)
                sortPayees();
            }
        });
    }
    
    public void setFields(HashMap<String, String> _params){
        if (_params == null) throw new IllegalArgumentException();

//        this.id = new SimpleIntegerProperty((int)_params.get("id"));
        this.name.set(_params.get("name"));
        //for categories
        String categoryId = _params.get("category_deposit");
        this.categoryDepositId.set(categoryId == null ? 0 : Integer.parseInt(categoryId));
        categoryId = _params.get("category_withdrawal");
        this.categoryWithdrawalId.set(categoryId == null ? 0 : Integer.parseInt(categoryId));
        this.usageFreq.set(Integer.parseInt(_params.get("usage_freq")));
    }

    public int getId(){
        return this.id.get();
    }

    public String getName(){
        return this.name.get();
    }
    
    public void setName(String _name){
        if (_name == null) throw new IllegalArgumentException();

        this.name.set(_name);
    }
    
    /**
     * make name field observable by TableViewCell
     * @return 
     */
    public StringProperty nameProperty(){
        return this.name;
    }
    
    public int getCategoryDepositId(){
        return this.categoryDepositId.get();
    }

    public int getCategoryWithdrawalId(){
        return this.categoryWithdrawalId.get();
    }

    public int getUsageFreq(){
        return this.usageFreq.get();
    }

    public void setUsageFreq(int _value){
        if (_value < 0 || _value > Integer.MAX_VALUE) throw new IllegalArgumentException();

        this.usageFreq.set(_value);
    }

    /**
     * increment current usage frequency by _delta
     *
     * @param _delta
     * @return true than value changed, false otherwise
     */
    public void incUsageFreq(int _delta){
        if (_delta < 0){
            if (this.usageFreq.get() == Integer.MIN_VALUE){
                throw new IllegalArgumentException();
            }
        }else if (_delta == 0){
            throw new IllegalArgumentException();
        }else{
            if (this.usageFreq.get() == Integer.MAX_VALUE){
                throw new IllegalArgumentException();
            }
        }

        this.usageFreq.set(this.usageFreq.get() + _delta);
    }

    public static Payee getPayee(int _id){
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "payees");
        params.put("id", Integer.toString(_id));
        
        java.util.ArrayList<HashMap<String, String>> rows = Kman.getDB().selectData(params); //should be only one row
        
        if (rows.size() == 1){
            return new Payee(rows.get(0));
        }else{
            return null;
        }
    }
    
    public static javafx.collections.ObservableList<Payee> getPayees(){
        if (Payee.payeesCache.isEmpty()){ //never filled, need to get data from DB
            HashMap<String, String> params = new HashMap<>();
            params.put("table", "payees");
            //  order by usage frequency (more frequent items come first) and name
            params.put("order", "usage_freq desc, name asc");
            
            java.util.ArrayList<HashMap<String, String>> rows = Kman.getDB().selectData(params);
            for (HashMap<String, String> row: rows){
                Payee.payeesCache.add(new Payee(row));
            }
        }
        
        return Payee.payeesCache;
    }

    private void sortPayees(){
        if (Payee.getPayees().size() > 0){
            Payee.getPayees().sort((o1, o2) -> {
                int usageFreq1 = o1.getUsageFreq();
                int usegeFreq2 = o2.getUsageFreq();
                if (usageFreq1 > usegeFreq2){
                    //  payee with higher freq move to the top of the list
                    return -1;
                }else if (usageFreq1 == usegeFreq2){
                    return 0;
                }else{
                    //  payee with lower freq move to the bottom of the list
                    return 1;
                }
            });
        }
    }
    
    @Override
    public String toString(){
        return this.name.get();
    }

    /**
     * Saves instance to DB
     *
     * @return true is successful, false - otherwise
     */
    public boolean save(){
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "payees");
        params.put("name", this.getName());
        //  avoid foreign key constrain
        params.put("category_deposit", this.getCategoryDepositId() == 0 ? null: String.valueOf(this.getCategoryDepositId()));
        params.put("category_withdrawal", this.getCategoryWithdrawalId() == 0 ? null: String.valueOf(this.getCategoryWithdrawalId()));
        params.put("usage_freq", String.valueOf(this.getUsageFreq()));

        if (this.getId() > 0){
            //  existed payee
            params.put("id", String.valueOf(this.getId()));

        }

        return (Kman.getDB().updateData(this.getId() == 0, params) > 0);
    }

    /**
     * Returns category based on transaction type
     * Returns null if transaction type is not deposit nor withdrawal
     *
     * @param _type
     * @return
     */
    public Category getCategory(TransactionType _type){
        if (_type.getId() == TransactionType.ACCOUNT_TYPES_DEPOSIT){
            return Category.getCategory(this.categoryDepositId.get());
        }else if (_type.getId() == TransactionType.ACCOUNT_TYPES_WITHDRAWAL){
            return Category.getCategory(this.categoryWithdrawalId.get());
        }

        return null;
    }

    /**
     * Sets for payee category based on transaction type
     * Category will be set if payee's one is empty
     *
     * @param _category
     * @param _type
     * @param _saveDB Method saves data to DB
     * @return
     */
    public boolean setCategory(Category _category, TransactionType _type, boolean _saveDB){
        if (_category == null || _type == null) return false;

        int transactionTypeId = _type.getId();

        return setCategory(_category, transactionTypeId, _saveDB);
    }

    /**
     * The same as above but with transaction type id
     *
     * @param _category
     * @param _transactionTypeId
     * @param _saveDB save cnahges to DB or not
     * @return
     */
    public boolean setCategory(Category _category, int _transactionTypeId, boolean _saveDB){
        if (_category == null || _transactionTypeId <= 0) return false;

        int categoryId = _category.getId();

        //  the probability of this is close to 0 but anyway.
        if (categoryId <= 0) return false;

        boolean saveNeeded = false;
        if (_transactionTypeId == TransactionType.ACCOUNT_TYPES_DEPOSIT){
            if (this.getCategoryDepositId() == 0){
                //  category is not set yet
                this.categoryDepositId.set(categoryId);
                saveNeeded = true;
            }
        }else if (_transactionTypeId == TransactionType.ACCOUNT_TYPES_WITHDRAWAL){
            if (this.getCategoryWithdrawalId() == 0){
                //  category is not set yet
                this.categoryWithdrawalId.set(categoryId);
                saveNeeded = true;
            }
        }

        if (_saveDB && saveNeeded){
            return this.save();
        }

        return false;
    }
}
