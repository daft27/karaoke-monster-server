package km.comm;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

public class Lounge {
	public static Lounge Instance = new Lounge();
	private final Vector<User> users;
	private final Playlist playlist = new Playlist();
	private String stagename = "";
	private Stage stage;
	
	private Map<String, Long> expiredUsers = new HashMap<String, Long>();
	
	public Lounge()
	{
		users = new Vector<User>();
		stage = new Stage();
	}
	
	public boolean hasUser(String name)
	{
		Iterator<User> i = users.iterator();
		while(i.hasNext())
		{
			User other = i.next();
			if(other.name.equals(name))
				return true;
		}
		return false;
	}
	
	public void stageReq(Entry e) 
	{
		stage.addEntry(e);
	}
	
	public void expirePlaylists() throws IOException
	{
		// expire users from current playlist
		long currTime = System.currentTimeMillis();
		Iterator<Map.Entry<String, Long>> ei = expiredUsers.entrySet().iterator();
		while(ei.hasNext())
		{
			Map.Entry<String, Long> pairs = ei.next();
			if(currTime > pairs.getValue())
			{
				String name = pairs.getKey();
				if(!hasUser(name))
				{
					playlist.removeUser(name);
					clearUser(name);
				}
				
				ei.remove();
			}
		}	
	}
	
	public void sendPlaylist(User u)  throws IOException
	{	
		expirePlaylists();
		
		Vector<Song> songs = playlist.getSongs();

		// talk to others
		Iterator<Song> i = songs.iterator();
		while(i.hasNext())
		{
			Song s = i.next();
			try
			{
				u.addSong(s);
			}
			catch(IOException e)
			{
				u.disconnect();
				break;
			}			
		}
		
		// empty em out
		removeDisconnectedUsers();				
}
	
	public void addSong(User u, Song s) throws IOException
	{
		if(!stagename.equals("") && !stagename.equals(u.name))
		{
			return;
		}
		
		playlist.enqueueSong(s);
		
		// talk to others
		Iterator<User> i = users.iterator();
		while(i.hasNext())
		{
			User other = i.next();
			try
			{
				other.addSong(s);
			}
			catch(IOException e)
			{
				other.disconnect();
			}			
		}
		
		// empty em out
		removeDisconnectedUsers();				
	}
	
	public void endRecording(User u) throws IOException
	{
		playlist.endRecording(u.name);
		
		// talk to others
		Iterator<User> i = users.iterator();
		while(i.hasNext())
		{
			User other = i.next();
			try
			{
				other.notifyUserRecEnded(u);
			}
			catch(IOException e)
			{
				other.disconnect();
			}			
		}
		
		// empty em out
		removeDisconnectedUsers();			
	}

	public void stopAll() throws IOException
	{
		// talk to others
		Iterator<User> i = users.iterator();
		while(i.hasNext())
		{
			User other = i.next();
			try
			{
				other.stopMsg();
			}
			catch(IOException e)
			{
				other.disconnect();
			}			
		}
		
		// empty em out
		removeDisconnectedUsers();	
	}
	
	public void forceUpgrade() throws IOException
	{
		// talk to others
		Iterator<User> i = users.iterator();
		while(i.hasNext())
		{
			User other = i.next();
			try
			{
				other.forceUpgrade();
			}
			catch(IOException e)
			{
				other.disconnect();
			}			
		}
		
		// empty em out
		removeDisconnectedUsers();	
	}

	public void stage(String name) throws IOException
	{
		stagename = name;
		System.out.println("STAGE: " + name);
		
		// talk to others
		Iterator<User> i = users.iterator();
		while(i.hasNext())
		{
			User other = i.next();
			try
			{
				other.stage(name);
			}
			catch(IOException e)
			{
				other.disconnect();
			}			
		}
		
		// empty em out
		removeDisconnectedUsers();		
	}
	
