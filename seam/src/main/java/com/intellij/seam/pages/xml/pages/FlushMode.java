package com.intellij.seam.pages.xml.pages;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/pages:flush-modeAttrType enumeration.
 */
public enum FlushMode implements NamedEnum {
	AUTO ("AUTO"),
	COMMIT ("COMMIT"),
	MANUAL ("MANUAL"),
	AUTO_LOWCASE ("auto"),
	COMMIT_LOWCASEException ("commit"),
	MANUAL_LOWCASE ("manual");

	private final String value;
	FlushMode(String value) { this.value = value; }
	@Override
        public String getValue() { return value; }

}
