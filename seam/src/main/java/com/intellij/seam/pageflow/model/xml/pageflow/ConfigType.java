
package com.intellij.seam.pageflow.model.xml.pageflow;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/pageflow:config-typeAttrType enumeration.
 */
public enum ConfigType implements NamedEnum {
	BEAN ("bean"),
	CONFIGURATION_PROPERTY ("configuration-property"),
	CONSTRUCTOR ("constructor"),
	FIELD ("field");

	private final String value;
	ConfigType(String value) { this.value = value; }
	@Override
        public String getValue() { return value; }

}
