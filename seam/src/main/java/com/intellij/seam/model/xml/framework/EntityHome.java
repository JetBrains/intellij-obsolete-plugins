// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/framework

package com.intellij.seam.model.xml.framework;

import com.intellij.psi.PsiClass;
import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jboss.com/products/seam/framework:entity-homeElemType interface.
 */
public interface EntityHome extends SeamFrameworkDomElement, BasicSeamComponent {

	/**
	 * Returns the value of the simple content.
	 * @return the value of the simple content.
	 */
	@NotNull
	@Required
	String getValue();
	/**
	 * Sets the value of the simple content.
	 * @param value the new value to set
	 */
	void setValue(@NotNull String value);


	/**
	 * Returns the value of the entity-class child.
	 * @return the value of the entity-class child.
	 */
	@NotNull
	GenericAttributeValue<PsiClass> getEntityClass();


	/**
	 * Returns the value of the new-instance child.
	 * @return the value of the new-instance child.
	 */
	@NotNull
	GenericAttributeValue<String> getNewInstance();


	/**
	 * Returns the value of the created-message child.
	 * @return the value of the created-message child.
	 */
	@NotNull
	GenericAttributeValue<String> getCreatedMessage();


	/**
	 * Returns the value of the updated-message child.
	 * @return the value of the updated-message child.
	 */
	@NotNull
	GenericAttributeValue<String> getUpdatedMessage();


	/**
	 * Returns the value of the deleted-message child.
	 * @return the value of the deleted-message child.
	 */
	@NotNull
	GenericAttributeValue<String> getDeletedMessage();


	/**
	 * Returns the value of the entity-manager child.
	 * @return the value of the entity-manager child.
	 */
	@NotNull
	GenericAttributeValue<String> getEntityManager();


	/**
	 * Returns the list of id children.
	 * @return the list of id children.
	 */
	@NotNull
	List<GenericDomValue<String>> getIds();
	/**
	 * Adds new child to the list of id children.
	 * @return created child
	 */
	GenericDomValue<String> addId();


	/**
	 * Returns the list of created-message children.
	 * @return the list of created-message children.
	 */
	@NotNull
	List<GenericDomValue<String>> getCreatedMessages();
	/**
	 * Adds new child to the list of created-message children.
	 * @return created child
	 */
	GenericDomValue<String> addCreatedMessage();


	/**
	 * Returns the list of updated-message children.
	 * @return the list of updated-message children.
	 */
	@NotNull
	List<GenericDomValue<String>> getUpdatedMessages();
	/**
	 * Adds new child to the list of updated-message children.
	 * @return created child
	 */
	GenericDomValue<String> addUpdatedMessage();


	/**
	 * Returns the list of deleted-message children.
	 * @return the list of deleted-message children.
	 */
	@NotNull
	List<GenericDomValue<String>> getDeletedMessages();
	/**
	 * Adds new child to the list of deleted-message children.
	 * @return created child
	 */
	GenericDomValue<String> addDeletedMessage();


	/**
	 * Returns the list of new-instance children.
	 * @return the list of new-instance children.
	 */
	@NotNull
	List<GenericDomValue<String>> getNewInstances();
	/**
	 * Adds new child to the list of new-instance children.
	 * @return created child
	 */
	GenericDomValue<String> addNewInstance();


}
