package com.intellij.seam.pages.xml.pages;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/pages:login-requiredAttrType enumeration.
 */
public enum LoginRequired implements NamedEnum {
	FALSE ("false"),
	TRUE ("true");

	private final String value;
	LoginRequired(String value) { this.value = value; }
	@Override
        public String getValue() { return value; }

}
