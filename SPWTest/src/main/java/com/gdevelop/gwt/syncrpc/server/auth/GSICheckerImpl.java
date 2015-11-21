package com.gdevelop.gwt.syncrpc.server.auth;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.gdevelop.gwt.syncrpc.server.auth.gae.CrossClientAuthRSS;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

public class GSICheckerImpl implements GoogleOAuth2Checker {
	private final List<String> mClientIDs;
	private final String mAudience;
	private GoogleIdTokenVerifier mVerifier;
	private JsonFactory mJFactory;
	private String mProblem = "Verification failed. (Time-out?)";
	Logger logger = Logger.getLogger(GSICheckerImpl.class.getName());

	public GSICheckerImpl(ServletContext context) {
		ClientIdManager manager = new ClientIdManagerImpl(context);
		mClientIDs = Arrays.asList(manager.getAllClients());
		mAudience = manager.getServerAudience();
		init();
	}

	protected void init() {
		NetHttpTransport transport = new NetHttpTransport();
		mJFactory = new GsonFactory();
		mVerifier = new GoogleIdTokenVerifier.Builder(transport, mJFactory).setAudience(mClientIDs)
				.setIssuer("https://accounts.google.com").build();

	}

	@Override
	public String problem() {
		return mProblem;
	}

	@Override
	public GoogleIdToken.Payload check(String tokenString) {
		GoogleIdToken.Payload payload = null;
		GoogleIdToken idToken = null;
		try {
			idToken = mVerifier.verify(tokenString);
		} catch (GeneralSecurityException e) {
			mProblem = "Security issue: " + e.getLocalizedMessage();
		} catch (IOException e) {
			mProblem = "Network problem: " + e.getLocalizedMessage();
		}
		if (idToken != null) {
			payload = idToken.getPayload();
			logger.info("User ID: " + payload.getSubject());
		} else {
			logger.info("Invalid ID token.");
		}
		return payload;
	}
}