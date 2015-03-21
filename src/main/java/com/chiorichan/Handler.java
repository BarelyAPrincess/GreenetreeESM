/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.SSLEngine;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class Handler extends SimpleChannelInboundHandler<Object>
{
	private ChannelHandlerContext context;
	private Timer timer = new Timer( "Heartbeat", true );
	
	@Override
	protected void messageReceived( ChannelHandlerContext ctx, Object obj ) throws Exception
	{
		if ( obj instanceof DefaultHttpRequest )
		{
			FullHttpResponse response = new DefaultFullHttpResponse( HttpVersion.HTTP_1_0, HttpResponseStatus.valueOf( 200 ), Unpooled.copiedBuffer( new byte[0] ) );
			context = ctx;
			
			response.headers().add( "Content-Type", "application/octet-stream" );
			
			ctx.write( response );
			ctx.write( "\r\n\r\n" ); // 0d 0a
			ctx.flush();
			
			SSLEngine engine = SslContextFactory.getServerContext().createSSLEngine();
			engine.setUseClientMode( false );
			engine.setEnabledProtocols( new String[] {"TLSv1"} );
			engine.setEnabledCipherSuites( new String[] {"SSL_RSA_WITH_RC4_128_SHA", "SSL_RSA_WITH_RC4_128_MD5"} );
			
			ctx.pipeline().addFirst( "ssl", new TestSslHandler( engine ) );
			
			TimerTask task = new TimerTask()
			{
				@Override
				public void run()
				{
					try
					{
						writeHex( "01 00 0c 00 08 00 00 00 0b 01 06 04 70 69 6e 67" ); // ping!
					}
					catch ( DecoderException e )
					{
						e.printStackTrace();
					}
				}
			};
			timer.scheduleAtFixedRate( task, 23000l, 30000l );
		}
		else if ( obj instanceof HttpContent )
		{
			HttpContent msg = ( HttpContent ) obj;
			ByteBuf buf = msg.content();
			
			byte[] content = new byte[buf.readableBytes()];
			
			int i = 0;
			while ( buf.readableBytes() > 0 )
			{
				byte b = buf.readByte();
				content[i] = b;
				i++;
			}
			
			String hex = Hex.encodeHexString( content ).toLowerCase();
			
			System.out.println( "RCVD: " + hex );
			
			// 01 00 0c 00 08 00 00 00 0b 01 06 04 70 69 6e 67 -- Heartbeat?
			if ( hex.equals( "01000c00080000000b01060470696e67" ) ) // Ping Heartbeat
			{
				// 01 00 0c 00 08 00 00 00 0b 01 06 04 70 6f 6e 67 -- Pong!
				writeHex( "01 00 0c 00 08 00 00 00 0b 01 06 04 70 6f 6e 67" );
			}
			else if ( hex.startsWith( "01001a00160000000b0306057e4c47494e030000000000" ) )
			{
				// 01 00 72 00 6e 00 00 00 0b 03 06 01 65 03 00 00 00 00 00 98 9a c0 0b 02 06 0b 6c 6f 67 69 6e 50 61 72 61 6d 73 09 0a 41 73 73 6f 63 41 72 72 61 79 06 08 2a 64 65 66 61 75 6c 74 02 06 1b 64 65 66 61 75 6c 74 41 75 74
				// 68 65 6e 74 69 63 61 74 69 6f 6e 4d 65 74 68 6f 64 06 05 4d 61 72 63 68 06 0c 77 61 72 6e 49 6e 61 63 74 69 76 65 08 06 04 2a 65 6e 64 0a 01 00 2a 00 26 00 00 00 0b 03 06 01 65 03 00 00 00 00 00 9c 9a c0 0b 02 06 0d
				// 61 75 74 68 65 6e 74 69 63 61 74 6f 72 06 05 4d 61 72 63 68
				writeHex( "01 00 72 00 6e 00 00 00 0b 03 06 01 65 03 00 00 00 00 00 98 9a c0 0b 02 06 0b 6c 6f 67 69 6e 50 61 72 61 6d 73 09 0a 41 73 73 6f 63 41 72 72 61 79 06 08 2a 64 65 66 61 75 6c 74 02 06 1b 64 65 66 61 75 6c 74 41 75 74 68 65 6e 74 69 63 61 74 69 6f 6e 4d 65 74 68 6f 64 06 05 4d 61 72 63 68 06 0c 77 61 72 6e 49 6e 61 63 74 69 76 65 08 06 04 2a 65 6e 64 0a 01 00 2a 00 26 00 00 00 0b 03 06 01 65 03 00 00 00 00 00 9c 9a c0 0b 02 06 0d 61 75 74 68 65 6e 74 69 63 61 74 6f 72 06 05 4d 61 72 63 68" );
			}
			
			// TYPE? COMMAND! REPEAT DELIM USER END
			// 01001a00160000000b030605 7e4c47494e030000000000 989ac00b0 20202010020001c0000000b030605 7e4c47494e030000000000 9c9ac00b 0206 0561646d696e 02
			// 01001a00160000000b030605 7e4c47494e030000000000 449cc00b0 20202010020001c0000000b030605 7e4c47494e030000000000 489cc00b 0206 0561646d696e 02
			// 01001a00160000000b030605 7e4c47494e030000000000 4c9cc00b0 20202010020001c0000000b030605 7e4c47494e030000000000 509cc00b 0206 0561646d696e 02
			
			// 01000c00080000000b010604 70696e67
		}
	}
	
	public void writeHex( String s ) throws DecoderException
	{
		write( Hex.decodeHex( s.replaceAll( " ", "" ).toCharArray() ) );
	}
	
	public void write( byte[] msg )
	{
		context.writeAndFlush( context.alloc().buffer().writeBytes( msg ) );
		// context.flush();
	}
	
	public void write( String msg )
	{
		context.write( msg );
		context.flush();
	}
	
	public void log( String... msgs )
	{
		for ( String msg : msgs )
			System.out.println( msg );
	}
}
