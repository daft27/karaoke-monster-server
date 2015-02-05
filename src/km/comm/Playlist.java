package km.comm;

import java.util.Iterator;
import java.util.Vector;

public class Playlist {
	private Vector<Song> songs = new Vector<Song>();
	public Playlist()
	{
		
	}
	
	public Vector<Song> getSongs()
	{
		return new Vector<Song>(songs);
	}
	
	public void enqueueSong(Song s)
	{
		boolean found = false;
		Iterator<Song> i = songs.iterator();
		
		while(i.hasNext())
		{
			Song t = i.next();
			if(t.owner.equals(s.owner) && t.source.equals(s.source))
			{
				t.pitch = s.pitch;
				t.live = s.live;
				found = true;
			}
		}		
		if(!found)
			songs.add(s);
	}
	
	public void endRecording(String name)
	{
		Iterator<Song> i = songs.iterator();
		
		while(i.hasNext())
		{
			Song s = i.next();
			if(s.owner.equals(name))
				s.live = 0;
		}		
	}
	
	public void removeUser(String name)
	{
		Iterator<Song> i = songs.iterator();
		
		while(i.hasNext())
		{
			Song s = i.next();
			if(s.owner.equals(name))
				i.remove();
		}
	}
}
