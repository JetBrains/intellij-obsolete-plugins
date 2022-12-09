package org.jetbrains.jps.dmserver.model.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.dmserver.model.JpsDMContainerPackagingElement;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.artifact.impl.elements.JpsCompositePackagingElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.module.JpsModuleReference;

/**
 * @author michael.golubev
 */
public class JpsDMContainerPackagingElementImpl extends JpsCompositePackagingElementBase<JpsDMContainerPackagingElementImpl>
  implements JpsDMContainerPackagingElement {

  private static final JpsElementChildRole<JpsModuleReference> MODULE_REFERENCE_CHILD_ROLE
    = JpsElementChildRoleBase.create("module reference");

  public JpsDMContainerPackagingElementImpl(JpsModuleReference moduleReference) {
    myContainer.setChild(MODULE_REFERENCE_CHILD_ROLE, moduleReference);
  }

  public JpsDMContainerPackagingElementImpl(JpsDMContainerPackagingElementImpl original) {
    super(original);
  }

  @NotNull
  @Override
  public JpsDMContainerPackagingElementImpl createCopy() {
    return new JpsDMContainerPackagingElementImpl(this);
  }

  @Override
  @NotNull
  public JpsModuleReference getModuleReference() {
    return myContainer.getChild(MODULE_REFERENCE_CHILD_ROLE);
  }
}
