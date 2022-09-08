package com.intellij.seam.pages.xml.pages;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/pages:switchAttrType enumeration.
 */
public enum Switch implements NamedEnum {
	DISABLED ("disabled"),
	ENABLED ("enabled");

	private final String value;
	Switch(String value) { this.value = value; }
	@Override
        public String getValue() { return value; }
}
