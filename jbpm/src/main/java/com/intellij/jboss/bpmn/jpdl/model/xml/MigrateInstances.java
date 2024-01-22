package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jbpm.org/4.3/jpdm:migrate-instancesElemType interface.
 */
public interface MigrateInstances extends JpdlDomElement {

  /**
   * Returns the value of the action child.
   *
   * @return the value of the action child.
   */
  @NotNull
  GenericAttributeValue<MigrationAction> getAction();


  /**
   * Returns the list of migration-handler children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:migration-handler documentation</h3>
   * The migration handler specifies the name of a class to be executed while migrating the process instance.
   * </pre>
   *
   * @return the list of migration-handler children.
   */
  @NotNull
  List<MigrationHandler> getMigrationHandlers();

  /**
   * Adds new child to the list of migration-handler children.
   *
   * @return created child
   */
  MigrationHandler addMigrationHandler();


  /**
   * Returns the list of activity-mapping children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:activity-mapping documentation</h3>
   * One activity mapping will be present for each activity of which the name changed.
   * </pre>
   *
   * @return the list of activity-mapping children.
   */
  @NotNull
  List<ActivityMapping> getActivityMappings();

  /**
   * Adds new child to the list of activity-mapping children.
   *
   * @return created child
   */
  ActivityMapping addActivityMapping();
}
