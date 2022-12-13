import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.Component;
import java.util.Map;

import java.util.*;

@Name("authenticator")
class IncorrectFactorySignature {
    @Factory   //could be static
    public static Map getUniverse1() { return null;}

    <error>@Factory</error> // must be public
    private Map getUniverse2() { return null;}

    <error>@Factory</error> // must have no parameters
    public Map getUniverse3(int days) { return null;}

    @Factory // but single parameter of 'org.jboss.seam.Component' type allowed
    public Map getUniverse4(org.jboss.seam.Component component) { return null;}

    <error>@Factory</error> //  must have JavaBean-style name
    public String tralala() { return "tralala"; }

    @Factory("factory_name") // valid if context variable defined explicitly
    public String createString() { return "factory_name";}

    @Factory("factory_name2") // valid if context variable defined explicitly
    public void createString2() {
        // Contexts.getApplicationContext().set("factory_name2", "factory_name2");
    }
}