/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.packet;

/**
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class PacketUtils
{
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
}
