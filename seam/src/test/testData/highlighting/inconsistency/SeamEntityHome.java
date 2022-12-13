import javax.ejb.EJBHome;
import javax.ejb.FinderException;
import java.rmi.RemoteException;

interface SeamEntityHome extends EJBHome {
    SeamTes findByPrimaryKey(String key) throws RemoteException, FinderException;
}
