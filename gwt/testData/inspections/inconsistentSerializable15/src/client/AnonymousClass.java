package client;

import java.io.Serializable;

public abstract class AnonymousClass implements Serializable {

  public static void main() {
    new AnonymousClass() {
      AnonymousClass() {
      }
    }
  }
}