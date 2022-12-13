package com.intellij.seam.pageflow.model.xml.pageflow;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/pageflow:booleanType enumeration.
 */
public enum Boolean implements NamedEnum {
	FALSE ("false"),
	NO ("no"),
	OFF ("off"),
	ON ("on"),
	TRUE ("true"),
	YES ("yes");

	private final String value;
	Boolean(String value) { this.value = value; }
	@Override
        public String getValue() { return value; }

}
