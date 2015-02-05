package km.comm;

import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class XMLHandler {

	static String createMsg(String[] str)
	{
		StringWriter sw = new StringWriter();
		try
		{
			XMLOutputFactory factory = (XMLOutputFactory) XMLOutputFactory.newInstance();		
			XMLStreamWriter xsw = (XMLStreamWriter) factory.createXMLStreamWriter(sw);
			
			xsw.writeStartElement(str[0]);
			for(int i = 1; i < str.length - 1; i+= 2)
			{
				xsw.writeAttribute(str[i], str[i + 1]);
			}
			
			if(str.length % 2 == 0)
				xsw.writeCData(str[str.length - 1]);
			
			xsw.writeEndElement();
		}
		catch(XMLStreamException xse)
		{
			
		}

		return sw.toString();
	}
}
