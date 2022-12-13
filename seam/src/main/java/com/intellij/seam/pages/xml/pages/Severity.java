package com.intellij.seam.pages.xml.pages;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/pages:severityAttrType enumeration.
 */
public enum Severity implements NamedEnum {
	ERROR ("ERROR"),
	FATAL ("FATAL"),
	INFO ("INFO"),
	WARN ("WARN"),
	ERROR_LOWCASE("error"),
	FATAL_LOWCASE("fatal"),
	INFO_LOWCASE("info"),
	WARN_LOWCASE("warn");

	private final String value;
	Severity(String value) { this.value = value; }
	@Override
        public String getValue() { return value; }

}
