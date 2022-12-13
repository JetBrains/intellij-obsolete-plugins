// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/web

package com.intellij.seam.model.xml.web;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/web:create-temp-filesAttrType enumeration.
 */
public enum CreateTempFiles implements NamedEnum {
	FALSE ("false"),
	TRUE ("true");

	private final String value;
	CreateTempFiles(String value) { this.value = value; }
	@Override
        public String getValue() { return value; }

}
