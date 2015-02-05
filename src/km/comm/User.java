package km.comm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class User {
	final ChannelCallback channelc; 
	int group = 0;
	String name = "";
	boolean isConnected = true;
	
	public User(ChannelCallback c)
	{
		channelc = c;
	}
	
	
	public void disconnect()
	{
		channelc.close();
		isConnected = false;
	}
	
	public boolean isConnected()
	{
		return isConnected;
	}
	
	public void readInput() throws IOException, InterruptedException
	{
		List<String> command = channelc.readMessage();
		while(command.size() > 0)
		{
			if(command.get(0).equals("enter"))
			{
				for(int i = 1; i < command.size(); i+= 2)
				{
					if(command.get(i).equals("name"))
						name = command.get(i + 1);
				}
				System.out.println("New user " + name + " entered!");
				Lounge.Instance.addUser(this);
			}
			else if(command.get(0).equals("msg"))
			{
				String sendname = "";
				String sendmsg = "";
				for(int i = 1; i < command.size(); i+= 2)
				{
					if(command.get(i).equals("name"))
						sendname = command.get(i + 1);
					else if(command.get(i).equals("CHARS"))
						sendmsg = command.get(i+1);
				}
				Lounge.Instance.msgAll(this, sendmsg);
			}
			else if(command.get(0).equals("music"))
			{
				String artist = "";
				String title = "";
				String source = "";
				float pitch = 0;
				int live = 0;
				int length = 0;
				for(int i = 1; i < command.size(); i+= 2)
				{
					if(command.get(i).equals("artist"))
						artist = command.get(i + 1);
					else if(command.get(i).equals("title"))
						title = command.get(i+1);
					else if(command.get(i).equals("source"))
						source = command.get(i+1);
					else if(command.get(i).equals("pitch"))
					{
						try
						{
							pitch = Float.valueOf(command.get(i+1));
						}
						catch(NumberFormatException nfe)
						{
							System.err.println("Failed to convert pitch " + nfe.getMessage());
						}
					}
					else if(command.get(i).equals("live"))
					{
						try
						{
							live = Integer.valueOf(command.get(i+1));
						}
						catch(NumberFormatException nfe)
						{
							System.err.println("Failed to convert live " + nfe.getMessage());
						}
					}
					else if(command.get(i).equals("length"))
					{
						try
						{
							length = Integer.valueOf(command.get(i+1));
						}
						catch(NumberFormatException nfe)
						{
							System.err.println("Failed to convert length " + nfe.getMessage());
						}
					}
				}
				Lounge.Instance.addSong(this, new Song(name, artist, title, source, pitch, length, live));
			}
			else if(command.get(0).equals("recend"))
			{
				Lounge.Instance.endRecording(this);
			}
			else if(command.get(0).equals("stopall"))
			{
				Lounge.Instance.stopAll();
			}
			else if(command.get(0).equals("stage"))
			{
				String stagename = "";
				for(int i = 1; i < command.size(); i+= 2)
				{
					if(command.get(i).equals("name"))
						stagename = command.get(i + 1);
				}
				Lounge.Instance.stage(name);
			}
			else if(command.get(0).equals("stagereq"))
			{
				String source = "";
				String song = "";
				for(int i = 1; i < command.size(); i+= 2)
				{
					if(command.get(i).equals("source"))
						source = command.get(i + 1);
					else if(command.get(i).equals("song"))
						song = command.get(i + 1);
				}
				Lounge.Instance.stageReq(new Entry(name, source, song));
			}
			else if(command.get(0).equals("ping"))
			{
				Lounge.Instance.expirePlaylists();
			}
			
			command = channelc.readMessage();
		}
	}
	
	private String getWelcomeMessage()
	{
		String message = "";
		try
		{
			FileReader fr = new FileReader("welcome.txt");
			BufferedReader br = new BufferedReader(fr);
			String s;
			while((s = br.readLine()) != null)
			{
				message += s + "\n";
			}
			fr.close();
		}
		catch(IOException e)
		{
			System.err.println("Unable to read welcome message: " + e.getMessage());
			message ="Welcome to Fansub.TV's Karaoke Booth.\n" +
			"Please enjoy your stay.  To order drinks, please dial #9.\n\n" +
			"http://karaokemonster.lighthouseapp.com/\n"; 
		}
		
		return message;
	}

	
	public void welcomeUser() throws IOException
	{
		String msg = "\ufeff<?xml version=\"1.0\" encoding=\"UTF-16\"?>\n<karaoke><welcome><![CDATA[" +
		getWelcomeMessage() +
//"Welcome to Fansub.TV's Karaoke Booth.\n" +
//"Please enjoy your stay.  To order drinks, please dial #9.\n" +
"]]></welcome>";
		channelc.writeMessage(msg);
	}
	
	public void stopMsg() throws IOException
	{
		String msg = XMLHandler.createMsg(new String[]{"stop"});
		channelc.writeMessage(msg);
	}
	
	public void forceUpgrade() throws IOException
	{
		String msg = XMLHandler.createMsg(new String[]{"forceupgrade"});
		channelc.writeMessage(msg);
	}
	
	public void stage(String stagename) throws IOException
	{
		String msg = XMLHandler.createMsg(new String[]{"stage", "name", stagename});
		channelc.writeMessage(msg);		
	}

	public void notifyUserJoined(User other, int alert) throws IOException
	{
		//String msg = "<join name=\"" + other.name + "\"	 group=\"" + other.group + "\" alert=\"" + alert + "\" />";
		String msg = XMLHandler.createMsg(new String[]{"join", "name", other.name, "group", Integer.toString(other.group), "alert", Integer.toString(alert)});
		channelc.writeMessage(msg);
	}
	
	public void notifyUserLeft(User other, int alert) throws IOException 
	{
		//String msg = "<left name=\"" + other.name + "\"	 alert=\"" + alert + "\" />";
		String msg = XMLHandler.createMsg(new String[]{"left", "name", other.name, "alert", Integer.toString(alert)});
		channelc.writeMessage(msg);		
	}
	
	public void notifyUserCleared(String name) throws IOException
	{
		String msg = XMLHandler.createMsg(new String[]{"clear", "name", name});
		channelc.writeMessage(msg);				
	}
	
	public void chatMsg(User other, String content) throws IOException
	{
		//String msg = "<msg name=\"" + other.name + "\"><![CDATA[" + content.replaceAll("]]", "") + "]]></msg>";
		String msg = XMLHandler.createMsg(new String[]{"msg", "name", other.name, content});
		channelc.writeMessage(msg);				
	}
	
	public void chatAction(User other, String content) throws IOException
	{
		String msg = XMLHandler.createMsg(new String[]{"action", "name", other.name, content});
		channelc.writeMessage(msg);						
	}
	
	public void addSong(Song s) throws IOException
	{
		String msg = s.toString();
		channelc.writeMessage(msg);						
	}
	
	public void notifyUserRecEnded(User other) throws IOException
	{
		//String msg = "<recend name=\"" + other.name + "\" />";
		String msg = XMLHandler.createMsg(new String[]{"recend", "name", other.name});
		channelc.writeMessage(msg);						
	}
	
}
