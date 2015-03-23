/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

/**
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class PacketPayload extends PayloadValue
{
	public enum PayloadType
	{
		ROOT(), ASSOC_ARRAY();
	}
	
	Map<String, PayloadValue> payload = Maps.newLinkedHashMap();
	PayloadType type = PayloadType.ROOT;
	int keyCounter = 0;
	String previousKey = null;
	
	protected PacketPayload()
	{
		
	}
	
	public PacketPayload( Map<String, PayloadValue> payload )
	{
		this.payload = payload;
	}
	
	public boolean isAssocArray()
	{
		return type == PayloadType.ASSOC_ARRAY;
	}
	
	private void put( String key, PayloadValue val )
	{
		previousKey = key;
		if ( val == null )
			val = new PayloadValue();
		payload.put( key, val );
	}
	
	public PacketPayload putSubload( String key )
	{
		return putSubload( key, PayloadType.ASSOC_ARRAY );
	}
	
	public PacketPayload putSubload( PayloadType type )
	{
		PacketPayload subload = new PacketPayloadChild( type );
		putValue( subload );
		return subload;
	}
	
	public PacketPayload putSubload( String key, PayloadType type )
	{
		PacketPayload subload = new PacketPayloadChild( type );
		put( key, subload );
		return subload;
	}
	
	public void putKey( String data )
	{
		put( data, null );
	}
	
	private void putValue0( PayloadValue data )
	{
		put( "*" + Integer.toString( keyCounter ), data );
		keyCounter++;
	}
	
	public void putValue( PayloadValue data )
	{
		if ( previousKey == null || !payload.containsKey( previousKey ) )
			putValue0( data );
		else
		{
			if ( isAssocArray() ) // Always Key -> Value
			{
				PayloadValue val = getValue( previousKey );
				drop( previousKey );
				String newKey = val.getString();
				if ( newKey == null )
					putValue0( data );
				else
					put( newKey, data );
			}
			else
			{
				putValue0( data );
			}
			previousKey = null;
		}
	}
	
	public void drop( String key )
	{
		payload.remove( key );
	}
	
	public PayloadValue getValue( String key )
	{
		return payload.get( key );
	}
	
	@Override
	protected ByteBuf encode()
	{
		ByteBuf buf = Unpooled.buffer();
		int sections = 0;
		
		for ( Entry<String, PayloadValue> e : payload.entrySet() )
		{
			sections++;
			if ( !e.getKey().startsWith( "*" ) )
				buf.writeBytes( new PayloadValue( e.getKey() ).encode() );
			buf.writeBytes( e.getValue().encode() ); // TODO Encode keys
		}
		
		ByteBuf fin = Unpooled.buffer();
		
		fin.writeByte( 0x0b );
		fin.writeByte( sections );
		fin.writeBytes( buf );
		
		return fin;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		for ( Entry<String, PayloadValue> e : payload.entrySet() )
		{
			sb.append( "," + e.getKey() + "=" + e.getValue() );
		}
		
		return "{" + ( ( sb.length() > 0 ) ? sb.toString().substring( 1 ) : "" ) + "}";
	}
}
