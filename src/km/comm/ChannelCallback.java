package km.comm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import java.util.Vector;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


public class ChannelCallback {
	private SocketChannel channel;
	private StringBuffer buffer;
	private XMLStreamReader xsr;
	
	public ChannelCallback( SocketChannel channel ) 
	{
		this.channel = channel;
		this.buffer = new StringBuffer();
	}
	
	void close()
	{
		try {
			channel.close();
		}
		catch(IOException e)
		{
			
		}
	}

	static public ByteBuffer encode( String mess ) throws CharacterCodingException 
	{
		CharBuffer charBuffer = CharBuffer.wrap(mess.toCharArray());
		Charset charset = Charset.forName( "UTF-16LE" );
		CharsetEncoder encoder = charset.newEncoder();
		
		return encoder.encode(charBuffer);
	}
	
	static public String decode( ByteBuffer byteBuffer ) throws CharacterCodingException 
	{
		Charset charset = Charset.forName( "UTF-16LE" );
		CharsetDecoder decoder = charset.newDecoder();
		CharBuffer charBuffer = decoder.decode( byteBuffer );
		String result = charBuffer.toString();
		return result;
	}
	
	static final int BUFSIZE = 2048;

	public List<String> readMessage() throws IOException, InterruptedException 
	{
		ByteBuffer byteBuffer = ByteBuffer.allocate( BUFSIZE );
		int nbytes = channel.read( byteBuffer );
		if(nbytes > 0)
		{
			byteBuffer.flip();
			String result = decode( byteBuffer );
			buffer.append( result.toString() );
		}
		else if(buffer.length() == 0)
			return new Vector<String>();
		
		String buff = buffer.toString();
		// If we are done with the line then we execute the callback.
		int eolpos = 0;
		if ( (eolpos = buff.indexOf("\r\n")) >= 0)
		{
			List<String> ret = parseXML(buff.substring(0, eolpos + 2));
			buffer.delete(0, eolpos + 2);
			return ret;
		}
		
		return new Vector<String>();
	}


	public void writeMessage( String message ) throws IOException 
	{
		ByteBuffer buf = encode(message);
		int n = channel.write(buf);
		//System.out.println("Wrote " + n);	
	}


	
	public List<String> parseXML(String mess) throws IOException 
	{
		System.out.println( mess );
//		writeMessage( mess );
//		buffer = new StringBuffer();
		try
		{
			System.out.println("parsing mess: " + mess);
			 XMLInputFactory factory = (XMLInputFactory) XMLInputFactory.newInstance();
			 xsr = (XMLStreamReader) factory.createXMLStreamReader(new ByteArrayInputStream(mess.getBytes("UTF-16")));
			//xsr = XMLStreamReaderFactory.create(new InputSource(mess), false);
			List<String> obj = new Vector<String>();
			while(xsr.hasNext())
			{
				switch(xsr.next())
				{
				case XMLStreamReader.START_ELEMENT:
					if(xsr.isEndElement())
						System.out.println("Standalone");
					System.out.println("Ele " + xsr.getLocalName() + " " + xsr.getAttributeCount());
					obj.add(xsr.getLocalName());
					for(int i = 0; i < xsr.getAttributeCount(); i++)
					{
						System.out.println(" >  " + xsr.getAttributeLocalName(i) + " = " + xsr.getAttributeValue(i));
						obj.add(xsr.getAttributeLocalName(i));
						obj.add(xsr.getAttributeValue(i));
					}
										
					break;
				case XMLStreamReader.END_ELEMENT:
					System.out.println("End ele" + xsr.getLocalName());
					return obj;
				case XMLStreamReader.CDATA:
				case XMLStreamReader.CHARACTERS:
					System.out.println("Chars " + xsr.getText());
					obj.add("CHARS");
					obj.add(xsr.getText());
					break;
				default:
					System.out.println("Something else in XML..." + xsr.getEventType());	
				}
			}
			
		}
		catch(XMLStreamException se)
		{
			System.err.println("XMLStreamException: " + se.getMessage());
		}
		return new Vector<String>();
	}

}
