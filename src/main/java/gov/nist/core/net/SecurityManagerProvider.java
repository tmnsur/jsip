package gov.nist.core.net;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

/**
 * Override this interface to provide custom keys and trust stores for TLS.
 */
public interface SecurityManagerProvider {
	void init(Properties properties) throws GeneralSecurityException, IOException;
	KeyManager[] getKeyManagers(boolean client);
	TrustManager[] getTrustManagers(boolean client);
}
