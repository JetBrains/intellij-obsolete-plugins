// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/spring

package com.intellij.seam.model.xml.spring;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/spring:spring-transactionElemType interface.
 */
public interface SpringTransaction extends SeamSpringDomElement, BasicSeamComponent {

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
	 * Returns the value of the platform-transaction-manager child.
	 * <pre>
	 * <h3>Attribute null:platform-transaction-manager documentation</h3>
	 *                         	An expression evalutating to the spring platform transaction manager
	 *                         
	 * </pre>
	 * @return the value of the platform-transaction-manager child.
	 */
	@NotNull
	GenericAttributeValue<String> getPlatformTransactionManager();


	/**
	 * Returns the value of the conversation-context-required child.
	 * <pre>
	 * <h3>Attribute null:conversation-context-required documentation</h3>
	 * Specify if this transaction manager requires a conversation context to be available or not.
	 * 						Set to true if you're using a JpaTransactionManager with a conversation scoped persistence
	 * 						context.
	 * </pre>
	 * @return the value of the conversation-context-required child.
	 */
	@NotNull
	GenericAttributeValue<Boolean> getConversationContextRequired();


	/**
	 * Returns the value of the join-transaction child.
	 * <pre>
	 * <h3>Attribute null:join-transaction documentation</h3>
	 * Should this transaction manager participate in request to join a transaction.  For JTA
	 * 						transactions set to true.
	 * </pre>
	 * @return the value of the join-transaction child.
	 */
	@NotNull
	GenericAttributeValue<Boolean> getJoinTransaction();


}
