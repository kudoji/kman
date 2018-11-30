package com.kudoji.kman.models;

import java.util.HashMap;

import com.kudoji.kman.Kman;
import javafx.collections.FXCollections;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/**
 *
 * @author kudoji
 */
public class Category {
    //  id of transfer category
    public final static int CATEGORY_TRANSFER_ID = 1;
    private int id;
    private String name, fullPath;
    private int categories_id;

    /**
     * Keeps information about payees which allows reduce DB usage
     */
    private final static javafx.collections.ObservableList<Category> categoriesCache = FXCollections.observableArrayList();

    public Category(){
        this.id = 0;
        this.name = "Categories";
        this.fullPath = "";
        this.categories_id = 0;
    }
    
    public Category(int _id, String _name){
        if (_id <= 0 || _name == null) throw new IllegalArgumentException();

        this.id = _id;
        this.name = _name;
        this.fullPath = this.name;
        this.categories_id = 0;
    }
    
    public Category(HashMap<String, String> _params){
        if (_params == null) throw new IllegalArgumentException();

        this.id = Integer.parseInt(_params.get("id"));
        this.name = _params.get("name");
        this.fullPath = this.name;
        String parent_id = _params.get("categories_id");
        if (parent_id == null) parent_id = "0";
        this.categories_id = Integer.parseInt(parent_id);
    }
    
    public void setFields(HashMap<String, String> _params){
        if (_params == null) throw new IllegalArgumentException();

//        this.id = Integer.parseInt(_params.get("id"));
        this.name = _params.get("name");
        this.fullPath = this.name;
        String parent_id = _params.get("categories_id");
        if (parent_id == null) parent_id = "0";
        this.categories_id = Integer.parseInt(parent_id);
    }

    /**
     * Returns category ID
     * @return 
     */
    public int getId(){
        return this.id;
    }
    
    public String getName(){
        return this.name;
    }
    
    /**
     * Returns parent category for the current one
     * 
     * @return 
     */
    public int getParentID(){
        return this.categories_id;
    }
    
    public void setFullPath(String _fullPath){
        this.fullPath = _fullPath;
    }
    
    public String getFullPath(){
        return this.fullPath;
    }
    
    /**
     * Returns category object by particular _id
     * Returns null if _id is incorrect or not found
     *
     * @param _id
     * @return 
     */
    public static Category getCategory(int _id){
        if (_id <= 0) return null;

        HashMap<String, String> params = new HashMap<>();
        params.put("table", "categories");
        params.put("id", Integer.toString(_id));
        
        java.util.ArrayList<HashMap<String, String>> rows = Kman.getDB().selectData(params); //should be only one row
        
        if (rows.size() == 1){
            return new Category(rows.get(0));
        }else{
            return null;
        }
    }

    /**
     * Returns category id based on Category instance
     *
     * @param _category
     * @return category id or 0 if _category is not set
     */
    public static int getCategoryId(Category _category){
        if (_category == null){
            return 0;
        }else{
            return _category.getId();
        }
    }
    
    @Override
    public String toString(){
        return this.name;
    }
    
    /**
     * Finds parent node for particular with category ID
     * @return
     */
    private static TreeItem<Category> findParentRecursively(TreeItem<Category> _tiRoot, Category _category){
        TreeItem<Category> tiResult = null;
        
        int parentID = _category.getParentID();
        for (TreeItem<Category> parentNode: _tiRoot.getChildren()){
            if (parentNode.getValue().getId() == parentID){
                tiResult = parentNode;
                
                _category.setFullPath(tiResult.getValue().getFullPath() + ":" + _category.getFullPath());
                
                return tiResult;
            }else{
                if (!parentNode.isLeaf()){
                    tiResult = findParentRecursively(parentNode, _category);
                    if (tiResult != null){ //item found!
                        return tiResult;
                    }
                }
            }
        }
        
        return tiResult;
    }
    
    /**
     * Read categories from DB and populates _tvCategories TreeView
     * @param _tvCategories
     * @param _idSelect select this id if more than zero
     */
    public static void populateCategoriesTree(TreeView<Category> _tvCategories, int _idSelect){
        Category cRoot = new Category();
        
        TreeItem<Category> tiRoot = new TreeItem<>(cRoot);
        tiRoot.setExpanded(true);
        
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "categories");
        params.put("order", "categories_id asc, id asc");
        java.util.ArrayList<HashMap<String, String>> rows = Kman.getDB().selectData(params);
        
        int currentParentID = cRoot.getParentID();
        TreeItem<Category> selectCategory = null;   //  select this category item in the list
        TreeItem<Category> currentItem = tiRoot;
        for (int i = 0; i < rows.size(); i++){
            Category category = new Category(rows.get(i));
            TreeItem<Category> tiCategory = new TreeItem<>(category);
            
            if (category.getParentID() == currentParentID){
                if (currentItem.getValue().getFullPath().equals("")){
                    category.setFullPath(category.getFullPath());
                }else{
                    category.setFullPath(currentItem.getValue().getFullPath() + ":" + category.getFullPath());
                }
            }else{
                TreeItem<Category> currentItemTmp = findParentRecursively(tiRoot, category);
                if (currentItemTmp != null){
                    currentParentID = category.getParentID();
                    currentItem = currentItemTmp;
                }
            }
            
            currentItem.getChildren().add(tiCategory);
            currentItem.setExpanded(true);
            if (_idSelect > 0 && category.getId() == _idSelect){
                //  select current category in the list
                selectCategory = tiCategory;
            }
        }
        
        _tvCategories.setRoot(tiRoot);
        if (selectCategory != null){
            //  there is a category to select
            _tvCategories.getSelectionModel().select(selectCategory);
            _tvCategories.scrollTo(_tvCategories.getSelectionModel().getSelectedIndex());
        }else{
            //  otherwise select root item
            _tvCategories.getSelectionModel().select(tiRoot);
        }
        //  set focus to categories tree
        _tvCategories.requestFocus();
    }

    public static javafx.collections.ObservableList<Category> getCategories(){
        if (Category.categoriesCache.isEmpty()){ //never filled, need to get data from DB
            HashMap<String, String> params = new HashMap<>();
            params.put("table", "categories");
            params.put("order", "name asc");

            java.util.ArrayList<HashMap<String, String>> rows = Kman.getDB().selectData(params);
            for (HashMap<String, String> row: rows){
                Category.categoriesCache.add(new Category(row));
            }
        }

        return Category.categoriesCache;
    }
}
