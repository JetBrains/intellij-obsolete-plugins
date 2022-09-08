import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.Name;
import java.util.*;
import org.jboss.seam.ScopeType;

@Name("dataModel")
class DataModelBean {
    <error>@DataModel(scope=ScopeType.APPLICATION)</error> private List dataModel;
    <error>@DataModel(scope=ScopeType.BUSINESS_PROCESS)</error> private List dataModel2;
    <error>@DataModel(scope=ScopeType.CONVERSATION)</error> private List dataModel3;
    <error>@DataModel(scope=ScopeType.EVENT)</error> private List dataModel4;
    @DataModel(scope=ScopeType.PAGE) private List dataModel5;
    <error>@DataModel(scope=ScopeType.STATELESS)</error> private List dataModel6;
    <error>@DataModel(scope=ScopeType.SESSION)</error> private List dataModel7;
    @DataModel(scope=ScopeType.UNSPECIFIED) private List dataModel8;
}