package gov.nist.javax.sip.parser;

import gov.nist.core.InternalErrorHandler;
import gov.nist.javax.sip.header.SIPHeaderNamesCache;
import gov.nist.javax.sip.header.extensions.Join;
import gov.nist.javax.sip.header.extensions.MinSE;
import gov.nist.javax.sip.header.extensions.References;
import gov.nist.javax.sip.header.extensions.ReferredBy;
import gov.nist.javax.sip.header.extensions.Replaces;
import gov.nist.javax.sip.header.extensions.SessionExpires;
import gov.nist.javax.sip.header.ims.PAccessNetworkInfoHeader;
import gov.nist.javax.sip.header.ims.PAssertedIdentityHeader;
import gov.nist.javax.sip.header.ims.PAssertedServiceHeader;
import gov.nist.javax.sip.header.ims.PAssociatedURIHeader;
import gov.nist.javax.sip.header.ims.PCalledPartyIDHeader;
import gov.nist.javax.sip.header.ims.PChargingFunctionAddressesHeader;
import gov.nist.javax.sip.header.ims.PChargingVectorHeader;
import gov.nist.javax.sip.header.ims.PMediaAuthorizationHeader;
import gov.nist.javax.sip.header.ims.PPreferredIdentityHeader;
import gov.nist.javax.sip.header.ims.PPreferredServiceHeader;
import gov.nist.javax.sip.header.ims.PProfileKeyHeader;
import gov.nist.javax.sip.header.ims.PServedUserHeader;
import gov.nist.javax.sip.header.ims.PUserDatabaseHeader;
import gov.nist.javax.sip.header.ims.PVisitedNetworkIDHeader;
import gov.nist.javax.sip.header.ims.PathHeader;
import gov.nist.javax.sip.header.ims.PrivacyHeader;
import gov.nist.javax.sip.header.ims.SecurityClientHeader;
import gov.nist.javax.sip.header.ims.SecurityServerHeader;
import gov.nist.javax.sip.header.ims.SecurityVerifyHeader;
import gov.nist.javax.sip.header.ims.ServiceRouteHeader;
import gov.nist.javax.sip.parser.extensions.JoinParser;
import gov.nist.javax.sip.parser.extensions.MinSEParser;
import gov.nist.javax.sip.parser.extensions.ReferencesParser;
import gov.nist.javax.sip.parser.extensions.ReferredByParser;
import gov.nist.javax.sip.parser.extensions.ReplacesParser;
import gov.nist.javax.sip.parser.extensions.SessionExpiresParser;
import gov.nist.javax.sip.parser.ims.PAccessNetworkInfoParser;
import gov.nist.javax.sip.parser.ims.PAssertedIdentityParser;
import gov.nist.javax.sip.parser.ims.PAssertedServiceParser;
import gov.nist.javax.sip.parser.ims.PAssociatedURIParser;
import gov.nist.javax.sip.parser.ims.PCalledPartyIDParser;
import gov.nist.javax.sip.parser.ims.PChargingFunctionAddressesParser;
import gov.nist.javax.sip.parser.ims.PChargingVectorParser;
import gov.nist.javax.sip.parser.ims.PMediaAuthorizationParser;
import gov.nist.javax.sip.parser.ims.PPreferredIdentityParser;
import gov.nist.javax.sip.parser.ims.PPreferredServiceParser;
import gov.nist.javax.sip.parser.ims.PProfileKeyParser;
import gov.nist.javax.sip.parser.ims.PServedUserParser;
import gov.nist.javax.sip.parser.ims.PUserDatabaseParser;
import gov.nist.javax.sip.parser.ims.PVisitedNetworkIDParser;
import gov.nist.javax.sip.parser.ims.PathParser;
import gov.nist.javax.sip.parser.ims.PrivacyParser;
import gov.nist.javax.sip.parser.ims.SecurityClientParser;
import gov.nist.javax.sip.parser.ims.SecurityServerParser;
import gov.nist.javax.sip.parser.ims.SecurityVerifyParser;
import gov.nist.javax.sip.parser.ims.ServiceRouteParser;

