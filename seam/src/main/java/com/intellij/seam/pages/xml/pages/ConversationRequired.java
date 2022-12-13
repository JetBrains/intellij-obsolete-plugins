package com.intellij.seam.pages.xml.pages;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/pages:conversation-requiredAttrType enumeration.
 */
public enum ConversationRequired implements NamedEnum {
	FALSE ("false"),
	TRUE ("true");

	private final String value;
	ConversationRequired(String value) { this.value = value; }
	@Override
        public String getValue() { return value; }

}
