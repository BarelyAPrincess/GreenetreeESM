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
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;

import com.chiorichan.packet.MessageBus;
import com.chiorichan.packet.MessageStream;
import com.chiorichan.packet.Packet;
import com.google.common.collect.Maps;

public class ServerMessageBus extends MessageBus
{
	private final NetClient client;
	private final ClientMessageBus bus;
	
	public ServerMessageBus( boolean ssl, String url, ChannelHandlerContext ctx )
	{
		super( new MessageStream( ctx ) );
		
		bus = new ClientMessageBus( this );
		client = new NetClient( "96.95.92.35", 2804, url, ssl, bus );
		// "162.220.160.185", 443
		
		client.connect();
		
		/*
		 * register( new MessageReceiver()
		 * {
		 * 
		 * @Override
		 * public boolean handle( MessageStream stream, Packet packet )
		 * {
		 * if ( "SGIA".equals( packet.command() ) )
		 * {
		 * stream.write( new Packet( new byte[] {0x65} ).setPayload( "success" ).encode() );
		 * 
		 * return true;
		 * }
		 * return false;
		 * }
		 * } );
		 * 
		 * 
		 * register( new MessageReceiver()
		 * {
		 * 
		 * @Override
		 * public boolean handle( MessageStream stream, Packet packet )
		 * {
		 * if ( "~LGIN".equals( packet.command() ) )
		 * {
		 * PayloadValue payload0 = packet.getPayload();
		 * 
		 * if ( ! ( payload0 instanceof PacketPayload ) )
		 * return false;
		 * 
		 * PacketPayload payload = ( PacketPayload ) payload0;
		 * byte[] arg0 = payload.getValue( "*0" ).getBytes();
		 * byte[] arg1 = payload.getValue( "*1" ).getBytes();
		 * 
		 * Packet resp = new Packet( new byte[] {0x65}, packet.packetId() );
		 * Map<String, Object> value = Maps.newLinkedHashMap();
		 * 
		 * if ( arg0.length == 0 && arg1.length == 0 )
		 * {
		 * Map<String, Object> child = Maps.newLinkedHashMap();
		 * 
		 * child.put( "defaultAuthenticationMethod", "March" );
		 * child.put( "warnInactive", true );
		 * 
		 * value.put( "loginParams", child );
		 * value.put( "*0", ( byte ) 0x0a );
		 * }
		 * else if ( arg0.length > 0 && arg1.length == 0 )
		 * {
		 * // XXX Check the authenticator for provided username. Probably do nothing since I don't see us supporting anything other.
		 * // Possible auth methods include clear text, March and SSPI
		 * value.put( "*0", "authenticator" );
		 * value.put( "*1", "March" );
		 * }
		 * else if ( arg0.length > 0 && arg1.length > 0 )
		 * if ( payload.getValue( "*2" ) == null )
		 * {
		 * System.out.println( "Got Username: " + new String( arg0 ) );
		 * 
		 * resp.setPayload( "success" );
		 * value = null;
		 * }
		 * else
		 * {
		 * byte[] arg2 = payload.getValue( "*2" ).getBytes();
		 * 
		 * System.out.println( "Got Username: " + new String( arg0 ) );
		 * 
		 * try
		 * {
		 * value.put( "*0", "login" );
		 * value.put( "*1", Hex.decodeHex( "95dde11f47e8603f1fe619b2313ce3c9e743cf84".toCharArray() ) );
		 * value.put( "*2", Hex.decodeHex( "0af9f97266d1f031dd01f4e7de0b0c08".toCharArray() ) );
		 * }
		 * catch ( DecoderException e )
		 * {
		 * 
		 * }
		 * }
		 * 
		 * if ( value != null )
		 * resp.setPayload( value );
		 * 
		 * ByteBuf encoded = resp.encode();
		 * 
		 * System.out.println( "Successfully Sent Packet: " + resp );
		 * 
		 * stream.write( encoded );
		 * 
		 * return true;
		 * }
		 * return false;
		 * }
		 * } );
		 */
	}
	
	@Override
	protected void badCommand( byte[] packetId )
	{
		Map<String, Object> data = Maps.newHashMap();
		data.put( "*0", "badCommand" );
		bus.sendPacket( new Packet( new byte[] {0x65} ).addPacketId( packetId ).setPayload( data ) );
	}
	
	public NetClient client()
	{
		return client;
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
		System.out.println( "Server Start Indicated" );
		
		/*
		 * Map<String, Object> data = Maps.newHashMap();
		 * 
		 * try
		 * {
		 * data.put( "*0", Hex.decodeHex( "47533130333054383030".toCharArray() ) );
		 * data.put( "*1", Hex.decodeHex( "fa09fc3551a7dbb4c9939a95fa9d82c83d248f8d".toCharArray() ) );
		 * }
		 * catch ( DecoderException e )
		 * {
		 * e.printStackTrace();
		 * }
		 * 
		 * stream.write( new Packet( "SGIA" ).addPacketId().setPayload( data ).encode() );
		 * 
		 * stream.flush();
		 */
	}
}
