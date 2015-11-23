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
import io.netty.handler.ssl.SslHandshakeCompletionEvent;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Timer;

import javax.net.ssl.SSLEngine;

import com.chiorichan.packet.Packet;
import com.chiorichan.packet.PacketException;
import com.google.common.base.Charsets;

/**
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class Handler extends SimpleChannelInboundHandler<Object>
{
	private Timer timer = new Timer( "Heartbeat", true );
	private boolean ssl;
	private ServerMessageBus bus;
	private String url;
	
	public Handler( boolean ssl )
	{
		this.ssl = ssl;
	}
	
	@Override
	public void channelInactive( ChannelHandlerContext ctx )
	{
		timer.cancel();
		if ( bus != null && bus.client() != null )
			bus.client().disconnect();
	}
	
	public String getUri( String uri )
	{
		try
		{
			uri = URLDecoder.decode( uri, Charsets.UTF_8.name() );
		}
		catch ( UnsupportedEncodingException e )
		{
			try
			{
				uri = URLDecoder.decode( uri, Charsets.ISO_8859_1.name() );
			}
			catch ( UnsupportedEncodingException e1 )
			{
				e1.printStackTrace();
			}
		}
		catch ( IllegalArgumentException e1 )
		{
			
		}
		
		if ( uri.contains( "?" ) )
			uri = uri.substring( 0, uri.indexOf( "?" ) );
		
		if ( !uri.startsWith( "/" ) )
			uri = "/" + uri;
		
		return uri;
	}
	
	@Override
	protected void messageReceived( ChannelHandlerContext ctx, Object obj ) throws Exception
	{
		if ( obj instanceof DefaultHttpRequest )
		{
			DefaultHttpRequest request = ( ( DefaultHttpRequest ) obj );
			url = getUri( request.uri() );
			
			FullHttpResponse response = new DefaultFullHttpResponse( HttpVersion.HTTP_1_0, HttpResponseStatus.valueOf( 200 ), Unpooled.copiedBuffer( new byte[0] ) );
			
			response.headers().add( "Content-Type", "application/octet-stream" );
			
			ctx.write( response );
			ctx.write( "\r\n\r\n" ); // 0d 0a
			ctx.flush();
			
			if ( ssl )
			{
				SSLEngine engine = SslContextFactory.getServerContext().createSSLEngine();
				engine.setUseClientMode( false );
				engine.setEnabledProtocols( new String[] {"TLSv1"} );
				engine.setEnabledCipherSuites( new String[] {"SSL_RSA_WITH_RC4_128_SHA", "SSL_RSA_WITH_RC4_128_MD5"} );
				
				ctx.pipeline().addFirst( "ssl", new MarchSslHandler( engine ) );
			}
			else
				start( ctx );
		}
		else if ( obj instanceof HttpContent )
			try
			{
				System.out.println( "From Client Message Received: " + obj );
				
				HttpContent msg = ( HttpContent ) obj;
				ByteBuf buf = msg.content();
				
				if ( buf.readableBytes() < 1 )
				{
					System.out.println( "WARNING: Received an empty message!" );
					return;
				}
				
				int mark = buf.readerIndex();
				Packet[] rcvds = Packet.decode( buf );
				
				if ( rcvds.length > 0 )
					bus.handle( rcvds );
				else
					bus.incoming( buf.copy( mark, buf.readerIndex() - mark ) );
			}
			catch ( PacketException e )
			{
				System.out.println( e.hexDump() );
				throw e;
			}
		else
			System.out.println( "Received an unknown packet type: " + obj );
	}
	
	public void start( ChannelHandlerContext ctx )
	{
		bus = new ServerMessageBus( ssl, url, ctx );
		
		bus.start0();
		bus.startPinger();
	}
	
	@Override
	public void userEventTriggered( ChannelHandlerContext ctx, Object evt )
	{
		if ( evt instanceof SslHandshakeCompletionEvent )
		{
			SslHandshakeCompletionEvent event = ( SslHandshakeCompletionEvent ) evt;
			if ( event.isSuccess() )
				start( ctx );
			else
			{
				System.err.println( "SSL Handshake Failed!" );
				ctx.close();
			}
		}
	}
}
