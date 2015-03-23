/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.Validate;

import com.chiorichan.packet.PacketPayload.PayloadType;
import com.google.common.collect.Lists;

/**
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class Packet
{
	private PacketCommand cmd = null;
	private byte[] packetId = null;
	private PayloadValue payload;
	
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
		
		protected ByteBuf encode()
		{
			ByteBuf buf = Unpooled.buffer();
			buf.writeByte( ( byte ) 0x06 );
			buf.writeByte( ( byte ) cmd.length );
			buf.writeBytes( cmd );
			return buf;
		}
		
		public String command()
		{
			return new String( cmd );
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
	
	public ByteBuf encode()
	{
		ByteBuf packet = Unpooled.buffer();
		
		packet.writeByte( countParts() );
		
		packet.writeBytes( cmd.encode() );
		
		if ( hasPacketId() )
		{
			packet.writeByte( 0x03 );
			packet.writeBytes( packetId );
		}
		
		if ( hasPayload() )
		{
			packet.writeBytes( payload.encode() );
		}
		
		ByteBuf newPacket = Unpooled.buffer();
		
		newPacket.writeByte( 0x01 );
		
		newPacket.writeShort( ( short ) ( packet.writerIndex() + 5 ) ); // Length + 4
		newPacket.writeShort( ( short ) ( packet.writerIndex() + 1 ) ); // Length
		
		newPacket.writeBytes( new byte[] {0x00, 0x00, 0x00, 0x0b} );
		
		newPacket.writeBytes( packet );
		
		return newPacket;
	}
	
	/**
	 * @param data
	 *            The data to be read from
	 * @param payload
	 *            The payload for the data to be written to
	 * @return true if we have reached the end of the payload
	 * @throws PacketException
	 *             thrown on several reading errors
	 */
	private static boolean readMultipartPayload( ByteBuf data, PacketPayload payload ) throws PacketException
	{
		if ( !data.isReadable() )
			return true;
		
		int payloadStart = data.readByte();
		
		switch ( payloadStart )
		{
			case 0x02: // Make Last Key Empty
			{
				// if ( payload.previousKey() != null )
				// payload.putKeyValue( payload.previousKey(), new byte[0] );
				// else
				payload.putValue( PayloadValue.EMPTY );
				break;
			}
			case 0x06:
			{
				int payloadLength = data.readByte();
				
				byte[] value = new byte[payloadLength];
				data.readBytes( value );
				
				if ( new String( value ).startsWith( "*" ) ) // Special
				{
					if ( "*end".equals( new String( value ) ) )
						return true;
				}
				
				payload.putValue( new PayloadValue( value ) );
				break;
			}
			case 0x08: // Boolean Value? True or False?
			{
				payload.putValue( PayloadValue.TRUE );
				break;
			}
			case 0x09:
			{
				int payloadLength = data.readByte();
				
				byte[] value = new byte[payloadLength];
				data.readBytes( value );
				
				switch ( new String( value ) )
				{
					case "AssocArray":
					{
						PacketPayload subload = payload.putSubload( PayloadType.ASSOC_ARRAY );
						
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
				throw new PacketException( "Payload section was not started with the expected byte." );
		}
		
		return false;
	}
	
	private static void readSection( ByteBuf data, Packet packet ) throws PacketException
	{
		if ( !data.isReadable() )
			throw new PacketException( "SEVERE: The data array ended unexpectedly. We were expecting at least one more section." );
		
		int secType = data.readByte();
		
		switch ( secType )
		{
			case 0x03: // PacketId
			{
				byte[] cmdId = new byte[8];
				data.readBytes( cmdId );
				packet.packetId = cmdId;
				break;
			}
			case 0x0b: // Multipart Payload
			{
				// XXX Could there be more then just one multipart or string payload?
				packet.payload = new PacketPayload();
				
				int payloadSections = data.readByte();
				
				for ( int i = 0; i < payloadSections; i++ )
					if ( readMultipartPayload( data, ( PacketPayload ) packet.payload ) )
						break; // TODO check if end was expected.
						
				break;
			}
			case 0x06: // String Payload
			{
				int payloadLength = data.readByte();
				
				byte[] payload = new byte[payloadLength];
				data.readBytes( payload );
				packet.payload = new PayloadValue( payload );
				
				break;
			}
			default:
				throw new PacketException( "SEVERE: The next data section was not started properly. It started with 0x" + Hex.encodeHexString( new byte[] {( byte ) secType} ) + ".", data );
		}
	}
	
	private static Packet decode0( ByteBuf data ) throws PacketException
	{
		int dataType = data.readByte();
		
		if ( dataType != 0x0b )
			throw new PacketException( "The first byte in the data stream did not start with 0x0b." );
		
		int dataLength = data.readByte();
		
		if ( dataLength > 3 ) // > 3
			throw new PacketException( "Data over 3 sections is not supported." );
		
		int cmdType = data.readByte();
		
		if ( cmdType != 0x06 )
			throw new PacketException( "Expected the 'command' section." );
		
		int cmdLength = data.readByte();
		
		byte[] cmd = new byte[cmdLength];
		data.readBytes( cmd );
		Packet inital = new Packet( cmd );
		
		for ( int i = 1; i < dataLength; i++ )
			readSection( data, inital );
		
		System.out.println( "Succesfully Decoded Packet: " + inital );
		
		return inital;
	}
	
	public static Packet[] decode( ByteBuf data ) throws PacketException
	{
		Validate.notNull( data );
		
		if ( !data.isReadable() )
			throw new PacketException( "Data stream is not readable." );
		
		try
		{
			List<Packet> packets = Lists.newLinkedList();
			
			do
			{
				int packetStart = data.readByte();
				
				if ( packetStart == 0x01 )
				{
					int packetLength1 = data.readShort();
					int packetLength2 = data.readShort();
					
					if ( packetLength1 + 4 == packetLength2 )
					{
						// This is valid apparently, don't know why they did this???
						System.out.println( "NOTICE: packetLength1 did not match packetLength2, don't know why... " + PacketUtils.hex2Readable( packetLength1, packetLength2 ) );
					}
					
					int z1 = data.readByte();
					int z2 = data.readByte();
					int z3 = data.readByte();
					
					if ( z1 != 0x00 || z2 != 0x00 || z3 != 0x00 )
					{
						// Still not sure what these three bits are used for, but they are always 0x00.
						System.out.println( "NOTICE: z1, z2, or z3 were not 0x00, don't know why... " + Hex.encodeHexString( new byte[] {( byte ) z1, ( byte ) z2, ( byte ) z3} ) );
					}
					
					packets.add( decode0( data.readBytes( packetLength2 ) ) );
				}
				else
				{
					data.readerIndex( data.readerIndex() - 1 );
					break;
				}
			}
			while ( data.isReadable() );
			
			if ( data.isReadable() )
			{
				byte[] tmp = new byte[data.readableBytes()];
				data.readBytes( tmp );
				System.err.println( "Warning: The data array has excess data that was not readable. Excess: " + PacketUtils.hex2Readable( tmp ) );
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
		return "Packet{cmd=" + command() + ",packetId=" + PacketUtils.hex2Readable( packetId ) + ",payload=" + payload + "}";
	}
	
	public byte[] packetId()
	{
		return packetId;
	}
	
	public void setPayload( String payload )
	{
		this.payload = new PayloadValue( payload );
	}
	
	public void setPayload( Map<String, PayloadValue> payload )
	{
		this.payload = new PacketPayload( payload );
	}
}
