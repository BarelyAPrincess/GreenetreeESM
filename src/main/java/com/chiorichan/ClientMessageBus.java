/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan;

import io.netty.buffer.ByteBuf;

import java.util.Map;

import com.chiorichan.packet.MessageBus;
import com.chiorichan.packet.Packet;
import com.google.common.collect.Maps;

public class ClientMessageBus extends MessageBus
{
	private final ServerMessageBus bus;
	
	public ClientMessageBus( final ServerMessageBus bus )
	{
		this.bus = bus;
	}
	
	@Override
	protected void badCommand( byte[] packetId )
	{
		Map<String, Object> data = Maps.newHashMap();
		data.put( "*0", "badCommand" );
		bus.sendPacket( new Packet( new byte[] {0x65} ).addPacketId( packetId ).setPayload( data ) );
	}
	
	@Override
	protected void failedCommand( byte[] packetId )
	{
		Map<String, Object> data = Maps.newHashMap();
		data.put( "*0", "failed" );
		bus.sendPacket( new Packet( new byte[] {0x65} ).addPacketId( packetId ).setPayload( data ) );
	}
	
	@Override
	public void incoming( ByteBuf buf )
	{
		bus.write( buf );
	}
	
	@Override
	protected boolean packetReceived( final Packet... packet )
	{
		bus.sendPacket( packet );
		return true;
	}
	
	@Override
	protected void start()
	{
		System.out.println( "Client Start Indicated" );
	}
}
