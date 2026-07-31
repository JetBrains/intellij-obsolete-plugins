import java.util.*;

public interface MyService extends com.google.gwt.user.client.rpc.RemoteService {
  /**
  * @gwt.typeArgs <java.lang.String>
  * @gwt.typeArgs x <java.lang.String>
  */
  Collection method(Set x);

  /**
  * @gwt.typeArgs <java.lang.String>
  */
  List method2(String b);

  Collection<String> method3(Set<String> aaa);
}