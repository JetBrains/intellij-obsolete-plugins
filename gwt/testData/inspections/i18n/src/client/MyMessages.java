package client;

public interface MyMessages extends com.google.gwt.i18n.client.Messages {
  String myKey();

  @com.google.gwt.i18n.client.Messages.DefaultMessage("default")
  String withDefault();

  String undeclared();
}