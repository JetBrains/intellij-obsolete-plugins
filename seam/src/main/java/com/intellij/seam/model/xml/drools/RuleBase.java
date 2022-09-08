// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/drools

package com.intellij.seam.model.xml.drools;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.model.xml.components.MultiValuedProperty;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jboss.com/products/seam/drools:rule-baseElemType interface.
 */
public interface RuleBase extends SeamDroolsDomElement, BasicSeamComponent {

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
	 * Returns the value of the dsl-file child.
	 * @return the value of the dsl-file child.
	 */
	@NotNull
	GenericAttributeValue<String> getDslFile();


	/**
	 * Returns the value of the rule-files child.
	 * @return the value of the rule-files child.
	 */
	@NotNull
	GenericAttributeValue<String> getRuleFiles();


	/**
	 * Returns the list of rule-files children.
	 * @return the list of rule-files children.
	 */
	@NotNull
	List<MultiValuedProperty> getRuleFileses();
	/**
	 * Adds new child to the list of rule-files children.
	 * @return created child
	 */
	MultiValuedProperty addRuleFiles();


}
