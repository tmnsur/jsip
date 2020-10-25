package gov.nist.javax.sdp.parser;

import java.text.ParseException;

import gov.nist.core.ParserCore;
import gov.nist.javax.sdp.fields.SDPField;

public abstract class SDPParser extends ParserCore {
	public abstract SDPField parse() throws ParseException;
}
