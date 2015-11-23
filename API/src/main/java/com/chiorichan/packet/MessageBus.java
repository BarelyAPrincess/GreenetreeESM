/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.packet;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.codec.DecoderException;

import com.google.common.collect.Lists;

/**
 * Handles the I/O of March Commands
 */
public abstract class MessageBus
{
	private static final Packet PING_PACKET = new Packet( "ping" );
	private static final Packet PONG_PACKET = new Packet( "pong" );
	
	private Timer timer = new Timer( "Heartbeat", true );
	public List<MessageReceiver> receivers = Lists.newArrayList();
	protected MessageStream stream = null;
	private boolean connected = false;
	
	private List<Packet> cachedPackets = Lists.newLinkedList();
	private List<ByteBuf> cachedData = Lists.newLinkedList();
	
	public MessageBus()
	{
		register( new MessageReceiver()
		{
			@Override
			public boolean handle( MessageStream stream, Packet packet )
			{
				if ( "ping".equals( packet.command() ) )
				{
					stream.write( PONG_PACKET.encode() );
					return true;
				}
				else if ( "pong".equals( packet.command() ) )
				{
					System.out.println( "PONG!" );
					return true;
				}
				
				return false;
			}
		} );
		
		register( new MessageReceiver()
		{
			@Override
			public boolean handle( MessageStream stream, Packet packet )
			{
				if ( packet.command().getBytes()[0] == 0x65 )
					if ( packet.hasPayload() )
					{
						PayloadValue payload = packet.getPayload();
						
						String result = null;
						if ( payload.isString() )
							result = payload.getString();
						else if ( payload.isPayload() )
							result = ( ( PacketPayload ) payload ).getValue( "*0" ).getString();
						
						if ( "badCommand".equals( result ) )
						{
							badCommand( packet.packetId() );
							System.err.println( "Bad Command" );
							return true;
						}
						else if ( "failed".equals( result ) )
						{
							failedCommand( packet.packetId() );
							System.err.println( "Failed Command" );
							return true;
						}
					}
				
				return false;
			}
		} );
	}
	
	public MessageBus( MessageStream stream )
	{
		this();
		this.stream = stream;
	}
	
	protected abstract void badCommand( byte[] packetId );
	
	protected abstract void failedCommand( byte[] packetId );
	
	public void handle( final Packet... packets ) throws DecoderException
	{
		for ( Packet packet : packets )
			for ( MessageReceiver receiver : receivers )
				if ( receiver.handle( stream, packet ) )
					return;
		
		if ( packetReceived( packets ) )
			return;
		
		System.out.println( "WARNING: The last packet was not understood" );
	}
	
	public abstract void incoming( final ByteBuf buf );
	
	protected abstract boolean packetReceived( final Packet... packets );
	
	public void register( MessageReceiver receiver )
	{
		receivers.add( receiver );
	}
	
	public void sendPacket( final Packet... packets )
	{
		if ( connected )
		{
			for ( Packet packet : packets )
				stream.write( packet.encode() );
			stream.flush();
		}
		else
			cachedPackets.addAll( Arrays.asList( packets ) );
	}
	
	private void sendPing()
	{
		if ( stream == null || !connected )
			return;
		
		stream.write( PING_PACKET.encode() );
		stream.flush();
	}
	
	public void setStream( final MessageStream stream )
	{
		this.stream = stream;
	}
	
	protected abstract void start();
	
	public void start0()
	{
		connected = true;
		
		synchronized ( cachedPackets )
		{
			for ( Packet packet : cachedPackets )
				stream.write( packet.encode() );
			
			stream.flush();
			cachedPackets.clear();
		}
		
		synchronized ( cachedData )
		{
			for ( ByteBuf buf : cachedData )
				stream.write( buf );
			
			stream.flush();
			cachedData.clear();
		}
		
		start();
	}
	
	public void startPinger()
	{
		TimerTask task = new TimerTask()
		{
			@Override
			public void run()
			{
				sendPing();
			}
		};
		timer.scheduleAtFixedRate( task, 23000l, 30000l );
	}
	
	public void stopPinger()
	{
		timer.cancel();
	}
	
	public MessageStream stream()
	{
		return stream;
	}
	
	public void write( final ByteBuf... bufs )
	{
		if ( connected )
		{
			for ( ByteBuf buf : bufs )
				stream.write( buf );
			stream.flush();
		}
		else
			cachedData.addAll( Arrays.asList( bufs ) );
	}
}
