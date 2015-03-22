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
import org.apache.commons.lang3.ArrayUtils;

import com.chiorichan.packet.Packet;

/**
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class Handler extends SimpleChannelInboundHandler<Object>
{
	private ChannelHandlerContext context;
	private static final Packet PING_PACKET = new Packet( "ping" );
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
			
			// ctx.pipeline().addFirst( new Tester( "POST" ) );
			
			TimerTask task = new TimerTask()
			{
				@Override
				public void run()
				{
					write( PING_PACKET.encode() );
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
			
			Packet[] rcvds = Packet.decode( content );
			
			for ( Packet rcvd : rcvds )
			{
				if ( "ping".equals( rcvd.command() ) )
				{
					write( new Packet( "pong" ).encode() );
				}
				else if ( "~LGIN".equals( rcvd.command() ) )
				{
					// Login Command!
					
					Packet resp = new Packet( new byte[] {0x65}, rcvd.packetId() );
					
					System.out.println( Hex.encodeHexString( resp.encode() ) );
					
					// 689fc00b02060b6c6f67696e506172616d73090a4173736f63417272617906082a64656661756c7402061b64656661756c7441757468656e7469636174696f6e4d6574686f6406054d61726368060c7761726e496e6163746976650806042a656e640a01002a00260000000b030601650300000000006c9fc00b02060d61757468656e74696361746f7206054d61726368
					
					write( resp.encode() );
					
					/*
					 * String hex = Hex.encodeHexString( content );
					 * String id1 = hex.substring( 34, 34 + 22 );
					 * String id2 = hex.substring( 94, 94 + 24 );
					 * 
					 * String response = "010072006e0000000b03060165" + id1 +
					 * "060b6c6f67696e506172616d73090a4173736f63417272617906082a64656661756c7402061b64656661756c7441757468656e7469636174696f6e4d6574686f6406054d61726368060c7761726e496e6163746976650806042a656e640a01002a00260000000b03060165"
					 * + id2 + "0d61757468656e74696361746f7206054d61726368";
					 * 
					 * System.out.println( "SENT: " + response );
					 * System.out.println( "ID1: " + id1 );
					 * System.out.println( "ID2: " + id2 );
					 * 
					 * writeHex( response );
					 */
				}
				else
				{
					System.out.println( "WARNING: The last packet was not understood" );
				}
			}
			
			context.flush();
		}
	}
	
	public void writeHex( String s ) throws DecoderException
	{
		write( ArrayUtils.addAll( Hex.decodeHex( s.replaceAll( " ", "" ).toCharArray() ) ) );
	}
	
	public void write( byte[] msg )
	{
		context.write( Unpooled.copiedBuffer( msg ) );
	}
	
	public void write( String msg )
	{
		context.write( msg );
	}
	
	public void log( String... msgs )
	{
		for ( String msg : msgs )
			System.out.println( msg );
	}
}
