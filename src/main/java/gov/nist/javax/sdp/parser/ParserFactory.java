package gov.nist.javax.sdp.parser;

import java.lang.reflect.Constructor;
import java.text.ParseException;
import java.util.Hashtable;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.PackageNames;

/**
 * Factory for creating parsers for the SDP stuff.
 */
public class ParserFactory {
	private static final String PACKAGE_NAME = PackageNames.SDP_PACKAGE + ".parser";

	private static Hashtable<String, Class<?>> parserTable;
	private static Class<?>[] constructorArgs;

	private static Class<?> getParser(String parserClass) {
		try {
			return Class.forName(PACKAGE_NAME + "." + parserClass);
		} catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Could not find class", ex);
		}
	}

	static {
		constructorArgs = new Class[1];
		constructorArgs[0] = String.class;
		parserTable = new Hashtable<>();
		parserTable.put("a", getParser("AttributeFieldParser"));
		parserTable.put("b", getParser("BandwidthFieldParser"));
		parserTable.put("c", getParser("ConnectionFieldParser"));
		parserTable.put("e", getParser("EmailFieldParser"));
		parserTable.put("i", getParser("InformationFieldParser"));
		parserTable.put("k", getParser("KeyFieldParser"));
		parserTable.put("m", getParser("MediaFieldParser"));
		parserTable.put("o", getParser("OriginFieldParser"));
		parserTable.put("p", getParser("PhoneFieldParser"));
		parserTable.put("v", getParser("ProtoVersionFieldParser"));
		parserTable.put("r", getParser("RepeatFieldParser"));
		parserTable.put("s", getParser("SessionNameFieldParser"));
		parserTable.put("t", getParser("TimeFieldParser"));
		parserTable.put("u", getParser("URIFieldParser"));
		parserTable.put("z", getParser("ZoneFieldParser"));
	}

	public static SDPParser createParser(String field) throws ParseException {
		String fieldName = Lexer.getFieldName(field);

		if(fieldName == null) {
			return null;
		}

		Class<?> parserClass = parserTable.get(fieldName.toLowerCase());

		if(parserClass != null) {
			try {
				Constructor<?> cons = parserClass.getConstructor(constructorArgs);
				Object[] args = new Object[1];

				args[0] = field;

				SDPParser retval = (SDPParser) cons.newInstance(args);

				return retval;
			} catch(Exception ex) {
				InternalErrorHandler.handleException(ex);

				return null;
			}
		}

		throw new ParseException("Could not find parser for " + fieldName, 0);
	}
}
