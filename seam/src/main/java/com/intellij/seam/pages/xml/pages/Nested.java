package com.intellij.seam.pages.xml.pages;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/pages:nestedAttrType enumeration.
 */
public enum Nested implements NamedEnum {
	FALSE ("false"),
	TRUE ("true");

	private final String value;
	Nested(String value) { this.value = value; }
	@Override
        public String getValue() { return value; }

}
