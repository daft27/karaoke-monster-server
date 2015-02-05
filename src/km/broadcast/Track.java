package km.broadcast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Track {

	private SocketChannel channel;
	private FileOutputStream fos = null;
	private byte[] headerBuff = new byte[1024];
	private int totreadlen = 0;
	
	private String user = "";
	private String source = "";
	private boolean musicTrack = false;
	private String mode = "";
	
	public Track(SocketChannel channel)
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
	
	public void close()
	{
		if(fos != null)
		{
			try{
					fos.close();
					channel.close();
					System.out.println("Closing out on " + user + "/" + source + "(" + mode + ")" );
			}
			catch(IOException e)
			{
				
			}
		}
	}
	public void parse(String str)
	{
		String [] sa = str.split("\n");
		if(sa.length == 0)
			return;
		
		if(!sa[0].equals("Karaoke Monster BROADCAST Stream")){
			System.out.println("Header doesn't match. " + sa[0]);
			return;
		}
		
		for(int i = 1 ; i < sa.length; i +=2)
		{
			if(sa[i].equals("User"))
				user = sa[i + 1];
			else if(sa[i].equals("Source"))
				source = sa[i+1];			
			else if(sa[i].equals("Type"))
				mode = sa[i+1];			
		}
		
	}
	
	private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
        	int halfbyte = (data[i] >>> 4) & 0x0F;
        	int two_halfs = 0;
        	do {
	        	if ((0 <= halfbyte) && (halfbyte <= 9))
	                buf.append((char) ('0' + halfbyte));
	            else
	            	buf.append((char) ('a' + (halfbyte - 10)));
	        	halfbyte = data[i] & 0x0F;
        	} while(two_halfs++ < 1);
        }
        return buf.toString();
    }
 
	public static String MD5(String text) 
	throws NoSuchAlgorithmException  {
		MessageDigest md;
		md = MessageDigest.getInstance("MD5");
		byte[] md5hash = new byte[32];
		md.update(text.getBytes(), 0, text.length());
		md5hash = md.digest();
		return convertToHex(md5hash);
	}

	static final int BUFSIZE = 4096;

	public void readMessage() throws IOException, InterruptedException 
	{
		ByteBuffer byteBuffer = ByteBuffer.allocate( BUFSIZE );
		int nbytes = channel.read( byteBuffer );
		
		if(nbytes > 0)
		{
			byteBuffer.flip();
			int blocksize = 0;
			if(totreadlen < 1024)
			{
				blocksize = nbytes - totreadlen;
				if(totreadlen + blocksize > 1024)
					blocksize = 1024 - totreadlen;
				
				System.arraycopy(byteBuffer.array(), totreadlen, headerBuff, totreadlen, blocksize);
				
				if((totreadlen + blocksize) == 1024)
				{
					String result = decode(ByteBuffer.wrap(headerBuff));
					parse(result);
					System.out.println(result);
					
					
					try{					
					    String fname = MD5(source);
					    String uname = MD5(user);
					    
						fos = new FileOutputStream(uname + " - " + mode + fname + ".mp3");
						System.out.println("Hashed to " + uname + " - " + mode + fname + ".mp3");
					}
					catch(NoSuchAlgorithmException e)
					{
						e.printStackTrace();
					}
					catch(FileNotFoundException e)
					{
						e.printStackTrace();
					}

				}
			}
			
			totreadlen += nbytes;
			
			try 
			{
				fos.write(byteBuffer.array(), 0 + blocksize, nbytes - blocksize);
			}
			catch(FileNotFoundException e)
			{
				System.err.println("FNF " + e.getMessage());
			}
		}
		else
		{
			close();
		}
	}

}
