// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/spring

package com.intellij.seam.model.xml.spring;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/spring:task-executor-dispatcherElemType interface.
 */
public interface TaskExecutorDispatcher extends SeamSpringDomElement, BasicSeamComponent {

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
	 * Returns the value of the task-executor child.
	 * @return the value of the task-executor child.
	 */
	@NotNull
	@Required
	GenericAttributeValue<String> getTaskExecutor();


	/**
	 * Returns the value of the schedule-dispatcher child.
	 * @return the value of the schedule-dispatcher child.
	 */
	@NotNull
	GenericAttributeValue<String> getScheduleDispatcher();
}
