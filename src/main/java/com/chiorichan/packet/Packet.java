/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.packet;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.commons.codec.binary.Hex;

import com.google.common.collect.Lists;


/**
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class Packet
{
	private PacketCommand cmd = null;
	private byte[] packetId = null;
	private PacketPayload payload = null;
	
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
	
	public class PacketPayload
	{
		
	}
	
	private Packet()
	{
		
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
	
	private static Packet decode0( byte[] msg ) throws PacketException
	{
		ByteBuffer data = ByteBuffer.wrap( msg );
		
		int dataType = data.get();
		int dataLen = data.get();
		
		if ( dataLen > 3 ) // > 3
			throw new PacketException( "Data over 3 sections is unknown and not supported! " );
		
		if ( data.get() != ( byte ) 0x06 )
			throw new PacketException( "Expected the first section, called command" );
		
		int cl = data.get();
		
		byte[] cmd = new byte[cl];
		
		data.get( cmd, 0, cl );
		
		Packet inital = new Packet( cmd );
		
		if ( dataLen > 1 )
		{
			if ( data.position() < data.capacity() )
			{
				int idenStart = data.get();
				
				if ( idenStart != 0x03 )
					System.err.println( "WARNING: The next data section was not started properly with 0x03, ignoring." );
				else
				{
					byte[] cmdId = new byte[8];
					data.get( cmdId, 0, 8 );
					inital.packetId = cmdId;
				}
			}
			else
			{
				System.err.println( "WARNING: The data array ended unexpectedly. We were expecting at least one more section." );
			}
		}
		
		System.out.println( "Succesfully Decoded Packet: " + inital );
		
		return inital;
	}
	
	public static Packet[] decode( byte[] msg ) throws PacketException
	{
		List<Packet> packets = Lists.newLinkedList();
		
		if ( msg == null )
			throw new PacketException( "Data can't be null" );
		
		if ( msg.length < 10 )
			throw new PacketException( "Data must be invalid since it does not contain the expected number of fields" );
		
		ByteBuffer data = ByteBuffer.wrap( msg );
		
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
			System.err.println( "Warning: The data array has excess data on the end that was unreadable." );
		
		return packets.toArray( new Packet[0] );
	}
	
	public String command()
	{
		return ( cmd == null ) ? null : cmd.command();
	}
	
	@Override
	public String toString()
	{
		return "Packet{cmd=" + command() + ",packetId=" + Hex.encodeHexString( packetId ) + "}";
	}
	
	public byte[] packetId()
	{
		return packetId;
	}
}
