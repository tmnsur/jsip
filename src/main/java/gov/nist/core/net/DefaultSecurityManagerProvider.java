package gov.nist.core.net;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implement the default TLS security policy by loading keys specified in stack properties or system -D settings.
 */
public class DefaultSecurityManagerProvider implements SecurityManagerProvider {
	private static final Logger logger = Logger.getLogger(DefaultSecurityManagerProvider.class.getName());

	private KeyManagerFactory keyManagerFactory;
	private TrustManagerFactory trustManagerFactory;

	public void init(Properties properties) throws GeneralSecurityException, IOException {
		// required, could use default keyStore, but it is better practice to explicitly specify
		final String keyStoreFilename = properties.getProperty("javax.net.ssl.keyStore");
		// required
		final String keyStorePassword = properties.getProperty("javax.net.ssl.keyStorePassword");
		// optional, uses default if not specified 
		String keyStoreType = properties.getProperty("javax.net.ssl.keyStoreType");
		if(keyStoreType == null) {
			keyStoreType = KeyStore.getDefaultType();

			logger.log(Level.WARNING, "Using default keystore type {0}", keyStoreType);
		}

		if(keyStoreFilename == null || keyStorePassword == null) {
			logger.log(Level.WARNING, "TLS server settings will be inactive - TLS key store will use JVM defaults"
					+ " keyStoreType={0} javax.net.ssl.keyStore={1} javax.net.ssl.keyStorePassword={2}",
							new Object[] {keyStoreType, keyStoreFilename, (keyStorePassword == null? null: "***")});
		}

		// required, could use default trustStore, but it is better practice to explicitly specify
		final String trustStoreFilename = properties.getProperty("javax.net.ssl.trustStore");
		// optional, if not specified using keyStorePassword
		String trustStorePassword = properties.getProperty("javax.net.ssl.trustStorePassword");
		if(trustStorePassword == null) {
			logger.log(Level.INFO, "javax.net.ssl.trustStorePassword is null, using the password passed through"
					+ " javax.net.ssl.keyStorePassword");

			trustStorePassword = keyStorePassword;
		}

		// optional, uses default if not specified 
		String trustStoreType = properties.getProperty("javax.net.ssl.trustStoreType");
		if(null == trustStoreType) {
			trustStoreType = KeyStore.getDefaultType();

			logger.log(Level.WARNING, "Using default truststore type {0}", trustStoreType);
		}

		if(null == trustStoreFilename || null == trustStorePassword) {
			logger.log(Level.WARNING, "TLS trust settings will be inactive - TLS trust store will use JVM defaults."
					+ " trustStoreType={0} javax.net.ssl.trustStore={1} javax.net.ssl.trustStorePassword={2}",
							new Object[] {trustStoreType, trustStoreFilename,
									(trustStorePassword == null ? null: "***")});
		}

		String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
		if(null == algorithm) {
			algorithm = "SunX509";
		}

		if(logger.isLoggable(Level.FINEST)) {
			logger.log(Level.FINEST, "SecurityManagerProvider {0} will use algorithm {1}",
					new Object[] {this.getClass().getCanonicalName(), algorithm});
		}

		keyManagerFactory = KeyManagerFactory.getInstance(algorithm);

		if(null == keyStoreFilename) {
			keyManagerFactory.init(null, null);
		} else {
			final KeyStore ks = KeyStore.getInstance(keyStoreType);

			if(keyStorePassword != null) {
				ks.load(new FileInputStream(new File(keyStoreFilename)), keyStorePassword.toCharArray());
			} else {
				ks.load(new FileInputStream(new File(keyStoreFilename)), null);
			}

			keyManagerFactory.init(ks, keyStorePassword.toCharArray());
		}

		trustManagerFactory = TrustManagerFactory.getInstance(algorithm);

		if(null == trustStoreFilename) {
			trustManagerFactory.init((KeyStore)null);
		} else {
			final KeyStore ts = KeyStore.getInstance(trustStoreType);

			if(trustStorePassword != null) {
				ts.load(new FileInputStream(new File(trustStoreFilename)), trustStorePassword.toCharArray());
			} else {
				ts.load(new FileInputStream(new File(trustStoreFilename)), null);
			}

			trustManagerFactory.init(ts);
		}

		if(logger.isLoggable(Level.FINEST)) {
			logger.log(Level.FINEST, "TLS settings OK. SecurityManagerProvider {0} initialized.",
					this.getClass().getCanonicalName());
		}
	}

	public KeyManager[] getKeyManagers(boolean client) {
		if(null == keyManagerFactory) {
			return null;
		}

		return keyManagerFactory.getKeyManagers();
	}

	public TrustManager[] getTrustManagers(boolean client) {
		if(null == trustManagerFactory) {
			return null;
		}

		return trustManagerFactory.getTrustManagers();
	}
}
