import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.Name;
import java.util.*;

@Name("authenticator")
class IncorrectDataModelSignature {
    @DataModel private List dataModel;
    @DataModel private Map dataModel2;
    @DataModel private Set dataModel3;
    @DataModel private String[] dataMode4;

    <error>@DataModel</error> private String dataMode5;

    <error>@DataModel</error> // not getter
    private Map method() {return null;}

    <error>@DataModel</error> // static getter
    private static Map getMethod() { return null;}

    <error>@DataModel</error> // Map, List, Set, arrays allowed
    private String getMethod2() {return null;}

    @DataModel
    private Map getMapMethod() {return null;}

    @DataModel
    private Map<String, String> getMapMethod2() {return null;}

    @DataModel
    private List getListMethod() {return null;}

    @DataModel
    private Set getSetMethod() {return null;}

    @DataModel
    private ArrayList<String> getGenericListMethod() {return null;}

    @DataModel
    private String[] getArrayMethod() {return null;}
}