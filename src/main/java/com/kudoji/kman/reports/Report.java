package com.kudoji.kman.reports;

import javafx.beans.property.SimpleStringProperty;

/**
 * Report object to put data into TreeTableView
 */
public class Report {
    private SimpleStringProperty parameter;
    private SimpleStringProperty value;

    public Report(String parameter, String value){
        this.parameter = new SimpleStringProperty(parameter);
        this.value = new SimpleStringProperty(value);
    }

    public SimpleStringProperty parameterProperty(){
        return this.parameter;
    }

    public SimpleStringProperty valueProperty(){
        return this.value;
    }

    public String getParameter(){
        return this.parameter.get();
    }

    public String getValue(){
        return this.value.get();
    }

}
