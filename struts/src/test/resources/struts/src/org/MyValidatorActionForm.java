package org;

import org.apache.struts.validator.ValidatorActionForm;

import java.util.List;

public class MyValidatorActionForm extends ValidatorActionForm {
    private String myUsername;
    private String myPassword;

    private List<Bean> myBeans;

    private Bean[] myBeanArray;

    public String getPassword() {
        return myPassword;
    }

    public void setPassword(String myPassword) {
        this.myPassword = myPassword;
    }

    public String getUsername() {
        return myUsername;
    }

    public void setUsername(String myUsername) {
        this.myUsername = myUsername;
    }

    public static void hi() {}

    public List<Bean> getBeansList() {
        return myBeans;
    }

    public void setBeansList(List<Bean> myBeans) {
        this.myBeans = myBeans;
    }

    public Bean[] getBeansArray() {
        return myBeanArray;
    }

    public void setBeansArray(Bean[] myBeanArray) {
        this.myBeanArray = myBeanArray;
    }
}