package com.example.childhomeworktracker.model;
import java.util.List;
public class Parent {
    private String phoneNumber;
    private List<Child> children;
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public List<Child> getChildren() { return children; }
    public void setChildren(List<Child> children) { this.children = children; }
}
