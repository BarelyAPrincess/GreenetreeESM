/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.packet;

import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Hex;


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
	}
	
	public class PacketPayload
	{
		
	}
	
	private Packet()
	{
		
	}
	
	public Packet( String cmd )
	{
		this.cmd = new PacketCommand( cmd );
		// packetId = PacketUtils.generatePacketId();
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
	
	public static Packet decode( byte[] data ) throws PacketException
	{
		if ( data == null )
			throw new PacketException( "data can't be null" );
		
		
		// String hex = Hex.encodeHexString( content ).toLowerCase();
		
		Packet inital = new Packet();
	}
}
