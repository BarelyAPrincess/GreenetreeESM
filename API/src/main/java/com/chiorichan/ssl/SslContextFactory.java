/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.ssl;

import javax.net.ssl.SSLContext;

/**
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class SslContextFactory
{
	private static final String PROTOCOL = "TLS";
	private static final SSLContext CLIENT_CONTEXT;
	
	static
	{
		SSLContext clientContext = null;
		
		try
		{
			clientContext = SSLContext.getInstance( PROTOCOL );
			clientContext.init( null, TrustManagerFactory.getTrustManagers(), null );
		}
		catch ( Exception e )
		{
			throw new Error( "Failed to initialize the client-side SSLContext", e );
		}
		
		CLIENT_CONTEXT = clientContext;
	}
	
	
	public static SSLContext getClientContext()
	{
		return CLIENT_CONTEXT;
	}
	
	private SslContextFactory()
	{
		// Unused
	}
}
