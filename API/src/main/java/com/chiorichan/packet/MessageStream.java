/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Wraps the Netty Context
 */
public class MessageStream
{
	private ChannelHandlerContext context;
	
	public MessageStream( ChannelHandlerContext context )
	{
		this.context = context;
	}
	
	public void flush()
	{
		context.flush();
	}
	
	public void log( String... msgs )
	{
		for ( String msg : msgs )
			System.out.println( msg );
	}
	
	public void write( byte[] msg )
	{
		context.write( Unpooled.copiedBuffer( msg ) );
	}
	
	public void write( ByteBuf msg )
	{
		context.write( msg );
	}
	
	public void write( String msg )
	{
		context.write( msg );
	}
	
	public void writeHex( String s ) throws DecoderException
	{
		write( ArrayUtils.addAll( Hex.decodeHex( s.replaceAll( " ", "" ).toCharArray() ) ) );
	}
}
