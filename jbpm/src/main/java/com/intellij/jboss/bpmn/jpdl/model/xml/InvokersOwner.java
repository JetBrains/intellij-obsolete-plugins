package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup.JavaActivity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface InvokersOwner extends JpdlDomElement {
  /**
   * Returns the list of hql children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:hql documentation</h3>
   * Performs a hibernate query
   * </pre>
   *
   * @return the list of hql children.
   */
  @NotNull
  List<Hql> getHqls();

  /**
   * Adds new child to the list of hql children.
   *
   * @return created child
   */
  Hql addHql();


  /**
   * Returns the list of sql children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:sql documentation</h3>
   * Performs a hibernate SQL query
   * </pre>
   *
   * @return the list of sql children.
   */
  @NotNull
  List<Sql> getSqls();

  /**
   * Adds new child to the list of sql children.
   *
   * @return created child
   */
  Sql addSql();

  /**
   * Returns the list of java children.
   *
   * @return the list of java children.
   */
  @NotNull
  List<JavaActivity> getJavas();

  /**
   * Adds new child to the list of java children.
   *
   * @return created child
   */
  JavaActivity addJava();

  /**
   * Returns the list of script children.
   *
   * @return the list of script children.
   */
  @NotNull
  List<Script> getScripts();

  /**
   * Adds new child to the list of script children.
   *
   * @return created child
   */
  Script addScript();


  /**
   * Returns the list of mail children.
   *
   * @return the list of mail children.
   */
  @NotNull
  List<Mail> getMails();

  /**
   * Adds new child to the list of mail children.
   *
   * @return created child
   */
  Mail addMail();
}
