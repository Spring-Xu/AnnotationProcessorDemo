package com.compiler;

public class StringBinding {
    private String value;
    private String name;
    public StringBinding(String name,String value){
        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }
}
