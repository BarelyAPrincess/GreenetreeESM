/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.packet;

import java.util.Map;
import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class PacketPayloadChild extends PacketPayload
{
	public PacketPayloadChild( Map<String, Object> payload )
	{
		super( payload );
		type = PayloadType.ASSOC_ARRAY;
	}
	
	public PacketPayloadChild( PayloadType type )
	{
		this.type = type;
	}
	
	@Override
	protected ByteBuf encode()
	{
		ByteBuf buf = Unpooled.buffer();
		
		String s = "";
		if ( type == PayloadType.ASSOC_ARRAY )
			s = "AssocArray";
		
		buf.writeByte( 0x09 );
		buf.writeByte( s.length() );
		buf.writeBytes( s.getBytes() );
		
		buf.writeBytes( new PayloadValue( "*default" ).encode() );
		
		if ( getValue( "*default" ) == null )
			buf.writeByte( 0x02 );
		else
			buf.writeBytes( getValue( "*default" ).encode() );
		
		for ( Entry<String, PayloadValue> e : payload.entrySet() )
			if ( !e.getKey().startsWith( "*" ) )
			{
				buf.writeBytes( new PayloadValue( e.getKey() ).encode() );
				buf.writeBytes( e.getValue().encode() );
			}
		
		buf.writeByte( 0x06 );
		buf.writeByte( 0x04 );
		buf.writeBytes( "*end".getBytes() );
		buf.writeByte( 0x0a );
		
		return buf;
	}
}
