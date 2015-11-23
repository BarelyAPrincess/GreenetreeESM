/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.util;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.StringUtil;

import java.util.Random;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;

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
			BYTE2HEX[i] = ' ' + StringUtil.byteToHexStringPadded( i );
		
		// Generate the lookup table for hex dump paddings
		for ( i = 0; i < HEXPADDING.length; i++ )
		{
			int padding = HEXPADDING.length - i;
			StringBuilder buf = new StringBuilder( padding * 3 );
			for ( int j = 0; j < padding; j++ )
				buf.append( "   " );
			HEXPADDING[i] = buf.toString();
		}
		
		// Generate the lookup table for byte dump paddings
		for ( i = 0; i < BYTEPADDING.length; i++ )
		{
			int padding = BYTEPADDING.length - i;
			StringBuilder buf = new StringBuilder( padding );
			for ( int j = 0; j < padding; j++ )
				buf.append( ' ' );
			BYTEPADDING[i] = buf.toString();
		}
		
		// Generate the lookup table for byte-to-char conversion
		for ( i = 0; i < BYTE2CHAR.length; i++ )
			if ( i <= 0x1f || i >= 0x7f )
				BYTE2CHAR[i] = '.';
			else
				BYTE2CHAR[i] = ( char ) i;
		
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
	 * Appends the prefix of each hex dump row. Uses the look-up table for the buffer <= 64 KiB.
	 */
	private static void appendHexDumpRowPrefix( StringBuilder dump, int row, int rowStartIndex )
	{
		if ( row < HEXDUMP_ROWPREFIXES.length )
			dump.append( HEXDUMP_ROWPREFIXES[row] );
		else
		{
			dump.append( NEWLINE );
			dump.append( Long.toHexString( rowStartIndex & 0xFFFFFFFFL | 0x100000000L ) );
			dump.setCharAt( dump.length() - 9, '|' );
			dump.append( '|' );
		}
	}
	
	/**
	 * Generates a Packet Id for use in a new packet sent to server.
	 * 
	 * @return Packet Id
	 */
	public static byte[] generatePacketId()
	{
		int[] allowed = new int[] {0x00, 0x00, 0x00, 0x00, 0xc4, 0x9e, 0xc0};
		
		byte[] bytes = new byte[8];
		for ( int i = 0; i < 8; i++ )
			bytes[i] = ( byte ) allowed[new Random().nextInt( allowed.length )];
		
		return bytes;
	}
	
	public static String hex2Readable( byte... elements )
	{
		if ( elements == null )
			return "";
		
		// TODO Char Dump
		String result = "";
		char[] chars = Hex.encodeHex( elements, true );
		for ( int i = 0; i < chars.length; i = i + 2 )
			result += " " + chars[i] + chars[i + 1];
		
		if ( result.length() > 0 )
			result = result.substring( 1 );
		
		return result;
	}
	
	public static String hex2Readable( int... elements )
	{
		byte[] e2 = new byte[elements.length];
		for ( int i = 0; i < elements.length; i++ )
			e2[i] = ( byte ) elements[i];
		return hex2Readable( e2 );
	}
	
	public static String hexDump( ByteBuf buf )
	{
		return hexDump( buf, buf.readerIndex() );
	}
	
	public static String hexDump( ByteBuf buf, int highlightIndex )
	{
		if ( buf == null )
			return "Buffer: null!";
		
		if ( buf.capacity() < 1 )
			return "Buffer: 0B!";
		
		StringBuilder dump = new StringBuilder();
		
		final int startIndex = 0;
		final int endIndex = buf.capacity();
		final int length = endIndex - startIndex;
		final int fullRows = length >>> 4;
		final int remainder = length & 0xF;
		
		int highlightRow = -1;
		
		dump.append( NEWLINE + "         +-------------------------------------------------+" + NEWLINE + "         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |" + NEWLINE + "+--------+-------------------------------------------------+----------------+" );
		
		if ( highlightIndex > 0 )
		{
			highlightRow = highlightIndex >>> 4;
			highlightIndex = highlightIndex - ( 16 * highlightRow ) - 1;
			
			dump.append( NEWLINE + "|        |" + Strings.repeat( "   ", highlightIndex ) + " $$" + Strings.repeat( "   ", 15 - highlightIndex ) );
			dump.append( " |" + Strings.repeat( " ", highlightIndex ) + "$" + Strings.repeat( " ", 15 - highlightIndex ) + "|" );
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
				dump.append( BYTE2HEX[buf.getUnsignedByte( j )] );
			dump.append( " |" );
			
			// ASCII dump
			for ( int j = rowStartIndex; j < rowEndIndex; j++ )
				dump.append( BYTE2CHAR[buf.getUnsignedByte( j )] );
			dump.append( '|' );
			
			if ( highlightIndex > 0 && highlightRow == row + 1 )
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
				dump.append( BYTE2HEX[buf.getUnsignedByte( j )] );
			dump.append( HEXPADDING[remainder] );
			dump.append( " |" );
			
			// Ascii dump
			for ( int j = rowStartIndex; j < rowEndIndex; j++ )
				dump.append( BYTE2CHAR[buf.getUnsignedByte( j )] );
			dump.append( BYTEPADDING[remainder] );
			dump.append( '|' );
			
			if ( highlightIndex > 0 && highlightRow > fullRows + 1 )
				dump.append( " <--" );
		}
		
		dump.append( NEWLINE + "+--------+-------------------------------------------------+----------------+" );
		
		return dump.toString();
	}
	
	public static String random()
	{
		return random( 8, true, false, new String[0] );
	}
	
	public static String random( int length )
	{
		return random( length, true, false, new String[0] );
	}
	
	public static String random( int length, boolean numbers )
	{
		return random( length, numbers, false, new String[0] );
	}
	
	public static String random( int length, boolean numbers, boolean letters )
	{
		return random( length, numbers, letters, new String[0] );
	}
	
	public static String random( int length, boolean numbers, boolean letters, String[] allowedChars )
	{
		if ( allowedChars == null )
			allowedChars = new String[0];
		
		if ( numbers )
			allowedChars = ArrayUtils.addAll( allowedChars, new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"} );
		
		if ( letters )
			allowedChars = ArrayUtils.addAll( allowedChars, new String[] {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"} );
		
		String rtn = "";
		for ( int i = 0; i < length; i++ )
			rtn += allowedChars[new Random().nextInt( allowedChars.length )];
		
		return rtn;
	}
}
