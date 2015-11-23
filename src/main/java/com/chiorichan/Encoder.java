/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseEncoder;

import java.util.List;

/**
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class Encoder extends HttpResponseEncoder
{
	@Override
	protected void encode( ChannelHandlerContext ctx, Object msg, List<Object> out ) throws Exception
	{
		// There seems to be a problem with this ResponseEncoder putting an empty ByteBuf. *sigh*
		if ( msg instanceof ByteBuf )
			out.add( ( ( ByteBuf ) msg ).retain( 1 ) );
		else
			super.encode( ctx, msg, out );
	}
}
