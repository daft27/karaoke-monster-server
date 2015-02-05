package km.relay;

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

public class RelayServer {

	int port = 41844;
	Selector selector = null;
	ServerSocketChannel selectableChannel = null;
	int keysAdded = 0;

	public RelayServer() 
	{
	}

	public RelayServer( int port ) 
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

		System.out.println("Relay acceptor loop..." );
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
					SelectionKey readKey = channel.register( selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
					Playback t = new Playback(channel);
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
						((Playback)  key.attachment()).readMessage( );
						//((User) key.attachment()).readInput();
					}
					catch(IOException e)
					{
						System.out.println("Closed read " + e.getMessage());
						key.cancel();
						((Playback) key.attachment()).close();
						//Lounge.Instance.removeUser((User) key.attachment());
					}
				}
				
				else if ( key.isWritable() ) 
				{
					
					try
					{
						((Playback) key.attachment()).writeOut();
					}
					catch(IOException e)
					{
						System.out.println("Closed on write");
						key.cancel();
						((Playback) key.attachment()).close();
					}
				}
				
			}
			Thread.sleep(400);
		}

		System.out.println("End acceptor loop..." );

	}


	public static void main( String[] args ) 
	{

		RelayServer nbServer = new RelayServer();

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
