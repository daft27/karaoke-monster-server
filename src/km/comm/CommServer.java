package km.comm;

import java.io.*;
import java.math.BigInteger;
import java.nio.channels.*;
import java.nio.channels.spi.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;

public class CommServer {

	int port = 41842;
	Selector selector = null;
	ServerSocketChannel selectableChannel = null;
	int keysAdded = 0;

	public CommServer() 
	{
	}

	public CommServer( int port ) 
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

		System.out.println("Comm mcceptor loop..." );
		while (( this.keysAdded = acceptKey.selector().select()) > 0 ) 
		{

			//System.out.println("Selector returned " + this.keysAdded + " ready for IO operations" );

			Set<SelectionKey> readyKeys = this.selector.selectedKeys();
			Iterator<SelectionKey> i = readyKeys.iterator();

			while (i.hasNext()) 
			{
				SelectionKey key = (SelectionKey)i.next();
				i.remove();

				try
				{
					if(!key.isValid())
					{
//						key.cancel();
						User u = ((User) key.attachment());
						if(u != null)
							u.disconnect();
						
						continue;
					}
					
					if ( key.isAcceptable() ) 
					{
						ServerSocketChannel nextReady = (ServerSocketChannel)key.channel();
	
						SocketChannel channel = nextReady.accept();
						channel.socket().setSendBufferSize(16384);
						channel.configureBlocking( false );
						SelectionKey readKey = channel.register( selector, SelectionKey.OP_READ );
						User u = new User(new ChannelCallback(channel));
						readKey.attach(u);
						
						try
						{
							u.welcomeUser();
						}
						catch(IOException e)
						{
							System.out.println("Closed accept");
							key.cancel();
							u.disconnect();
						}
					}
					else if ( key.isReadable() ) 
					{
						//SelectableChannel nextReady = (SelectableChannel) key.channel();
						//System.out.println("Processing selection key read=" + key.isReadable() + " write=" + key.isWritable() + " accept=" + key.isAcceptable() );
						try
						{
							//((ChannelCallback)  key.attachment()).readMessage( );
							((User) key.attachment()).readInput();
						}
						catch(IOException e)
						{
							System.out.println("Closed read");
							key.cancel();
							Lounge.Instance.removeUser((User) key.attachment());
							((User) key.attachment()).disconnect();
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
				catch(Exception e)				
				{
					System.err.println("CommServer.loop");
					e.printStackTrace();
				}
			}
		}

		System.out.println("End acceptor loop..." );

	}


	public static void main( String[] args ) 
	{

		CommServer nbServer = new CommServer();

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
