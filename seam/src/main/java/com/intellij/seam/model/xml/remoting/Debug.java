// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/remoting

package com.intellij.seam.model.xml.remoting;

import com.intellij.util.xml.NamedEnum;

/**
 * http://jboss.com/products/seam/remoting:debugAttrType enumeration.
 */
public enum Debug implements NamedEnum {
	FALSE ("false"),
	TRUE ("true");

	private final String value;
	Debug(String value) { this.value = value; }
	@Override
        public String getValue() { return value; }

}
