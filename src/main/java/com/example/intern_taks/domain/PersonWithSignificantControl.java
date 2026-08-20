package com.example.intern_taks.domain;

public class PersonWithSignificantControl {

    private String name;
    private String natureOfControl;

    public PersonWithSignificantControl() {
    }

    public PersonWithSignificantControl(String name, String natureOfControl) {
        this.name = name;
        this.natureOfControl = natureOfControl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNatureOfControl() {
        return natureOfControl;
    }

    public void setNatureOfControl(String natureOfControl) {
        this.natureOfControl = natureOfControl;
    }
}