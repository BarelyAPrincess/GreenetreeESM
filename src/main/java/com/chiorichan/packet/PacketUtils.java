/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.packet;

import io.netty.util.internal.StringUtil;

import java.nio.ByteBuffer;

import com.google.common.base.Strings;

/**
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class PacketUtils
{
	private static final String[] HEXDUMP_ROWPREFIXES = new String[65536 >>> 4];
	private static final String NEWLINE = StringUtil.NEWLINE;
	private static final String[] BYTE2HEX = new String[256];
	private static final char[] BYTE2CHAR = new char[256];
	private static final String[] HEXPADDING = new String[16];
	private static final String[] BYTEPADDING = new String[16];
	
	static
	{
		int i;
		
		// Generate the lookup table for byte-to-hex-dump conversion
		for ( i = 0; i < BYTE2HEX.length; i++ )
		{
			BYTE2HEX[i] = ' ' + StringUtil.byteToHexStringPadded( i );
		}
		
		// Generate the lookup table for hex dump paddings
		for ( i = 0; i < HEXPADDING.length; i++ )
		{
			int padding = HEXPADDING.length - i;
			StringBuilder buf = new StringBuilder( padding * 3 );
			for ( int j = 0; j < padding; j++ )
			{
				buf.append( "   " );
			}
			HEXPADDING[i] = buf.toString();
		}
		
		// Generate the lookup table for byte dump paddings
		for ( i = 0; i < BYTEPADDING.length; i++ )
		{
			int padding = BYTEPADDING.length - i;
			StringBuilder buf = new StringBuilder( padding );
			for ( int j = 0; j < padding; j++ )
			{
				buf.append( ' ' );
			}
			BYTEPADDING[i] = buf.toString();
		}
		
		// Generate the lookup table for byte-to-char conversion
		for ( i = 0; i < BYTE2CHAR.length; i++ )
		{
			if ( i <= 0x1f || i >= 0x7f )
			{
				BYTE2CHAR[i] = '.';
			}
			else
			{
				BYTE2CHAR[i] = ( char ) i;
			}
		}
		
		// Generate the lookup table for the start-offset header in each row (up to 64KiB).
		for ( i = 0; i < HEXDUMP_ROWPREFIXES.length; i++ )
		{
			StringBuilder buf = new StringBuilder( 12 );
			buf.append( NEWLINE );
			buf.append( Long.toHexString( i << 4 & 0xFFFFFFFFL | 0x100000000L ) );
			buf.setCharAt( buf.length() - 9, '|' );
			buf.append( '|' );
			HEXDUMP_ROWPREFIXES[i] = buf.toString();
		}
	}
	
	/**
	 * Generates a Packet Id for use in a new packet sent to server.
	 * 
	 * @return Packet Id
	 */
	public static byte[] generatePacketId()
	{
		// Temp until I can implement this
		int[] hex = new int[] {0x00, 0x00, 0x00, 0x00, 0x00, 0xc4, 0x9e, 0xc0};
		byte[] bytes = new byte[8];
		
		for ( int i = 0; i < 8; i++ )
			bytes[i] = ( byte ) hex[i];
		
		return bytes;
	}
	
	public static String hexDump( ByteBuffer buf )
	{
		return hexDump( buf, buf.position() );
	}
	
	public static String hexDump( ByteBuffer buf, int highlightIndex )
	{
		if ( buf == null )
			return "Buffer: null!";
		
		if ( buf.capacity() < 1 )
		{
			return "Buffer: 0B!";
		}
		
		StringBuilder dump = new StringBuilder();
		
		final int startIndex = 0;
		final int endIndex = buf.capacity();
		final int length = endIndex - startIndex;
		final int fullRows = length >>> 4;
		final int remainder = length & 0xF;
		
		int highlightRow = -1;
		
		dump.append( NEWLINE + "         +-------------------------------------------------+" + NEWLINE + "         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |" + NEWLINE + "+--------+-------------------------------------------------+----------------+" );
		
		if ( highlightIndex >= 0 )
		{
			highlightRow = highlightIndex >>> 4;
			highlightIndex = highlightIndex - ( 16 * highlightRow );
			
			dump.append( NEWLINE + "|        |" + ( ( highlightIndex > 1 ) ? Strings.repeat( "   ", highlightIndex - 1 ) : "" ) + " $$" + Strings.repeat( "   ", 16 - highlightIndex ) );
			dump.append( " |" + ( ( highlightIndex > 1 ) ? Strings.repeat( " ", highlightIndex - 1 ) : "" ) + "$" + Strings.repeat( " ", 16 - highlightIndex ) + "|" );
		}
		
		// Dump the rows which have 16 bytes.
		for ( int row = 0; row < fullRows; row++ )
		{
			int rowStartIndex = row << 4;
			
			// Per-row prefix.
			appendHexDumpRowPrefix( dump, row, rowStartIndex );
			
			// Hex dump
			int rowEndIndex = rowStartIndex + 16;
			for ( int j = rowStartIndex; j < rowEndIndex; j++ )
			{
				dump.append( BYTE2HEX[getUnsignedByte( buf, j )] );
			}
			dump.append( " |" );
			
			// ASCII dump
			for ( int j = rowStartIndex; j < rowEndIndex; j++ )
			{
				dump.append( BYTE2CHAR[getUnsignedByte( buf, j )] );
			}
			dump.append( '|' );
			
			if ( highlightIndex >= 0 && highlightRow == row + 1 )
				dump.append( " <--" );
		}
		
		// Dump the last row which has less than 16 bytes.
		if ( remainder != 0 )
		{
			int rowStartIndex = fullRows << 4;
			appendHexDumpRowPrefix( dump, fullRows, rowStartIndex );
			
			// Hex dump
			int rowEndIndex = rowStartIndex + remainder;
			for ( int j = rowStartIndex; j < rowEndIndex; j++ )
			{
				dump.append( BYTE2HEX[getUnsignedByte( buf, j )] );
			}
			dump.append( HEXPADDING[remainder] );
			dump.append( " |" );
			
			// Ascii dump
			for ( int j = rowStartIndex; j < rowEndIndex; j++ )
			{
				dump.append( BYTE2CHAR[getUnsignedByte( buf, j )] );
			}
			dump.append( BYTEPADDING[remainder] );
			dump.append( '|' );
			
			if ( highlightIndex >= 0 && highlightRow > fullRows + 1 )
				dump.append( " <--" );
		}
		
		dump.append( NEWLINE + "+--------+-------------------------------------------------+----------------+" );
		
		return dump.toString();
	}
	
	public static short getUnsignedByte( ByteBuffer bb, int position )
	{
		return ( ( short ) ( bb.get( position ) & ( short ) 0xff ) );
	}
	
	/**
	 * Appends the prefix of each hex dump row. Uses the look-up table for the buffer <= 64 KiB.
	 */
	private static void appendHexDumpRowPrefix( StringBuilder dump, int row, int rowStartIndex )
	{
		if ( row < HEXDUMP_ROWPREFIXES.length )
		{
			dump.append( HEXDUMP_ROWPREFIXES[row] );
		}
		else
		{
			dump.append( NEWLINE );
			dump.append( Long.toHexString( rowStartIndex & 0xFFFFFFFFL | 0x100000000L ) );
			dump.setCharAt( dump.length() - 9, '|' );
			dump.append( '|' );
		}
	}
}
