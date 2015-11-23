/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

public class Main
{
	public static EventLoopGroup bossGroup = new NioEventLoopGroup( 1 );
	public static EventLoopGroup workerGroup = new NioEventLoopGroup();
	
	public static void main( String... args )
	{
		new Thread()
		{
			@Override
			public void run()
			{
				// Secure 4443
				ServerBootstrap secure = new ServerBootstrap();
				secure.group( bossGroup, workerGroup ).channel( NioServerSocketChannel.class ).childHandler( new Initializer( true ) );
				
				try
				{
					final Channel ch = secure.bind( new InetSocketAddress( 4443 ) ).sync().channel();
					ch.closeFuture().sync();
				}
				catch ( InterruptedException e )
				{
					e.printStackTrace();
				}
				finally
				{
					bossGroup.shutdownGracefully();
					workerGroup.shutdownGracefully();
				}
			}
		}.start();
		
		/*
		new Thread()
		{
			@Override
			public void run()
			{
				// Unsecure 8080
				ServerBootstrap unsecure = new ServerBootstrap();
				unsecure.group( bossGroup, workerGroup ).channel( NioServerSocketChannel.class ).childHandler( new Initializer( false ) );
				
				try
				{
					final Channel ch = unsecure.bind( new InetSocketAddress( 8080 ) ).sync().channel();
					ch.closeFuture().sync();
				}
				catch ( InterruptedException e )
				{
					e.printStackTrace();
				}
				finally
				{
					bossGroup.shutdownGracefully();
					workerGroup.shutdownGracefully();
				}
			}
		}.start();
		*/
		
		// DVR 2804
		ServerBootstrap dvr = new ServerBootstrap();
		dvr.group( bossGroup, workerGroup ).channel( NioServerSocketChannel.class ).childHandler( new Initializer( true ) );
		
		try
		{
			final Channel ch = dvr.bind( new InetSocketAddress( 2804 ) ).sync().channel();
			ch.closeFuture().sync();
		}
		catch ( InterruptedException e )
		{
			e.printStackTrace();
		}
		finally
		{
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}
