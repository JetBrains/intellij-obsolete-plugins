package org.intellij.j2ee.web.resin.resin.configuration;

import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.NullableLazyValue;
import org.intellij.j2ee.web.resin.resin.ResinInstallation;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author michael.golubev
 */
public class Resin31ConfigurationStrategy extends Resin3XConfigurationStrategy {

  @NonNls
  protected static final String CLUSTER_ELEMENT = "cluster";
  @NonNls
  protected static final String CLUSTER_DEFAULT_ELEMENT = "cluster-default";

  public Resin31ConfigurationStrategy(ResinInstallation resinInstallation) {
    super(resinInstallation);
  }

  @Override
  protected ElementsProvider createElementsProvider() {
    return new Resin31ElementsProvider(getElement());
  }

  protected static class Resin31ElementsProvider extends ElementsProvider {

    private final NotNullLazyValue<Element> myCluster = NotNullLazyValue.lazy(() -> {
      return getRootElement().getChild(CLUSTER_ELEMENT, getNS());
    });
    private final NotNullLazyValue<Element> myClusterDefault = NotNullLazyValue.lazy(() -> {
      return doGetClusterDefaultElement();
    });
    private final NullableLazyValue<Element> myServerDefault = new NullableLazyValue<>() {

      @Override
      protected Element compute() {
        return getClusterElement().getChild(SERVER_DEFAULT_ELEMENT, getNS());
      }
    };

    public Resin31ElementsProvider(Element element) {
      super(element);
    }

    @NotNull
    public Element getClusterElement() {
      return myCluster.getValue();
    }

    @NotNull
    public Element getClusterDefaultElement() {
      return myClusterDefault.getValue();
    }

    @Nullable
    public Element getServerDefaultElement() {
      return myServerDefault.getValue();
    }

    //Resin 3.1
    //      <root>
    //          <cluster>
    //              <host>
    @Override
    public Element getHostParent() {
      return getClusterElement();
    }

    protected Element doGetClusterDefaultElement() {
      return getOrCreateChildElement(getRootElement(), CLUSTER_DEFAULT_ELEMENT);
    }

    @Override
    protected Element doGetParamParent() {
      Element serverElement = getServerElement();
      if (serverElement != null) {
        return serverElement;
      }
      Element serverDefaultElement = getServerDefaultElement();
      if (serverDefaultElement != null) {
        return serverDefaultElement;
      }
      return getOrCreateChildElement(getClusterDefaultElement(), SERVER_DEFAULT_ELEMENT);
    }
  }
}