	public void actionAll(User u, String msg) throws IOException
	{
		// talk to others
		Iterator<User> i = users.iterator();
		while(i.hasNext())
		{
			User other = i.next();
			try
			{
				other.chatAction(u, msg);
			}
			catch(IOException e)
			{
				other.disconnect();
			}			
		}
		
		// empty em out
		removeDisconnectedUsers();			
	}
	
	public void msgAll(User u, String msg) throws IOException
	{
		if(msg.startsWith("/2stage2 "))
		{
			stage(msg.substring("/2stage2 ".length()));
			return;
		}
		else if(msg.startsWith("/2stopall2"))
		{
			stopAll();
			return;
		}
		else if(msg.startsWith("/2forceupgrade2"))
		{
			forceUpgrade();
			return;
		}
		else if(msg.startsWith("/me "))
		{
			actionAll(u, msg);
			return;
		}
		else if(msg.startsWith("/"))
		{
			return;
		}
		
		// talk to others
		Iterator<User> i = users.iterator();
		while(i.hasNext())
		{
			User other = i.next();
			try
			{
				other.chatMsg(u, msg);
			}
			catch(IOException e)
			{
				other.disconnect();
			}			
		}
		
		// empty em out
		removeDisconnectedUsers();		
	}
		
	public void addUser(User u) throws IOException
	{
		// alert others
		Iterator<User> i = users.iterator();
		while(i.hasNext())
		{
			User other = i.next();
			try
			{
				other.notifyUserJoined(u, 1);
			}
			catch(IOException e)
			{
				other.disconnect();
			}			
		}		

		// empty em out
		removeDisconnectedUsers();


		try
		{
			// update the list, update the player
			i = users.iterator();
			while(i.hasNext())
			{
				User other = i.next();
				u.notifyUserJoined(other, 0);			
			}
			u.notifyUserJoined(u, 1);
		}
		catch(IOException e)
		{
			u.disconnect();
		}

		users.add(u);
		writeUsersToFile();

		sendPlaylist(u);
	}
	
	private void writeUsersToFile()
	{
		try
		{
			FileWriter fw = new FileWriter("usersonline.txt");
			for(int i = users.size() - 1; i >= 0; i--)
			{
				User u = users.get(i);
				fw.write(u.name + "\n");
			}
			fw.close();
		}
		catch(IOException e)
		{
			System.err.println("Unable to write users to file: " + e.getMessage());
		}
	}
	
	private void removeDisconnectedUsers() throws IOException
	{
		for(int i = users.size() - 1; i >= 0; i--)
		{
			if(i > (users.size() - 1) )
				continue;
			
			try
			{
				User other = users.get(i);
				if(! other.isConnected())
				{
					removeUser(other);
				}
			}
			catch(Exception e)
			{
				System.err.println("Lounge.removeDisconnectedUsers");
				e.printStackTrace();
			}
		}
		
	}
	
	public void clearUser(String name) throws IOException
	{
		// remove the user's data from their system
		Iterator<User> i = users.iterator();
		while(i.hasNext())
		{
			User other = i.next();
			try
			{
				other.notifyUserCleared(name);
			}
			catch(IOException e)
			{
				other.disconnect();
			}
		}
		
		// empty em out
		removeDisconnectedUsers();
		
	}
	
	public void removeUser(User u) throws IOException
	{
		// remove from my online list
		users.remove(u);
		playlist.endRecording(u.name);
		
		// begin playlist expiry process
		expiredUsers.put(u.name, System.currentTimeMillis() + 300000);
		
		// let other people know that the user has left the room
		Iterator<User> i = users.iterator();
		while(i.hasNext())
		{
			User other = i.next();
			try
			{
				other.notifyUserLeft(u, 1);
			}
			catch(IOException e)
			{
				other.disconnect();
			}
		}
		
		// empty em out
		removeDisconnectedUsers();
		
		writeUsersToFile();		
	}
}
