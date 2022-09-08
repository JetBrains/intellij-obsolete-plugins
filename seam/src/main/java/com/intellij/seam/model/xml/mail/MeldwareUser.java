// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/mail

package com.intellij.seam.model.xml.mail;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.model.xml.components.MultiValuedProperty;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jboss.com/products/seam/mail:meldware-userElemType interface.
 */
public interface MeldwareUser extends SeamMailDomElement, BasicSeamComponent {
	/**
	 * Returns the value of the username child.
	 * @return the value of the username child.
	 */
	@NotNull
	GenericAttributeValue<String> getUsername();


	/**
	 * Returns the value of the password child.
	 * @return the value of the password child.
	 */
	@NotNull
	GenericAttributeValue<String> getPassword();


	/**
	 * Returns the value of the admin child.
	 * @return the value of the admin child.
	 */
	@NotNull
	GenericAttributeValue<Boolean> getAdmin();


	/**
	 * Returns the list of aliases children.
	 * @return the list of aliases children.
	 */
	@NotNull
	List<MultiValuedProperty> getAliaseses();
	/**
	 * Adds new child to the list of aliases children.
	 * @return created child
	 */
	MultiValuedProperty addAliases();


}
