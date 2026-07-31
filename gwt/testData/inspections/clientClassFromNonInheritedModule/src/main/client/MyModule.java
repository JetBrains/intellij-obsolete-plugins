package main.client;

import dep1.client.*;
import dep2.client.*;

public class MyModule {
  public void method() {
    Dep1.m1();
    Dep2.m2();
  }
}