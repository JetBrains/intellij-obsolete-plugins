// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/drools

package com.intellij.seam.model.xml.drools;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/drools:managed-working-memoryElemType interface.
 */
public interface ManagedWorkingMemory extends BasicSeamComponent, SeamDroolsDomElement {

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
	 * Returns the value of the rule-base child.
	 * @return the value of the rule-base child.
	 */
	@NotNull
	GenericAttributeValue<String> getRuleBase();


}