import java.lang.reflect.Constructor;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sip.header.AcceptEncodingHeader;
import javax.sip.header.AcceptHeader;
import javax.sip.header.AcceptLanguageHeader;
import javax.sip.header.AlertInfoHeader;
import javax.sip.header.AllowEventsHeader;
import javax.sip.header.AllowHeader;
import javax.sip.header.AuthenticationInfoHeader;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.CallInfoHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentDispositionHeader;
import javax.sip.header.ContentEncodingHeader;
import javax.sip.header.ContentLanguageHeader;
import javax.sip.header.ContentLengthHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.DateHeader;
import javax.sip.header.ErrorInfoHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.InReplyToHeader;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.MimeVersionHeader;
import javax.sip.header.MinExpiresHeader;
import javax.sip.header.OrganizationHeader;
import javax.sip.header.PriorityHeader;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.ProxyAuthorizationHeader;
import javax.sip.header.ProxyRequireHeader;
import javax.sip.header.RAckHeader;
import javax.sip.header.RSeqHeader;
import javax.sip.header.ReasonHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.ReferToHeader;
import javax.sip.header.ReplyToHeader;
import javax.sip.header.RequireHeader;
import javax.sip.header.RetryAfterHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.SIPETagHeader;
import javax.sip.header.SIPIfMatchHeader;
import javax.sip.header.ServerHeader;
import javax.sip.header.SubjectHeader;
import javax.sip.header.SubscriptionStateHeader;
import javax.sip.header.SupportedHeader;
import javax.sip.header.TimeStampHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.UnsupportedHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.header.WarningHeader;

/**
 * A factory class that does a name lookup on a registered parser and returns a
 * header parser for the given name.
 */
public class ParserFactory {
	// moving to concurrent structures to avoid blocking witnessed during profiling
	private static Map<String, Class<? extends HeaderParser>> parserTable;
	private static Class[] constructorArgs;
	private static ConcurrentHashMap<Class, Constructor> parserConstructorCache;

