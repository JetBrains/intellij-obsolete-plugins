package com.intellij.seam.pages.xml.pages;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/pages:before-redirectAttrType enumeration.
 */
public enum BeforeRedirect implements NamedEnum {
	FALSE ("false"),
	TRUE ("true");

	private final String value;
	BeforeRedirect(String value) { this.value = value; }
	@Override
        public String getValue() { return value; }

}
