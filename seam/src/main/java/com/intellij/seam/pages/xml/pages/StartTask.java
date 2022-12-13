package com.intellij.seam.pages.xml.pages;


import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/pages:start-taskElemType interface.
 */
public interface StartTask extends SeamPagesDomElement {

	/**
	 * Returns the value of the task-id child.
	 * @return the value of the task-id child.
	 */
	@NotNull
	GenericAttributeValue<String> getTaskId();


	/**
	 * Returns the value of the pageflow child.
	 * @return the value of the pageflow child.
	 */
	@NotNull
	GenericAttributeValue<String> getPageflow();


	/**
	 * Returns the value of the flush-mode child.
	 * @return the value of the flush-mode child.
	 */
	@NotNull
	GenericAttributeValue<FlushMode> getFlushMode();


}