	static {
		parserTable = new ConcurrentHashMap<>(90);
		parserConstructorCache = new ConcurrentHashMap<>();
		constructorArgs = new Class[1];
		constructorArgs[0] = String.class;

		parserTable.put(ReplyToHeader.NAME.toLowerCase(), ReplyToParser.class);
		parserTable.put(InReplyToHeader.NAME.toLowerCase(), InReplyToParser.class);
		parserTable.put(AcceptEncodingHeader.NAME.toLowerCase(), AcceptEncodingParser.class);
		parserTable.put(AcceptLanguageHeader.NAME.toLowerCase(), AcceptLanguageParser.class);

		parserTable.put("t", ToParser.class);
		parserTable.put(ToHeader.NAME.toLowerCase(), ToParser.class);

		parserTable.put(FromHeader.NAME.toLowerCase(), FromParser.class);
		parserTable.put("f", FromParser.class);

		parserTable.put(CSeqHeader.NAME.toLowerCase(), CSeqParser.class);

		parserTable.put(ViaHeader.NAME.toLowerCase(), ViaParser.class);
		parserTable.put("v", ViaParser.class);

		parserTable.put(ContactHeader.NAME.toLowerCase(), ContactParser.class);
		parserTable.put("m", ContactParser.class);

		parserTable.put(ContentTypeHeader.NAME.toLowerCase(), ContentTypeParser.class);
		parserTable.put("c", ContentTypeParser.class);

		parserTable.put(ContentLengthHeader.NAME.toLowerCase(), ContentLengthParser.class);
		parserTable.put("l", ContentLengthParser.class);

		parserTable.put(AuthorizationHeader.NAME.toLowerCase(), AuthorizationParser.class);

		parserTable.put(WWWAuthenticateHeader.NAME.toLowerCase(), WWWAuthenticateParser.class);

		parserTable.put(CallIdHeader.NAME.toLowerCase(), CallIDParser.class);
		parserTable.put("i", CallIDParser.class);

		parserTable.put(RouteHeader.NAME.toLowerCase(), RouteParser.class);

		parserTable.put(RecordRouteHeader.NAME.toLowerCase(), RecordRouteParser.class);

		parserTable.put(DateHeader.NAME.toLowerCase(), DateParser.class);

		parserTable.put(ProxyAuthorizationHeader.NAME.toLowerCase(), ProxyAuthorizationParser.class);

		parserTable.put(ProxyAuthenticateHeader.NAME.toLowerCase(), ProxyAuthenticateParser.class);

		parserTable.put(RetryAfterHeader.NAME.toLowerCase(), RetryAfterParser.class);

		parserTable.put(RequireHeader.NAME.toLowerCase(), RequireParser.class);

		parserTable.put(ProxyRequireHeader.NAME.toLowerCase(), ProxyRequireParser.class);

		parserTable.put(TimeStampHeader.NAME.toLowerCase(), TimeStampParser.class);

		parserTable.put(UnsupportedHeader.NAME.toLowerCase(), UnsupportedParser.class);

		parserTable.put(UserAgentHeader.NAME.toLowerCase(), UserAgentParser.class);

		parserTable.put(SupportedHeader.NAME.toLowerCase(), SupportedParser.class);
		// bug fix by Steve Crosley
		parserTable.put("k", SupportedParser.class);

		parserTable.put(ServerHeader.NAME.toLowerCase(), ServerParser.class);

		parserTable.put(SubjectHeader.NAME.toLowerCase(), SubjectParser.class);
		parserTable.put("s", SubjectParser.class); // JvB: added

		parserTable.put(SubscriptionStateHeader.NAME.toLowerCase(), SubscriptionStateParser.class);

		parserTable.put(MaxForwardsHeader.NAME.toLowerCase(), MaxForwardsParser.class);

		parserTable.put(MimeVersionHeader.NAME.toLowerCase(), MimeVersionParser.class);

		parserTable.put(MinExpiresHeader.NAME.toLowerCase(), MinExpiresParser.class);

		parserTable.put(OrganizationHeader.NAME.toLowerCase(), OrganizationParser.class);

		parserTable.put(PriorityHeader.NAME.toLowerCase(), PriorityParser.class);

		parserTable.put(RAckHeader.NAME.toLowerCase(), RAckParser.class);

		parserTable.put(RSeqHeader.NAME.toLowerCase(), RSeqParser.class);

		parserTable.put(ReasonHeader.NAME.toLowerCase(), ReasonParser.class);

		parserTable.put(WarningHeader.NAME.toLowerCase(), WarningParser.class);

		parserTable.put(ExpiresHeader.NAME.toLowerCase(), ExpiresParser.class);

		parserTable.put(EventHeader.NAME.toLowerCase(), EventParser.class);
		parserTable.put("o", EventParser.class);

		parserTable.put(ErrorInfoHeader.NAME.toLowerCase(), ErrorInfoParser.class);

		parserTable.put(ContentLanguageHeader.NAME.toLowerCase(), ContentLanguageParser.class);

		parserTable.put(ContentEncodingHeader.NAME.toLowerCase(), ContentEncodingParser.class);
		parserTable.put("e", ContentEncodingParser.class);

		parserTable.put(ContentDispositionHeader.NAME.toLowerCase(), ContentDispositionParser.class);

		parserTable.put(CallInfoHeader.NAME.toLowerCase(), CallInfoParser.class);

		parserTable.put(AuthenticationInfoHeader.NAME.toLowerCase(), AuthenticationInfoParser.class);

		parserTable.put(AllowHeader.NAME.toLowerCase(), AllowParser.class);

		parserTable.put(AllowEventsHeader.NAME.toLowerCase(), AllowEventsParser.class);
		parserTable.put("u", AllowEventsParser.class);

		parserTable.put(AlertInfoHeader.NAME.toLowerCase(), AlertInfoParser.class);

		parserTable.put(AcceptHeader.NAME.toLowerCase(), AcceptParser.class);

		parserTable.put(ReferToHeader.NAME.toLowerCase(), ReferToParser.class);
		// Was missing (bug noticed by Steve Crossley)
		parserTable.put("r", ReferToParser.class);

		// JvB: added to support RFC3903 PUBLISH
		parserTable.put(SIPETagHeader.NAME.toLowerCase(), SIPETagParser.class);
		parserTable.put(SIPIfMatchHeader.NAME.toLowerCase(), SIPIfMatchParser.class);

		// IMS headers
		parserTable.put(PAccessNetworkInfoHeader.NAME.toLowerCase(), PAccessNetworkInfoParser.class);
		parserTable.put(PAssertedIdentityHeader.NAME.toLowerCase(), PAssertedIdentityParser.class);
		parserTable.put(PPreferredIdentityHeader.NAME.toLowerCase(), PPreferredIdentityParser.class);
		parserTable.put(PChargingVectorHeader.NAME.toLowerCase(), PChargingVectorParser.class);
		parserTable.put(PChargingFunctionAddressesHeader.NAME.toLowerCase(), PChargingFunctionAddressesParser.class);
		parserTable.put(PMediaAuthorizationHeader.NAME.toLowerCase(), PMediaAuthorizationParser.class);
		parserTable.put(PathHeader.NAME.toLowerCase(), PathParser.class);
		parserTable.put(PrivacyHeader.NAME.toLowerCase(), PrivacyParser.class);
		parserTable.put(ServiceRouteHeader.NAME.toLowerCase(), ServiceRouteParser.class);
		parserTable.put(PVisitedNetworkIDHeader.NAME.toLowerCase(), PVisitedNetworkIDParser.class);

		// added for more P-header extensions for IMS :
		parserTable.put(PServedUserHeader.NAME.toLowerCase(), PServedUserParser.class);
		parserTable.put(PPreferredServiceHeader.NAME.toLowerCase(), PPreferredServiceParser.class);
		parserTable.put(PAssertedServiceHeader.NAME.toLowerCase(), PAssertedServiceParser.class);
		parserTable.put(PProfileKeyHeader.NAME.toLowerCase(), PProfileKeyParser.class);
		parserTable.put(PUserDatabaseHeader.NAME.toLowerCase(), PUserDatabaseParser.class);

		parserTable.put(PAssociatedURIHeader.NAME.toLowerCase(), PAssociatedURIParser.class);
		parserTable.put(PCalledPartyIDHeader.NAME.toLowerCase(), PCalledPartyIDParser.class);

		parserTable.put(SecurityServerHeader.NAME.toLowerCase(), SecurityServerParser.class);
		parserTable.put(SecurityClientHeader.NAME.toLowerCase(), SecurityClientParser.class);
		parserTable.put(SecurityVerifyHeader.NAME.toLowerCase(), SecurityVerifyParser.class);

		// Per RFC 3892 (pmusgrave)
		parserTable.put(ReferredBy.NAME.toLowerCase(), ReferredByParser.class);
		parserTable.put("b", ReferToParser.class);

		// Per RFC4028 Session Timers (pmusgrave)
		parserTable.put(SessionExpires.NAME.toLowerCase(), SessionExpiresParser.class);
		parserTable.put("x", SessionExpiresParser.class);
		parserTable.put(MinSE.NAME.toLowerCase(), MinSEParser.class);
		// (RFC4028 does not give a short form header for MinSE)

		// Per RFC3891 (pmusgrave)
		parserTable.put(Replaces.NAME.toLowerCase(), ReplacesParser.class);

		// Per RFC3911 (jean deruelle)
		parserTable.put(Join.NAME.toLowerCase(), JoinParser.class);

		// http://tools.ietf.org/html/draft-worley-references-05
		parserTable.put(References.NAME.toLowerCase(), ReferencesParser.class);

	}

