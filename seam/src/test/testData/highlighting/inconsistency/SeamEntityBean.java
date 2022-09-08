import org.jboss.seam.annotations.*;
import javax.ejb.*;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;

@Name("entityBean")
<error>@JndiName("n")</error>
class SeamEntityBean implements EntityBean {
    <error>@DataModel</error> private java.util.List dataModel;

    <error>@Create</error>
    private void create(){}

    <error>@Destroy</error>
    private void destroy() {}

    public SeamEntityBean() {
    }

    public String ejbFindByPrimaryKey(String key) throws FinderException {
        return null;
    }

    public void setEntityContext(EntityContext entityContext) throws EJBException {
    }

    public void unsetEntityContext() throws EJBException {
    }

    public void ejbRemove() throws RemoveException, EJBException {
    }

    public void ejbActivate() throws EJBException {
    }

    public void ejbPassivate() throws EJBException {
    }

    public void ejbLoad() throws EJBException {
    }

    public void ejbStore() throws EJBException {
    }

}

