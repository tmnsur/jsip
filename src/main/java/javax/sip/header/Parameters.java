package javax.sip.header;

import java.text.ParseException;
import java.util.Iterator;

/**
 * This interface defines methods for accessing generic parameters for Headers
 * that contain generic parameter values.
 */
public interface Parameters {
	/**
	 * Returns the value of the named parameter, or null if it is not set. A
	 * zero-length String indicates flag parameter.
	 *
	 * @param name name of parameter to retrieve
	 * @return the value of specified parameter
	 */
	public String getParameter(String name);

	/**
	 * Sets the value of the specified parameter. If the parameter already had a
	 * value it will be overwritten. A zero-length String indicates flag parameter.
	 *
	 * @param name  - a String specifying the parameter name
	 * @param value - a String specifying the parameter value
	 * @throws ParseException which signals that an error has been reached
	 *                        unexpectedly while parsing the parameter name or
	 *                        value.
	 */
	public void setParameter(String name, String value) throws ParseException;

	/**
	 * Returns an Iterator over the names (Strings) of all parameters present in
	 * this ParametersHeader.
	 *
	 * @return an Iterator over all the parameter names
	 */
	public Iterator getParameterNames();

	/**
	 * Removes the specified parameter from Parameters of this ParametersHeader.
	 * This method returns silently if the parameter is not part of the
	 * ParametersHeader.
	 *
	 * @param name - a String specifying the parameter name
	 */
	public void removeParameter(String name);
}
