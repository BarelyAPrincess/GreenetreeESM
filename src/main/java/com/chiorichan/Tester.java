/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class Tester extends ChannelHandlerAdapter
{
	String prefix = "";
	
	public Tester( String pre )
	{
		prefix = pre;
	}
	
	@Override
	public void write( ChannelHandlerContext ctx, Object msg, ChannelPromise promise ) throws Exception
	{
		System.out.println( "Wrote (" + prefix + "): " + msg );
		
		ctx.write( msg, promise );
	}
}
