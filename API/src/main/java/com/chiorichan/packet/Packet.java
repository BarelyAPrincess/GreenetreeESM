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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.Validate;

import com.chiorichan.packet.PacketPayload.PayloadType;
import com.chiorichan.util.PacketUtils;
import com.google.common.collect.Lists;

/**
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class Packet
{
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
		
		public String command()
		{
			return new String( cmd );
		}
		
		protected ByteBuf encode()
		{
			ByteBuf buf = Unpooled.buffer();
			buf.writeByte( ( byte ) 0x06 );
			buf.writeByte( ( byte ) cmd.length );
			buf.writeBytes( cmd );
			return buf;
		}
	}
	
	private PacketCommand cmd = null;
	private byte[] packetId = null;
	private String hash = null;
	
	private PayloadValue payload;
	
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
				int start = data.readerIndex();
				int packetStart = data.readByte();
				
				if ( packetStart == 0x01 )
				{
					int packetLength1 = data.readShort();
					int packetLength2 = data.readShort();
					
					if ( packetLength1 + 4 == packetLength2 )
						// This is valid apparently, don't know why they did this???
						System.out.println( "NOTICE: packetLength1 did not match packetLength2, don't know why... " + PacketUtils.hex2Readable( packetLength1, packetLength2 ) );
					
					int z1 = data.readByte();
					int z2 = data.readByte();
					int z3 = data.readByte();
					
					if ( z1 != 0x00 || z2 != 0x00 || z3 != 0x00 )
						// Still not sure what these three bits are used for, but they are always 0x00.
						System.out.println( "NOTICE: z1, z2, or z3 was not 0x00, don't know why... " + Hex.encodeHexString( new byte[] {( byte ) z1, ( byte ) z2, ( byte ) z3} ) );
					
					byte[] b = new byte[ ( data.readerIndex() + packetLength2 ) - start];
					data.getBytes( start, b );
					
					// Use MD5 Hash
					packets.add( decode0( data.readBytes( packetLength2 ) ).setHash( Base64.encodeBase64String( b ) ) );
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
				System.out.println( "Warning: The data array has excess data that was not understandable. Excess: " + PacketUtils.hex2Readable( tmp ) );
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
	
	private static Packet decode0( ByteBuf data ) throws PacketException
	{
		int dataType = data.readByte();
		
		if ( dataType != 0x0b )
			throw new PacketException( "The first byte in the data stream did not start with 0x0b." );
		
		int dataLength = data.readByte();
		
		if ( dataLength > 3 )
			throw new PacketException( "Data over 3 sections is not supported ATM." );
		
		int cmdType = data.readByte();
		
		if ( cmdType != 0x06 )
			throw new PacketException( "Expected the 'command' section." );
		
		int cmdLength = data.readByte();
		
		byte[] cmd = new byte[cmdLength];
		data.readBytes( cmd );
		Packet inital = new Packet( cmd );
		
		for ( int i = 1; i < dataLength; i++ )
			readSection( data, inital );
		
		System.out.println( "Successfully Decoded Packet: " + inital );
		
		return inital;
	}
	
	public static String md5( byte[] bytes )
	{
		return DigestUtils.md5Hex( bytes );
	}
	
	public static String md5( String str )
	{
		if ( str == null )
			return null;
		
		return DigestUtils.md5Hex( str );
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
	private static void readMultipartPayload( ByteBuf data, PacketPayload payload, int maxPayloads ) throws PacketException
	{
		if ( !data.isReadable() )
			return;
		
		// Start in Key Mode
		boolean keyMode = true;
		
		for ( int i = 0; i < maxPayloads; i++ )
		{
			int payloadStart = data.readByte();
			
			switch ( payloadStart )
			{
				case 0x0a: // End of AssocArray - Previous key should be *end
				{
					if ( "*end".equals( payload.getPreviousKey() ) )
						System.out.println( "WARNING: We hit 0A marker indicating end of AssocArray but last key was not *end" );
					
					return;
				}
				case 0x01: // Unknown Key
				{
					break;
				}
				case 0x02: // Make Last Key Empty
				{
					keyMode = true;
					payload.putValue( PayloadValue.EMPTY );
					break;
				}
				case 0x03: // Unknown Key - Make Last Key 64-bit Float/Long
				{
					payload.putValue( data.readLong() );
					break;
				}
				case 0x06: // Normal String
				{
					int payloadLength = data.readByte();
					
					byte[] value = new byte[payloadLength];
					data.readBytes( value );
					
					// if ( new String( value ).startsWith( "*" ) )
					// if ( "*end".equals( new String( value ) ) )
					// return true;
					
					if ( keyMode )
						payload.putKey( new String( value ) );
					else
						payload.putValue( new PayloadValue( value ) );
					
					keyMode = false;
					break;
				}
				case 0x08: // Boolean Value? True or False?
				{
					payload.putValue( PayloadValue.TRUE );
					break;
				}
				case 0x09: // Typically means following payload is AssocArray
				{
					int payloadLength = data.readByte();
					
					byte[] value = new byte[payloadLength];
					data.readBytes( value );
					
					switch ( new String( value ) )
					{
						case "AssocArray":
						{
							PacketPayload subload = payload.putSubload( PayloadType.ASSOC_ARRAY );
							readMultipartPayload( data, subload, 9999 ); // No Max Available ATM
							break;
						}
						default:
							throw new PacketException( "Special data 0x09 encountered but it was not reconized: " + value );
					}
					
					break;
				}
				default:
					throw new PacketException( "Payload section was not started with the expected byte, payload start was: " + PacketUtils.hex2Readable( payloadStart ) + " at index " + ( data.readerIndex() - 1 ) );
			}
			
		}
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
				
				// Number of Payloads
				int payloadSections = data.readByte(); // Ignored?
				
				readMultipartPayload( data, ( PacketPayload ) packet.payload, payloadSections );
				
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
	
	public Packet addPacketId()
	{
		packetId = PacketUtils.generatePacketId();
		return this;
	}
	
	public Packet addPacketId( byte[] packetId )
	{
		this.packetId = packetId;
		return this;
	}
	
	public String command()
	{
		return ( cmd == null ) ? null : cmd.command();
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
		
		packet.writeByte( 0x0b );
		
		packet.writeByte( countParts() );
		
		packet.writeBytes( cmd.encode() );
		
		if ( hasPacketId() )
		{
			packet.writeByte( 0x03 );
			packet.writeBytes( packetId );
		}
		
		if ( hasPayload() )
			packet.writeBytes( payload.encode() );
		
		ByteBuf newPacket = Unpooled.buffer();
		
		newPacket.writeByte( 0x01 );
		
		newPacket.writeShort( ( short ) ( packet.writerIndex() + 4 ) ); // Length + 4
		newPacket.writeShort( ( short ) ( packet.writerIndex() ) ); // Length
		
		newPacket.writeBytes( new byte[] {0x00, 0x00, 0x00} );
		
		newPacket.writeBytes( packet );
		
		if ( hasHash() )
		{
			byte[] b = new byte[newPacket.writerIndex()];
			newPacket.getBytes( 0, b );
			
			// if ( !md5( b ).equals( hash() ) )
			
			if ( !Base64.encodeBase64String( b ).equals( hash() ) )
			{
				System.out.println( "Packet Hash Mismatch!" );
				
				byte[] orig = Base64.decodeBase64( hash() );
				
				System.out.println( "Mismatch Length: " + b.length + " <--> " + orig.length );
				System.out.println( "Mismatch Diff: " );
				
				for ( int i = 0; i < Math.min( b.length, orig.length ); i++ )
					if ( b[i] != orig[i] )
						System.out.println( "Index " + i + " mismatched: " + PacketUtils.hex2Readable( b[i] ) + " <--> " + PacketUtils.hex2Readable( orig[i] ) );
			}
		}
		
		return newPacket;
	}
	
	public PayloadValue getPayload()
	{
		return payload;
	}
	
	public String hash()
	{
		return hash;
	}
	
	public boolean hasHash()
	{
		return hash != null;
	}
	
	public boolean hasPacketId()
	{
		return packetId != null;
	}
	
	public boolean hasPayload()
	{
		return payload != null;
	}
	
	public byte[] packetId()
	{
		return packetId;
	}
	
	private Packet setHash( String hash )
	{
		this.hash = hash;
		return this;
	}
	
	public Packet setPayload( Map<String, Object> payload )
	{
		this.payload = new PacketPayload( payload );
		return this;
	}
	
	public Packet setPayload( String payload )
	{
		this.payload = new PayloadValue( payload );
		return this;
	}
	
	@Override
	public String toString()
	{
		return "Packet{cmd=" + command() + ",packetId=" + PacketUtils.hex2Readable( packetId ) + ",payload=" + payload + "}";
	}
}
