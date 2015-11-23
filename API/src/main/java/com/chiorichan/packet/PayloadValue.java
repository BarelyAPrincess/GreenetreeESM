/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import com.chiorichan.util.ObjectUtil;
import com.chiorichan.util.PacketUtils;

/**
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class PayloadValue
{
	enum ValueType
	{
		NULL, TRUE, FALSE, BYTES, STRING, PAYLOAD, LONG;
		
		public static ValueType Boolean( boolean val )
		{
			return ( val ) ? ValueType.TRUE : ValueType.FALSE;
		}
	}
	
	public static final PayloadValue NULL = new PayloadValue();
	public static final PayloadValue TRUE = new PayloadValue( true );
	public static final PayloadValue FALSE = new PayloadValue( false );
	
	public static final PayloadValue EMPTY = new PayloadValue( new byte[0] );
	ValueType type = ValueType.NULL;
	
	Object value = null;
	
	public PayloadValue()
	{
		if ( this instanceof PacketPayload )
			type = ValueType.PAYLOAD;
	}
	
	public PayloadValue( boolean value )
	{
		type = ValueType.Boolean( value );
	}
	
	public PayloadValue( byte[] value )
	{
		type = ValueType.BYTES;
		this.value = value;
	}
	
	public PayloadValue( String value )
	{
		type = ValueType.STRING;
		this.value = value;
	}
	
	public PacketPayload asPayload()
	{
		return ( PacketPayload ) this;
	}
	
	protected ByteBuf encode()
	{
		ByteBuf buf = Unpooled.buffer();
		byte[] value = getBytes();
		
		if ( value == null )
			switch ( type )
			{
				case TRUE:
					buf.writeByte( 0x08 ); // true???
					break;
				case FALSE:
					buf.writeByte( 0x07 ); // false???
					break;
			}
		else if ( value.length == 0 )
			buf.writeByte( 0x02 ); // empty
		else if ( value.length == 1 && value[0] == 0x0a )
			buf.writeByte( 0x0a );
		else if ( type == ValueType.LONG )
		{
			buf.writeByte( 0x03 );
			buf.writeBytes( value );
		}
		else
		{
			buf.writeByte( 0x06 );
			buf.writeByte( ( byte ) value.length );
			buf.writeBytes( value );
		}
		
		return buf;
	}
	
	public boolean getBoolean()
	{
		return ObjectUtil.castToBool( value );
	}
	
	public byte[] getBytes()
	{
		if ( type == ValueType.BYTES )
			return ( byte[] ) value;
		if ( type == ValueType.LONG )
		{
			ByteBuf buf = Unpooled.directBuffer( 8 );
			buf.writeLong( ( Long ) value );
			return buf.array();
		}
		if ( type == ValueType.STRING )
			return ( ( String ) value ).getBytes();
		return null;
	}
	
	public PacketPayload getPayload()
	{
		if ( type == ValueType.PAYLOAD )
			return ( ( PacketPayload ) this );
		return null;
	}
	
	public String getString()
	{
		if ( type == ValueType.STRING )
			return ( ( String ) value );
		if ( type == ValueType.BYTES )
			return new String( ( ( byte[] ) value ) );
		if ( type == ValueType.TRUE )
			return "true";
		if ( type == ValueType.FALSE )
			return "false";
		return null;
	}
	
	public ValueType getType()
	{
		return type;
	}
	
	public boolean isBoolean()
	{
		return type == ValueType.TRUE || type == ValueType.FALSE;
	}
	
	public boolean isBytes()
	{
		return type == ValueType.BYTES;
	}
	
	public boolean isLong()
	{
		return type == ValueType.LONG;
	}
	
	public boolean isNull()
	{
		return type == ValueType.NULL;
	}
	
	public boolean isPayload()
	{
		return type == ValueType.PAYLOAD;
	}
	
	public boolean isString()
	{
		return type == ValueType.STRING;
	}
	
	protected void putValue( Object val )
	{
		if ( val instanceof Long )
			type = ValueType.LONG;
		if ( val instanceof String )
			type = ValueType.STRING;
		else if ( val instanceof byte[] )
			type = ValueType.BYTES;
		else if ( val instanceof Boolean )
		{
			type = ValueType.Boolean( ( boolean ) val );
			return;
		}
		else
			return;
		
		value = val;
	}
	
	@Override
	public String toString()
	{
		String val = "";
		if ( isBytes() )
			val = PacketUtils.hex2Readable( getBytes() );
		else if ( isBoolean() )
			val = ObjectUtil.castToString( getBoolean() );
		else if ( isNull() )
			val = "null";
		else
			val = value.toString();
		return val;
	}
}
