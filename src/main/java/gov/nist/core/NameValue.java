package gov.nist.core;

import java.util.Map.Entry;

/**
 * Generic structure for storing name-value pairs.
 */
public class NameValue extends GenericObject implements Entry<String,String> {
	private static final long serialVersionUID = -1857729012596437950L;

	protected boolean isQuotedString;
	protected final boolean isFlagParameter;

	private String separator;
	private String quotes;
	private String name;
	private Object value;

	public NameValue() {
		name = null;
		value = "";
		separator = Separators.EQUAL;

		this.quotes = "";
		this.isFlagParameter = false;
	}

	/**
	 * New constructor, taking a boolean which is set if the NV pair is a flag
	 *
	 * @param n
	 * @param v
	 * @param isFlag
	 */
	public NameValue(String n, Object v, boolean isFlag) {
		name = n;
		value = v;
		separator = Separators.EQUAL;
		quotes = "";

		this.isFlagParameter = isFlag;
	}

	/**
	 * Original constructor, sets isFlagParameter to 'false'
	 *
	 * @param n
	 * @param v
	 */
	public NameValue(String n, Object v) {
		this(n, v, false);
	}

	/**
	 * Set the separator for the encoding method below.
	 */
	public void setSeparator(String sep) {
		separator = sep;
	}

	/**
	 * A flag that indicates that doublequotes should be put around the value
	 * when encoded (for example name=value when value is doublequoted).
	 */
	public void setQuotedValue() {
		isQuotedString = true;

		this.quotes = Separators.DOUBLE_QUOTE;
	}

	/**
	 * Return true if the value is quoted in doublequotes.
	 */
	public boolean isValueQuoted() {
		return isQuotedString;
	}

	public String getName() {
		return name;
	}

	public Object getValueAsObject() {
		return getValueAsObject(true);
	}

	public Object getValueAsObject(boolean stripQuotes) {
		if(isFlagParameter) {
			return ""; // never return null for flag params
		}

		// Issue 315 : (https://jain-sip.dev.java.net/issues/show_bug.cgi?id=315)
		// header.getParameter() doesn't return quoted value
		if(!stripQuotes && isQuotedString) {
			return quotes + value.toString() + quotes; // add the quotes for quoted string
		}

		return value;
	}

	/**
	 * Set the name member
	 */
	public void setName(String n) {
		name = n;
	}

	/**
	 * Set the value member
	 */
	public void setValueAsObject(Object v) {
		value = v;
	}

	/**
	 * Get the encoded representation of this namevalue object. Added
	 * doublequote for encoding doublequoted values.
	 *
	 * Bug: RFC3261 stipulates that an opaque parameter in authenticate header
	 * has to be:
	 * opaque              =  "opaque" EQUAL quoted-string
	 * so returning just the name is not acceptable. (e.g. LinkSys phones
	 * are picky about this)
	 *
	 * @since 1.0
	 * @return an encoded name value (eg. name=value) string.
	 */
	public String encode() {
		return encode(new StringBuilder()).toString();
	}

	@Override
	public StringBuilder encode(StringBuilder buffer) {
		if(name != null && value != null && !isFlagParameter) {
			if(GenericObject.isMySubclass(value.getClass())) {
				GenericObject gv = (GenericObject) value;

				buffer.append(name).append(separator).append(quotes);
				gv.encode(buffer);
				buffer.append(quotes);

				return buffer;
			}

			if(GenericObjectList.isMySubclass(value.getClass())) {
				GenericObjectList gvlist = (GenericObjectList) value;
				buffer.append(name).append(separator).append(gvlist.encode());

				return buffer;
			}

			if(value.toString().length() == 0) {
				if(this.isQuotedString ) {
					buffer.append(name).append(separator).append(quotes).append(quotes);
					return buffer;
				}

				buffer.append(name).append(separator);

				return buffer;
			}

			buffer.append(name).append(separator).append(quotes).append(value.toString()).append(quotes);

			return buffer;
		}

		if(name == null && value != null) {
			if(GenericObject.isMySubclass(value.getClass())) {
				GenericObject gv = (GenericObject) value;

				gv.encode(buffer);

				return buffer;
			}

			if(GenericObjectList.isMySubclass(value.getClass())) {
				GenericObjectList gvlist = (GenericObjectList) value;
				buffer.append(gvlist.encode());

				return buffer;
			}

			buffer.append(quotes).append(value.toString()).append(quotes);

			return buffer;
		}

		if(name != null && value == null) {
			buffer.append(name);

			return buffer;
		}

		return buffer;
	}

	@Override
	public Object clone() {
		NameValue retval = (NameValue) super.clone();

		if(value != null) {
			retval.value = makeClone(value);
		}

		return retval;
	}

	/**
	 * Equality comparison predicate.
	 */
	@Override
	public boolean equals(Object other) {
		if(other == null) {
			return false;
		}

		if(!other.getClass().equals(this.getClass())) {
			return false;
		}

		NameValue that = (NameValue) other;
		if(this == that) {
			return true;
		}

		if(this.name == null && that.name != null || this.name != null && that.name == null) {
			return false;
		}

		if(this.name != null && that.name != null && this.name.compareToIgnoreCase(that.name) != 0) {
			return false;
		}

		if(this.value != null && that.value == null || this.value == null && that.value != null) {
			return false;
		}

		if(this.value == that.value) {
			return true;
		}

		if(value instanceof String) {
			// Quoted string comparisons are case sensitive.
			if(isQuotedString) {
				return this.value.equals(that.value);
			}

			String val = (String) this.value;
			String val1 = (String) that.value;

			return val.compareToIgnoreCase(val1) == 0;
		}

		if(null == value) {
			return null == that.value;
		}

		return this.value.equals(that.value);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map$Entry#getKey()
	 */
	public String getKey() {
		return this.name;
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map$Entry#getValue()
	 */
	public String getValue() {
		if(value == null) {
			return null;
		}

		return value.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map$Entry#setValue(java.lang.Object)
	 */
	public String setValue(String value) {
		String retval = this.value == null ? null : value;

		this.value = value;

		return retval;
	}

	@Override
	public int hashCode() {
		return this.encode().toLowerCase().hashCode();
	}
}
