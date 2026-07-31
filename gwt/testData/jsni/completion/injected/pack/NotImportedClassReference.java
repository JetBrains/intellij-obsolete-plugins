package pack;

public class NotImportedClassReference {
  public native void x() /*-{
     @ClassFromAnotherPackag<caret>
  }-*/;
}