import java.util.*;

public interface MyService extends com.google.gwt.user.client.rpc.RemoteService {
  String m(Date d);

  void m(Math m);

  void m(int i) throws MyException;

  /**
  * @gwt.typeArgs   <java.lang.String>
  * asdde
  * @gwt.typeArgs     b    <java.lang.String>
  */
  Collection aa1(Set b);

  /**
  * @gwt.typeArgs <java.lang.Math>
  */
  List aa2(String b);

  Collection aa3(String b);

  /**
  * @gwt.typeArgs a <java.lang.String>
  */
  Collection aa4(String b);

  /**
  * @gwt.typeArgs <java.lang.String>
  */
  String aa5(List b);

  /**
  * @gwt.typeArgs c <java.lang.String>
  */
  String aa6(List b);

  /**
  * @gwt.typeArgs b <java.lang.Str>
  */
  String aa7(List b);

  /**
  * @gwt.typeArgs b qwerty
  */
  String aa8(List b);

  Collection<String> aa9();

  void aa10() throws Throwable;

  void aa11() throws Exception;

  void aa12() throws ArrayIndexOutOfBoundsException;

  class MyException extends Exception implements com.google.gwt.user.client.rpc.IsSerializable {}
}