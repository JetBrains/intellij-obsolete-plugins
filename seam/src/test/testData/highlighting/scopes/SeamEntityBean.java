import org.jboss.seam.annotations.*;
import org.jboss.seam.ScopeType;
import javax.ejb.*;

@Name("entityBean")
<error>@Scope(ScopeType.STATELESS)</error>
@Roles({
        @Role(name="role_1", scope=ScopeType.APPLICATION),
        @Role(name="role_2", scope=ScopeType.BUSINESS_PROCESS),
        @Role(name="role_3", scope=ScopeType.CONVERSATION),
        @Role(name="role_4", scope=ScopeType.EVENT),
        @Role(name="role_5", scope=ScopeType.PAGE),
        <error>@Role(name="role_6", scope=ScopeType.STATELESS)</error>,
        @Role(name="role_7", scope=ScopeType.SESSION),
        @Role(name="role_8", scope=ScopeType.UNSPECIFIED)
    })
class SeamEntityBean implements EntityBean {
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

