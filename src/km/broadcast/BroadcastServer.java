package km.broadcast;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.*;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.net.*;
import java.util.*;

public class BroadcastServer {

	int port = 41843;
	Selector selector = null;
	ServerSocketChannel selectableChannel = null;
	int keysAdded = 0;

	public BroadcastServer() 
	{
	}

	public BroadcastServer( int port ) 
	{
		this.port = port;
	}

	public void initialize() throws IOException 
	{
		selector = SelectorProvider.provider().openSelector();
		selectableChannel = ServerSocketChannel.open();
		selectableChannel.configureBlocking(false);
		InetSocketAddress isa = new InetSocketAddress(this.port );
		selectableChannel.socket().bind(isa);
	}

	public void finalize() throws IOException 
	{
		selectableChannel.close();
		selector.close();
	}
	
	public void acceptConnections() throws IOException, InterruptedException 
	{

		SelectionKey acceptKey = this.selectableChannel.register( this.selector, SelectionKey.OP_ACCEPT );

		System.out.println("Broadcast acceptor loop..." );
		while (( this.keysAdded = acceptKey.selector().select()) > 0 ) 
		{

			//System.out.println("Selector returned " + this.keysAdded + " ready for IO operations" );

			Set<SelectionKey> readyKeys = this.selector.selectedKeys();
			Iterator<SelectionKey> i = readyKeys.iterator();

			while (i.hasNext()) 
			{
				SelectionKey key = (SelectionKey)i.next();
				i.remove();

				if ( key.isAcceptable() ) 
				{
					ServerSocketChannel nextReady = (ServerSocketChannel)key.channel();

					SocketChannel channel = nextReady.accept();
					channel.configureBlocking( false );
					SelectionKey readKey = channel.register( selector, SelectionKey.OP_READ );
					Track t = new Track(channel);
					readKey.attach(t);
					System.out.println("Accepted");
								
				}
				else if ( key.isReadable() ) 
				{
					//SelectableChannel nextReady = (SelectableChannel) key.channel();
					//System.out.println("Processing selection key read=" + key.isReadable() + " write=" + key.isWritable() + " accept=" + key.isAcceptable() );
					try
					{
						
						//readMessage((SocketChannel) key.attachment());
						((Track)  key.attachment()).readMessage( );
						//((User) key.attachment()).readInput();
					}
					catch(IOException e)
					{
						System.out.println("Closed read " + e.getMessage());
						((Track)  key.attachment()).close( );
						key.cancel();
						//Lounge.Instance.removeUser((User) key.attachment());
					}
				}
				/*
				else if ( key.isWritable() ) 
				{
					ChannelCallback callback = (ChannelCallback) key.attachment();
					String message = "What is your name? ";
					ByteBuffer buf = ByteBuffer.wrap( message.getBytes() );
					try
					{
						int nbytes = callback.getChannel().write( buf );
						System.out.println(nbytes  + " < > " + buf.remaining() + " / " + buf.limit());						
					}
					catch(IOException e)
					{
						System.out.println("Closed on write");
						key.cancel();
					}
				}
				*/
			}
			Thread.sleep(100);
		}

		System.out.println("End acceptor loop..." );

	}


	public static void main( String[] args ) 
	{

		BroadcastServer nbServer = new BroadcastServer();

		try 
		{
			nbServer.initialize();
		} 
		catch ( IOException e ) 
		{
			e.printStackTrace();
			System.exit( -1 );
		}

		try 
		{
			nbServer.acceptConnections();
		}
		catch ( IOException e ) 
		{
			e.printStackTrace();
			System.err.println( e );
		}
		catch ( InterruptedException e ) 
		{
			System.out.println( "Exiting normally..." );
		}

	}

}
