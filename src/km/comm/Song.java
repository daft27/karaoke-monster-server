package km.comm;


public class Song {
	final String owner;
	final String title;
	final String artist;
	final String source; 
	float pitch;
	
	int length = 0;
	int live = 0; 
	
	public Song(String ownerIn, String artistIn, String titleIn, String sourceIn, float pitchIn, int lengthIn, int liveIn)
	{
		owner = ownerIn;
		artist = artistIn;
		title = titleIn;
		source = sourceIn;
		pitch = pitchIn;
		length = lengthIn;
		live = liveIn;
	}
	
	public String toString()
	{
		 //XMLInputFactory factory = (XMLInputFactory) XMLInputFactory.newInstance();
		 //xsr = (XMLStreamReader) factory.createXMLStreamReader(new ByteArrayInputStream(mess.getBytes()));
		return XMLHandler.createMsg(new String[] {"song", "owner", owner, "title", title, "artist", artist, "source", source, "pitch", Float.toString(pitch), "length", Integer.toString(length), "live", Integer.toString(live)});

		//return  "<song owner=\"" + owner + "\" title=\"" + title + "\" artist=\"" + artist + "\" source=\"" + source + "\" pitch=\"" + pitch + "\" live=\"" + live + "\" />";
	}
}
