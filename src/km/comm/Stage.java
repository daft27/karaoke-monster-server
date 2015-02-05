package km.comm;

import java.util.Iterator;
import java.util.Vector;

public class Stage {

	private Vector<Entry> entries = new Vector<Entry>();
	public Stage()
	{
		
	}
	
	public Vector<Entry> getSongs()
	{
		return new Vector<Entry>(entries);
	}
	
	public void addEntry(Entry e)
	{
		boolean found = false;
		Iterator<Entry> i = entries.iterator();
		
		while(i.hasNext())
		{
			Entry t = i.next();
			if(t.owner.equals(e.owner))
			{
				t.name = e.name;
				t.source = e.source;
				found = true;
			}
		}		
		if(!found)
			entries.add(e);
	}
		
	public void removeEntry(String name)
	{
		Iterator<Entry> i = entries.iterator();
		
		while(i.hasNext())
		{
			Entry s = i.next();
			if(s.owner.equals(name))
				i.remove();
		}
	}

}
