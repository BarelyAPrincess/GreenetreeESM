/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;

/**
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class SslContextFactory
{
	// private static final String PROTOCOL = "TLS";
	private static final SSLContext SERVER_CONTEXT;
	
	static
	{
		SSLContext serverContext = null;
		
		String algorithm = Security.getProperty( "ssl.KeyManagerFactory.algorithm" );
		if ( algorithm == null )
			algorithm = "SunX509";
		
		try
		{
			String pkcs12Base64 = "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";
			
			serverContext = SSLContext.getInstance( "TLS" );
			KeyStore ks = KeyStore.getInstance( "PKCS12" );
			ks.load( new ByteArrayInputStream( Base64.decodeBase64( pkcs12Base64 ) ), "abc123".toCharArray() );
			KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
			kmf.init( ks, "abc123".toCharArray() );
			serverContext.init( kmf.getKeyManagers(), null, null );
		}
		catch ( Exception e )
		{
			throw new Error( "Failed to initialize the server-side SSLContext", e );
		}
		
		SERVER_CONTEXT = serverContext;
	}
	
	
	public static SSLContext getServerContext()
	{
		return SERVER_CONTEXT;
	}
	
	private SslContextFactory()
	{
		// Unused
	}
}
