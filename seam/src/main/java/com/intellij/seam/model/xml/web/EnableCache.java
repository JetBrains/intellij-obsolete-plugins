// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/web

package com.intellij.seam.model.xml.web;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/web:enable-cacheAttrType enumeration.
 */
public enum EnableCache implements NamedEnum {
	FALSE ("false"),
	TRUE ("true");

	private final String value;
	EnableCache(String value) { this.value = value; }
	@Override
        public String getValue() { return value; }

}
