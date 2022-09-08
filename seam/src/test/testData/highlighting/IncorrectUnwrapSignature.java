import org.jboss.seam.annotations.Unwrap;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.Component;

import java.util.*;

@Name("authenticator")
class IncorrectUnwrapSignature {
    <error>@Unwrap</error> // static method not allowed
    private static Map createUniverse() { return null;}

    <error>@Unwrap</error> // must have no parameters
    private static Map createUniverse(int days) { return null;}

    <error>@Unwrap</error> // but single parameter of 'org.jboss.seam.Component' type allowed
    private static Map createUniverse(org.jboss.seam.Component component) { return null;}

    @Unwrap
    private String createString() { return null;}

    <error>@Unwrap</error>
    private void createString2() {}
}