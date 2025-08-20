package com.intellij.play.completion.beans;

import com.intellij.psi.CommonClassNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NameValueDescriptor {
  private final String myName;
  private String myType;
  private boolean isRequired;
  private boolean isActionPreferred; // commonly tag uses action, for instance, #{a @MyController.index()}Back#{/a}


  // #{list items:MyController.products() ...}
  public static NameValueDescriptor create(@Nullable String name) {
    return new NameValueDescriptor(name);
  }
  
  public static NameValueDescriptor create(@Nullable String name, @Nullable String type) {
    return new NameValueDescriptor(name,type);
  }

  // #{if tasks.size() > 1}...
  public static NameValueDescriptor createExpression() {
    return new NameValueDescriptor();
  }
  
  // #{a @Application.logout()}Disconnect#{/a}
  public static NameValueDescriptor createAction() {
    return new NameValueDescriptor().setActionPreferred(true);
  }

  // #{foo name:@Application.logout()}Disconnect#{/a}
  public static NameValueDescriptor createAction(@NotNull String name) {
    return new NameValueDescriptor(name).setActionPreferred(true);
  }

  //#{cache 'startTime'} #{/cache} or #{extends 'main.html' /}
  public static NameValueDescriptor createStringExpression() {
    return new NameValueDescriptor(null, CommonClassNames.JAVA_LANG_STRING);  
  }
  
  // #{form method:'GET', id:'detailsForm'}
  public static NameValueDescriptor createStringExpression(@NotNull String name) {
    return new NameValueDescriptor(name, CommonClassNames.JAVA_LANG_STRING);
  }

  public boolean isActionPreferred() {
    return isActionPreferred;
  }

  public NameValueDescriptor setActionPreferred(boolean actionPreferred) {
    isActionPreferred = actionPreferred;
    return this;
  }

  public NameValueDescriptor() {
    this(null);
  }

  public NameValueDescriptor(@Nullable String name) {
    this(name, CommonClassNames.JAVA_LANG_OBJECT);
  }
  
  public NameValueDescriptor(@Nullable String name, @Nullable String type) {
    this.myName = name;
    this.myType = type;
  }
  
  @Nullable
  public String getName() {
    return myName;
  }

  public NameValueDescriptor setRequired(boolean required) {
    isRequired = required;
    return this;
  }

  public boolean isRequired() {
    return isRequired;
  }

  @NotNull
  public String getType() {
    return myType == null ? CommonClassNames.JAVA_LANG_OBJECT : myType;
  }

  public NameValueDescriptor setType(@Nullable String type) {
    myType = type;
    return this;
  }

  public String getTailText() {
     return ":" + (isStringExpression() ? "''": (isActionPreferred()? "@" : ""));
  }

  public boolean isStringExpression() {
    return CommonClassNames.JAVA_LANG_STRING.equals(getType());
  }

  public String getPresentableText() {
     return getName() + ":" + (isStringExpression() ? "''": (isActionPreferred()? "@'action'" : "expression"));
  }
}
