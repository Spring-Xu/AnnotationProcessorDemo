package com.compiler;

public class IntBinding {
    private int value;
    private String name;
    public IntBinding(String name, int value){
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }
}
