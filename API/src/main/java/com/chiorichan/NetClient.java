/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;

import javax.net.ssl.SSLEngine;

import com.chiorichan.packet.MessageBus;
import com.chiorichan.packet.MessageStream;
import com.chiorichan.packet.Packet;
import com.chiorichan.packet.PacketException;
import com.chiorichan.ssl.SslContextFactory;

/**
 * Used to connect to an ESM Server or DVR Box
 */
public class NetClient
{
	private class NetHandler extends SimpleChannelInboundHandler<Object>
	{
		private ChannelHandlerContext context;
		private SSLEngine engine;
		
		@Override
		public void channelActive( ChannelHandlerContext ctx )
		{
			bus.setStream( new MessageStream( ctx ) );
		}
		
		@Override
		public void channelInactive( ChannelHandlerContext ctx )
		{
			bus.stopPinger();
		}
		
		@Override
		protected void messageReceived( ChannelHandlerContext ctx, Object obj ) throws Exception
		{
			if ( obj instanceof DefaultHttpResponse )
			{
				context = ctx;
				
				System.out.println( "Response Status: " + ( ( DefaultHttpResponse ) obj ).status() );
				
				if ( ssl )
				{
					engine = SslContextFactory.getClientContext().createSSLEngine();
					engine.setUseClientMode( true );
					engine.setEnabledProtocols( new String[] {"TLSv1"} );
					engine.setEnabledCipherSuites( new String[] {"SSL_RSA_WITH_RC4_128_SHA", "SSL_RSA_WITH_RC4_128_MD5"} );
					
					ctx.pipeline().addFirst( "ssl", new SslHandler( engine ) );
				}
				else
					start( ctx );
			}
			else if ( obj instanceof HttpContent )
				try
				{
					System.out.println( "From Server Message Received: " + obj );
					
					HttpContent msg = ( HttpContent ) obj;
					ByteBuf buf = msg.content();
					
					if ( buf.readableBytes() < 1 )
					{
						System.out.println( "WARNING: Received an empty message!" );
						return;
					}
					
					Packet[] rcvds = Packet.decode( buf );
					
					if ( rcvds.length > 0 )
						bus.handle( rcvds );
					else
						bus.incoming( buf );
				}
				catch ( PacketException e )
				{
					System.out.println( e.hexDump() );
					throw e;
				}
			else
				System.out.println( "Received an unknown packet type: " + obj );
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
	
	private class NetInitializer extends ChannelInitializer<SocketChannel>
	{
		@Override
		protected void initChannel( SocketChannel ch ) throws Exception
		{
			ChannelPipeline p = ch.pipeline();
			
			p.addLast( new LoggingHandler( LogLevel.INFO ) );
			
			p.addLast( "codec", new Codec() );
			p.addLast( "handler", new NetHandler() );
		}
	}
	
	private EventLoopGroup group = new NioEventLoopGroup();
	private Thread process;
	
	private final String addr;
	private final int port;
	private final String url;
	private final boolean ssl;
	private final MessageBus bus;
	
	public NetClient( String addr, int port, String url, boolean ssl, MessageBus bus )
	{
		this.addr = addr;
		this.port = port;
		this.url = url;
		this.ssl = ssl;
		this.bus = bus;
	}
	
	public void connect()
	{
		try
		{
			Bootstrap b = new Bootstrap();
			b.group( group ).channel( NioSocketChannel.class ).handler( new NetInitializer() );
			
			final Channel channel = b.connect( addr, port ).sync().channel();
			
			HttpRequest request = new DefaultHttpRequest( HttpVersion.HTTP_1_0, HttpMethod.GET, url );
			
			channel.writeAndFlush( request ).sync();
			
			process = new Thread()
			{
				@Override
				public void run()
				{
					try
					{
						channel.closeFuture().sync();
					}
					catch ( InterruptedException e )
					{
						e.printStackTrace();
					}
					finally
					{
						group.shutdownGracefully();
					}
				}
			};
			
			process.setName( "NetClient Connection" );
			process.start();
		}
		catch ( InterruptedException e )
		{
			e.printStackTrace();
			group.shutdownGracefully();
		}
	}
	
	public void disconnect()
	{
		
	}
	
	public void start( ChannelHandlerContext ctx )
	{
		bus.start0();
		bus.startPinger();
	}
}
