package com.kudoji.kman.models;

import java.util.HashMap;

import com.kudoji.kman.Kman;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/**
 *
 * @author kudoji
 */
public class Category {
    private int id;
    private String name, fullPath;
    private int categories_id;
    
    public Category(){
        this.id = 0;
        this.name = "Categories";
        this.fullPath = "";
        this.categories_id = 0;
    }
    
    public Category(int _id, String _name){
        this.id = _id;
        this.name = _name;
        this.fullPath = this.name;
        this.categories_id = 0;
    }
    
    public Category(HashMap<String, String> _params){
        this.id = Integer.parseInt(_params.get("id"));
        this.name = _params.get("name");
        this.fullPath = this.name;
        String parent_id = _params.get("categories_id");
        if (parent_id == null) parent_id = "0";
        this.categories_id = Integer.parseInt(parent_id);
    }
    
    public void setFields(HashMap<String, String> _params){
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
    public int getID(){
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
     * @param _id
     * @return 
     */
    public static Category getCategory(int _id){
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
    
    @Override
    public String toString(){
        return this.name;
    }
    
    /**
     * Finds parent node for particular with category ID
     * @param _parentID
     * @return 
     */
    private static TreeItem<Category> findParentRecursively(TreeItem<Category> _tiRoot, Category _category){
        TreeItem<Category> tiResult = null;
        
        int parentID = _category.getParentID();
        for (TreeItem<Category> parentNode: _tiRoot.getChildren()){
            if (parentNode.getValue().getID() == parentID){
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
     */
    public static void populateCategoriesTree(TreeView<Category> _tvCategories){
        Category cRoot = new Category();
        
        TreeItem<Category> tiRoot = new TreeItem<>(cRoot);
        tiRoot.setExpanded(true);
        
        HashMap<String, String> params = new HashMap<>();
        params.put("table", "categories");
        params.put("order", "categories_id asc, id asc");
        java.util.ArrayList<HashMap<String, String>> rows = Kman.getDB().selectData(params);
        
        int currentParentID = cRoot.getParentID();
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
        }
        
        _tvCategories.setRoot(tiRoot);
    }
}
