package gov.nist.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

/**
 * This is a Duplicate Name Value List that will allow multiple values map to the same key.
 * 
 * The parsing and encoding logic for it is the same as that of NameValueList. Only the HashMap
 * container is different.
 */
public class DuplicateNameValueList implements Serializable, Cloneable {
	private static final long serialVersionUID = -5611332957903796952L;

	private MultiValueMapImpl<NameValue> nameValueMap = new MultiValueMapImpl<>();

	/**
	 * Encode the list in semicolon separated form.
	 * 
	 * @return an encoded string containing the objects in this list.
	 */
	public String encode() {
		return encode(new StringBuilder()).toString();
	}

	public StringBuilder encode(StringBuilder buffer) {
		if (!nameValueMap.isEmpty()) {
			Iterator<NameValue> iterator = nameValueMap.values().iterator();

			if(iterator.hasNext()) {
				while (true) {
					Object obj = iterator.next();
					if(obj instanceof GenericObject) {
						GenericObject gobj = (GenericObject) obj;
						gobj.encode(buffer);
					} else {
						buffer.append(obj.toString());
					}

					if(iterator.hasNext())
						buffer.append(Separators.SEMICOLON);
					else
						break;
				}
			}
		}

		return buffer;
	}

	public String toString() {
		return this.encode();
	}

	/**
	 * Set a name-value object in this list.
	 */

	public void set(NameValue nv) {
		this.nameValueMap.put(nv.getName().toLowerCase(), nv);
	}

	/**
	 * Set a namevalue object in this list.
	 */
	public void set(String name, Object value) {
		NameValue nameValue = new NameValue(name, value);
		nameValueMap.put(name.toLowerCase(), nameValue);
	}

	/**
	 * Compare if two NameValue lists are equal.
	 * 
	 * @param otherObject is the object to compare to.
	 * @return true if the two objects compare for equality.
	 */
	public boolean equals(Object otherObject) {
		if(null == otherObject) {
			return false;
		}

		if(!otherObject.getClass().equals(this.getClass())) {
			return false;
		}

		DuplicateNameValueList other = (DuplicateNameValueList) otherObject;

		if(nameValueMap.size() != other.nameValueMap.size()) {
			return false;
		}

		Iterator<String> li = this.nameValueMap.keySet().iterator();

		while(li.hasNext()) {
			String key = (String) li.next();

			Collection nv1 = this.getNameValue(key);
			Collection nv2 = (Collection) other.nameValueMap.get(key);

			if(null == nv2) {
				return false;
			}

			if(!nv2.equals(nv1)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Do a lookup on a given name and return value associated with it.
	 */
	public Object getValue(String name) {
		return this.getNameValue(name.toLowerCase());
	}

	/**
	 * Get the NameValue record given a name.
	 * 
	 */
	public Collection getNameValue(String name) {
		return (Collection) this.nameValueMap.get(name.toLowerCase());
	}

	/**
	 * Returns a boolean telling if this NameValueList has a record with this name
	 */
	public boolean hasNameValue(String name) {
		return nameValueMap.containsKey(name.toLowerCase());
	}

	/**
	 * Remove the element corresponding to this name.
	 */
	public boolean delete(String name) {
		String lcName = name.toLowerCase();

		if(this.nameValueMap.containsKey(lcName)) {
			this.nameValueMap.remove(lcName);

			return true;
		}

		return false;
	}

	public Object clone() {
		DuplicateNameValueList retval = new DuplicateNameValueList();
		Iterator<NameValue> it = this.nameValueMap.values().iterator();

		while(it.hasNext()) {
			retval.set((NameValue) ((NameValue) it.next()).clone());
		}

		return retval;
	}

	/**
	 * Return an iterator for the name-value pairs of this list.
	 * 
	 * @return the iterator.
	 */
	public Iterator<NameValue> iterator() {
		return this.nameValueMap.values().iterator();
	}

	/**
	 * Get a list of parameter names.
	 * 
	 * @return a list iterator that has the names of the parameters.
	 */
	public Iterator<String> getNames() {
		return this.nameValueMap.keySet().iterator();
	}

	/**
	 * Get the parameter as a String.
	 * 
	 * @return the parameter as a string.
	 */
	public String getParameter(String name) {
		Object val = this.getValue(name);

		if(val == null) {
			return null;
		}

		if(val instanceof GenericObject) {
			return ((GenericObject) val).encode();
		}

		return val.toString();
	}

	public void clear() {
		nameValueMap.clear();
	}

	public boolean isEmpty() {
		return this.nameValueMap.isEmpty();
	}

	public NameValue put(String key, NameValue value) {
		return (NameValue) this.nameValueMap.put(key, value);
	}

	public NameValue remove(Object key) {
		return (NameValue) this.nameValueMap.remove(key);
	}

	public int size() {
		return this.nameValueMap.size();
	}

	public Collection<NameValue> values() {
		return this.nameValueMap.values();
	}

	public int hashCode() {
		return this.nameValueMap.keySet().hashCode();
	}
}
