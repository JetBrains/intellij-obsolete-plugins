// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/mail

package com.intellij.seam.model.xml.mail;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.model.xml.components.MultiValuedProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jboss.com/products/seam/mail:meldwareElemType interface.
 */
public interface Meldware extends SeamMailDomElement, BasicSeamComponent {

	/**
	 * Returns the list of users children.
	 * @return the list of users children.
	 */
	@NotNull
	List<MultiValuedProperty> getUserses();
	/**
	 * Adds new child to the list of users children.
	 * @return created child
	 */
	MultiValuedProperty addUsers();


}
