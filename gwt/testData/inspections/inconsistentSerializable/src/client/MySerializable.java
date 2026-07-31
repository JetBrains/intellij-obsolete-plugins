public class MySerializable implements com.google.gwt.user.client.rpc.IsSerializable {
  private int myA;
  private String myString;
  private transient Class myClass;
  private static final Class ourStaticClass = MySerializable.class;

  public MySerializable() {
  }

  public MySerializable(int a) {
     myA = a;
  }
}

