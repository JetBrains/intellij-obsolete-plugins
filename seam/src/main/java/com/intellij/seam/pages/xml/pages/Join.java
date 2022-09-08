package com.intellij.seam.pages.xml.pages;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/pages:joinAttrType enumeration.
 */
public enum Join implements NamedEnum {
	FALSE ("false"),
	TRUE ("true");

	private final String value;
	Join(String value) { this.value = value; }
	@Override
        public String getValue() { return value; }

}
