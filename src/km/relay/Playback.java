package km.relay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class Playback {

	private StringBuffer sbuffer = new StringBuffer(); 
	private SocketChannel channel;
	private FileInputStream fis = null;
	private byte[] headerBuff = new byte[1024];
	private boolean sentheaders = false;
	private int getmoredatas = 0;
	
	private ByteBuffer outgoing = null;
	
	private long lastDataTime = 0;
	
	private int datafailures = 0;
	
	public Playback(SocketChannel channel)
	{
		this.channel = channel;
	}
	
	static public String decode( ByteBuffer byteBuffer ) throws CharacterCodingException 
	{
		Charset charset = Charset.forName( "US-ASCII" );
		CharsetDecoder decoder = charset.newDecoder();
		CharBuffer charBuffer = decoder.decode( byteBuffer );
		String result = charBuffer.toString();
		return result;
	}
	
	static public ByteBuffer encode( String mess ) throws CharacterCodingException 
	{
		CharBuffer charBuffer = CharBuffer.wrap(mess.toCharArray());
		Charset charset = Charset.forName( "US-ASCII" );
		CharsetEncoder encoder = charset.newEncoder();
		
		return encoder.encode(charBuffer);
	}
	public void close()
	{
		if(fis != null)
		{
			try{
				fis.close();
				channel.close();
			}
			catch(IOException e)
			{
				
			}
			fis = null;
		}
	}
	
	public ByteBuffer getHeader()
	{
		String header = "HTTP/1.0 200 OK\r\n" +
				"Cache-Control: no-store, no-cache, must-revalidate, private, post-check=0, pre-check=0, max-age=0\r\n" +
				"Expires: Thu, 19 Nov 1981 08:52:00 GMT\r\n" +
				"Pragma: no-cache\r\n" +
				"Server: KaraokeMONSTERrelay\r\n" +
				"Connection: Close\r\n" +
				"\r\n";
		
		ByteBuffer out = null;
		try{
			out = encode(header);
		}
		catch(CharacterCodingException cce)
		{
			out = ByteBuffer.allocate(0);
		}
		return out;
	}
	
	public ByteBuffer getFailHeader()
	{
		String header = "HTTP/1.0 404 Not Found\r\n" +
				"Connection: Close\r\n" +
				"\r\n";
		
		ByteBuffer out = null;
		try{
			out = encode(header);
		}
		catch(CharacterCodingException cce)
		{
			out = ByteBuffer.allocate(0);
		}
		return out;
	}
	
	public void parse(String str)
	{
		String [] sa = str.split("\r\n");
		if(sa[0].endsWith("HTTP/1.1"))
		{
			try
			{
				String filename = new File(sa[0].substring(5, sa[0].length() - 9)).getName();
				filename = filename.replaceAll("%20", " ");
				System.out.println("Filename = " + filename);
				fis = new FileInputStream(filename);
			}
			catch(FileNotFoundException fnfe)
			{
				System.out.println("FNFE = " + fnfe.getMessage());
				
				// respond to failure
				ByteBuffer fourohfour = getFailHeader();
				System.out.println("Writing 404...");
				try
				{
					channel.write(fourohfour);
				}
				catch(IOException e)
				{
				
				}
				close();
			}
		}
		for(int i = 0; i < sa.length; i++)
		{
			System.out.println(i + ": " + sa[i]);
		}
		
	}
	
	static final int BUFSIZE = 4096;

	public void readMessage() throws IOException, InterruptedException 
	{
		ByteBuffer byteBuffer = ByteBuffer.allocate( BUFSIZE );
		int nbytes = channel.read( byteBuffer );
		
		if(nbytes > 0)
		{
			System.out.println("Buff = " + nbytes);
			byteBuffer.flip();
			String result = decode(byteBuffer);
			sbuffer.append(result);
			
			if(result.endsWith("\r\n\r\n"))
			{
				System.out.println("END HEAD");
				parse(sbuffer.toString());
				
				outgoing = getHeader();
			}
		}
	}

	public ByteBuffer getMoreData()
	{
		if(!sentheaders)
		{
			sentheaders = true;
			outgoing = ByteBuffer.allocate(8192);
		}
		

		long thisTime = System.currentTimeMillis();
		if(thisTime - lastDataTime < 100 )
			return outgoing;
	
		if(getmoredatas == 5)
			outgoing = ByteBuffer.allocate(4096);

		getmoredatas++;
		
		lastDataTime = thisTime;

		//System.out.println("getting more data");
		try
		{
			outgoing.clear();
			int rlen = fis.read(outgoing.array());
			if(rlen == -1)
			{
				System.out.println("fis ret -1");
				datafailures++;
				outgoing.flip();
//				outgoing = null;
//				return outgoing;
			}
			else if(rlen == 0)
			{
				System.out.println("fis ret 0");
				datafailures++;
			}
			else
			{
				datafailures = 0;
				outgoing.limit(rlen);
			}
			
			if(datafailures == 3)
			{
				System.out.println("ending due to data failure");
				outgoing = null;
				return outgoing;
			}
				
			//System.out.println("rlen = " + rlen);
		}
		catch(IOException e)
		{
			outgoing = null;
			System.out.println("IO Exception " + e.getMessage());
		}
		
		
		return outgoing;
	}
	
	public void writeOut() throws IOException, InterruptedException
	{
		
		if(fis == null || outgoing == null)
			return ;
		
		if(outgoing.remaining() == 0)
		{
			outgoing = getMoreData();
			if(outgoing == null)
			{
				fis.close();
				channel.close();
				return ;
			}
		}
		
		
		int nbytes = channel.write(outgoing);
		System.out.println("nbytes = " + nbytes + " / " + outgoing.remaining());
		if(outgoing.hasRemaining())
		{
			System.out.println("compacting");
			//outgoing.compact();
		}
		else
		{
			//System.out.println("clear remaining = " + outgoing.remaining() );
		} 
	
	}
}
