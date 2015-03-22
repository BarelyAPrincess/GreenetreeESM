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

import org.apache.commons.codec.binary.Hex;

import com.chiorichan.packet.Packet;

/**
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class Main
{
	public static EventLoopGroup bossGroup = new NioEventLoopGroup( 1 );
	public static EventLoopGroup workerGroup = new NioEventLoopGroup();
	
	public static void main( String... args )
	{
		ServerBootstrap b = new ServerBootstrap();
		b.group( bossGroup, workerGroup ).channel( NioServerSocketChannel.class ).childHandler( new Initializer() );
		
		try
		{
			final Channel ch = b.bind( new InetSocketAddress( 4443 ) ).sync().channel();
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