	/**
	 * This method is added to support parser extensibility.
	 */
	public static void addToParserTable(String headerName, Class<? extends HeaderParser> parserClass) {
		parserTable.put(headerName.toLowerCase(), parserClass);
	}

	/**
	 * create a parser for a header. This is the parser factory.
	 */
	public static HeaderParser createParser(String line) throws ParseException {
		String headerName = Lexer.getHeaderName(line);
		String headerValue = Lexer.getHeaderValue(line);
		if (headerName == null || headerValue == null)
			throw new ParseException("The header name or value is null", 0);

		Class parserClass = (Class) parserTable.get(SIPHeaderNamesCache.toLowerCase(headerName));
		if (parserClass != null) {
			try {
				Constructor cons = (Constructor) parserConstructorCache.get(parserClass);

				if(cons == null) {
					cons = parserClass.getConstructor(constructorArgs);
					parserConstructorCache.putIfAbsent(parserClass, cons);
				}

				Object[] args = new Object[1];

				args[0] = line;

				return (HeaderParser) cons.newInstance(args);
			} catch (Exception ex) {
				InternalErrorHandler.handleException(ex);

				return null;
			}
		} else {
			// Just generate a generic SIPHeader. We define parsers only for the above.
			return new HeaderParser(line);
		}
	}
}
