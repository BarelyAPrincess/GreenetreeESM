/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.packet;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Hex;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class Packet
{
	private PacketCommand cmd = null;
	private byte[] packetId = null;
	private PacketPayload payload;
	
	public class PacketCommand
	{
		private byte[] cmd = new byte[] {0x00};
		
		public PacketCommand( byte[] cmd )
		{
			this.cmd = cmd;
		}
		
		public PacketCommand( String cmd )
		{
			this.cmd = cmd.getBytes();
		}
		
		public void encode( ByteBuffer packet )
		{
			packet.put( ( byte ) 0x06 );
			
			packet.put( ( byte ) cmd.length );
			
			packet.put( cmd );
		}
		
		public String command()
		{
			return new String( cmd );
		}
	}
	
	/**
	 * Contains values that fail to be held by a map properly
	 */
	public enum StaticValues
	{
		NULL( null ), TRUE( true ), FALSE( false );
		
		Object value;
		
		StaticValues( Object value )
		{
			this.value = value;
		}
		
		@Override
		public String toString()
		{
			return ( String ) value;
		}
		
		public byte toByte()
		{
			if ( this == TRUE )
				return 0x08; // Need true value;
			if ( this == FALSE )
				return 0x07;
			return 0x00; // NULL
		}
		
		public Object toValue()
		{
			return value;
		}
	}
	
	public class PacketPayload
	{
		Map<String, Object> payload = Maps.newConcurrentMap();
		int keyCounter = 0;
		String previousKey = null;
		
		/**
		 * AssocArray's will ALWAYS contain a key=value pairing.
		 */
		boolean isAssocArray = false;
		
		public boolean isAssocArray()
		{
			return isAssocArray;
		}
		
		public String previousKey()
		{
			if ( previousKey != null )
			{
				Object obj = payload.get( previousKey );
				if ( obj == null || obj == StaticValues.NULL || ( obj instanceof Byte[] && ( ( Byte[] ) obj ).length < 1 ) )
					return previousKey;
				else
					return null; // Don't override previous if not null or empty.
			}
			
			return previousKey;
		}
		
		public PacketPayload putSubload( String key )
		{
			return putSubload( key, false );
		}
		
		public PacketPayload putSubload( String key, boolean isAssocArray )
		{
			previousKey = key;
			PacketPayload subload = new PacketPayload();
			payload.put( key, subload );
			this.isAssocArray = isAssocArray;
			return subload;
		}
		
		public void putKey( String data )
		{
			putKeyValue( data, StaticValues.NULL );
		}
		
		public void putValue( Object data )
		{
			putKeyValue( Integer.toString( keyCounter ), data );
			keyCounter++;
		}
		
		public void putKeyValue( String key, Object data )
		{
			previousKey = key;
			payload.put( key, data );
		}
		
		public byte[] getBytesAndDrop( String key )
		{
			byte[] value = getBytes( key );
			dropKey( key );
			return value;
		}
		
		public void dropKey( String key )
		{
			payload.remove( key );
		}
		
		public boolean getBool( String key )
		{
			Object obj = payload.get( key );
			
			if ( obj instanceof StaticValues )
			{
				if ( obj == StaticValues.TRUE )
					return true;
				else if ( obj == StaticValues.FALSE )
					return false;
			}
			
			if ( ! ( obj instanceof Boolean ) )
				return false;
			
			return ( Boolean ) obj;
		}
		
		public byte[] getBytes( String key )
		{
			Object obj = payload.get( key );
			
			if ( ! ( obj instanceof Byte[] ) )
				return new byte[0];
			
			return ( byte[] ) obj;
		}
		
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			
			for ( Entry<String, Object> e : payload.entrySet() )
			{
				String val = "";
				Object obj = e.getValue();
				if ( obj instanceof byte[] )
					val = Hex.encodeHexString( ( byte[] ) obj );
				else
					val = obj.toString();
				
				sb.append( "," + e.getKey() + "=" + val );
			}
			
			return "{" + ( ( sb.length() > 0 ) ? sb.toString().substring( 1 ) : "" ) + "}";
		}
	}
	
	public Packet( byte[] cmd )
	{
		this( cmd, null );
	}
	
	public Packet( byte[] cmd, byte[] packetId )
	{
		this.cmd = new PacketCommand( cmd );
		this.packetId = packetId;
	}
	
	public Packet( String cmd )
	{
		this( cmd, null );
	}
	
	public Packet( String cmd, byte[] packetId )
	{
		this.cmd = new PacketCommand( cmd );
		this.packetId = packetId;
	}
	
	public void addPacketId()
	{
		packetId = PacketUtils.generatePacketId();
	}
	
	public boolean hasPacketId()
	{
		return packetId != null;
	}
	
	public boolean hasPayload()
	{
		return payload != null;
	}
	
	private byte countParts()
	{
		int p = 0;
		
		if ( cmd != null )
			p++;
		
		if ( hasPacketId() )
			p++;
		
		if ( hasPayload() )
			p++;
		
		return ( byte ) p;
	}
	
	public byte[] encode()
	{
		ByteBuffer packet = ByteBuffer.allocate( 256 );
		
		packet.put( countParts() );
		
		cmd.encode( packet );
		
		if ( hasPacketId() )
		{
			packet.put( ( byte ) 0x03 );
			packet.put( packetId );
		}
		
		if ( hasPayload() )
		{
			
		}
		
		ByteBuffer newPacket = ByteBuffer.allocate( packet.position() + 9 );
		
		newPacket.put( ( byte ) 0x01 );
		
		newPacket.putShort( ( short ) ( packet.position() + 5 ) ); // Length + 4
		newPacket.putShort( ( short ) ( packet.position() + 1 ) ); // Length
		
		newPacket.put( new byte[] {0x00, 0x00, 0x00, 0x0b} );
		
		newPacket.put( packet.array(), 0, packet.position() );
		
		return newPacket.array();
	}
	
	/**
	 * @param data
	 *            The data to be read from
	 * @param payload
	 *            The payload for the data to be written to
	 * @return true if we seem to have reached the end of the payload
	 * @throws PacketException
	 *             thorwn on several reading errors
	 */
	private static boolean readMultipartPayload( ByteBuffer data, PacketPayload payload ) throws PacketException
	{
		if ( data.position() == data.capacity() )
			return true;
		
		int payloadStart = data.get();
		
		switch ( payloadStart )
		{
			case 0x02: // Make Last Key Empty
			{
				if ( payload.previousKey() != null )
					payload.putKeyValue( payload.previousKey(), new byte[0] );
				else
					payload.putValue( new byte[0] );
				break;
			}
			case 0x06:
			{
				int payloadLength = data.get();
				
				byte[] value = new byte[payloadLength];
				data.get( value, 0, payloadLength );
				
				if ( new String( value ).startsWith( "*" ) ) // Special
				{
					if ( "*end".equals( new String( value ) ) )
						return true;
				}
				
				if ( payload.isAssocArray )
					payload.putKeyValue( payload.previousKey(), value );
				else
					payload.putKey( new String( value ) );
				
				break;
			}
			case 0x08: // Boolean Value? True or False?
			{
				payload.putKeyValue( payload.previousKey(), StaticValues.TRUE );
				break;
			}
			case 0x09:
			{
				int payloadLength = data.get();
				
				byte[] value = new byte[payloadLength];
				data.get( value, 0, payloadLength );
				
				switch ( new String( value ) )
				{
					case "AssocArray":
					{
						payload.dropKey( payload.previousKey() );
						PacketPayload subload = payload.putSubload( payload.previousKey(), true );
						
						boolean end = false;
						do
						{
							end = readMultipartPayload( data, subload );
						}
						while ( !end );
						
						break;
					}
					default:
						throw new PacketException( "Data tried to change the previous key type and it was not reconized: " + value );
				}
				
				break;
			}
			case 0x0a:
			{
				// Unknown Type!
				break;
			}
			default:
				throw new PacketException( "Payload section was not started with the expected 0x06." );
		}
		
		return true;
	}
	
	private static void readSection( ByteBuffer data, Packet packet ) throws PacketException
	{
		if ( data.position() == data.capacity() )
			throw new PacketException( "SEVERE: The data array ended unexpectedly. We were expecting at least one more section." );
		
		int secType = data.get();
		
		switch ( secType )
		{
			case 0x03: // PacketId
			{
				byte[] cmdId = new byte[8];
				data.get( cmdId, 0, 8 );
				packet.packetId = cmdId;
				break;
			}
			case 0x0b: // Multipart Payload
			{
				if ( packet.payload == null )
					packet.payload = packet.new PacketPayload();
				
				int payloadSections = data.get();
				
				for ( int i = 0; i < payloadSections; i++ )
				{
					boolean end = readMultipartPayload( data, packet.payload );
					if ( end )
						break; // TODO check if end was expected.
				}
				
				break;
			}
			case 0x06: // String Payload
			{
				if ( packet.payload == null )
					packet.payload = packet.new PacketPayload();
				
				int payloadLength = data.get();
				
				byte[] payload = new byte[payloadLength];
				data.get( payload, 0, payloadLength );
				packet.payload.putValue( payload );
				
				break;
			}
			default:
				throw new PacketException( "SEVERE: The next data section was not started properly. It started with 0x" + Hex.encodeHexString( new byte[] {( byte ) secType} ) + ".", data );
		}
	}
	
	private static Packet decode0( byte[] msg ) throws PacketException
	{
		ByteBuffer data = ByteBuffer.wrap( msg );
		
		int dataType = data.get();
		
		if ( dataType != 0x0b )
			throw new PacketException( "The first byte in the data stream did not start with 0x0b." );
		
		int dataLength = data.get();
		
		if ( dataLength > 3 ) // > 3
			throw new PacketException( "Data over 3 sections is not supported." );
		
		int cmdType = data.get();
		
		if ( cmdType != 0x06 )
			throw new PacketException( "Expected the 'command' section." );
		
		int cmdLength = data.get();
		
		byte[] cmd = new byte[cmdLength];
		data.get( cmd, 0, cmdLength );
		Packet inital = new Packet( cmd );
		
		for ( int i = 1; i < dataLength; i++ )
			readSection( data, inital );
		
		System.out.println( "Succesfully Decoded Packet: " + inital );
		
		return inital;
	}
	
	public static Packet[] decode( byte[] msg ) throws PacketException
	{
		if ( msg == null )
			throw new PacketException( "Data can't be null" );
		
		ByteBuffer data = ByteBuffer.wrap( msg );
		
		try
		{
			List<Packet> packets = Lists.newLinkedList();
			
			if ( msg.length < 10 )
				throw new PacketException( "Data must be invalid since it does not contain the expected number of fields" );
			
			while ( data.position() < data.capacity() )
			{
				byte b = data.get();
				
				if ( b == ( byte ) 0x01 )
				{
					int len1 = data.getShort();
					int len2 = data.getShort();
					
					data.position( data.position() + 3 );
					
					byte[] dataRange = new byte[len2];
					data.get( dataRange, 0, len2 );
					
					packets.add( decode0( dataRange ) );
				}
			}
			
			if ( data.position() < data.capacity() )
			{
				byte[] tmp = new byte[data.capacity() - data.position()];
				data.get( tmp, 0, data.capacity() - data.position() );
				System.err.println( "Warning: The data array has excess data on the end that was unreadable. Excess: " + Hex.encodeHexString( tmp ) );
			}
			
			return packets.toArray( new Packet[0] );
		}
		catch ( PacketException e )
		{
			if ( !e.hasBuffer() )
				e.putBuffer( data );
			throw e;
		}
	}
	
	public String command()
	{
		return ( cmd == null ) ? null : cmd.command();
	}
	
	@Override
	public String toString()
	{
		return "Packet{cmd=" + command() + ",packetId=" + Hex.encodeHexString( packetId ) + ",payload=" + payload + "}";
	}
	
	public byte[] packetId()
	{
		return packetId;
	}
}
