import java.io.BufferedReader;
import java.io.InputStreamReader;


public class PingExample {
	public static String ping(String command) {
		//System.out.println("Pinging "+command);
		if(command.indexOf(":")==-1)
		{
		try {
			String osname=System.getProperty("os.name").toLowerCase();
			if(osname.indexOf("win") >= 0)
			{
			Process p = Runtime.getRuntime().exec("ping -n 10 " + command);
			BufferedReader inputStream = new BufferedReader(
					new InputStreamReader(p.getInputStream()));

			String s = "";
			// reading output stream of the command
			while ((s = inputStream.readLine()) != null) {
				//System.out.println(s);
				if (s.indexOf("% loss") != -1)
					return (s.substring(s.indexOf('(') + 1, s.indexOf('%')));
			}
			}
			else if(osname.indexOf("nix") >= 0 || osname.indexOf("nux") >= 0 || osname.indexOf("aix") > 0 )
			{
				Process p = Runtime.getRuntime().exec("ping -c 10 " + command);
				BufferedReader inputStream = new BufferedReader(
						new InputStreamReader(p.getInputStream()));

				String s = "";
				// reading output stream of the command
				while ((s = inputStream.readLine()) != null) {
					
					if (s.indexOf("% packet loss") != -1)
						{//System.out.println(s);
						if(s.indexOf("errors,")!=-1)
						{
							
							return (s.substring(s.indexOf(", ",s.indexOf("s, ")+1)+2, s.indexOf('%')));
						}
						else
							return (s.substring(s.indexOf(", ",s.indexOf(", ")+1)+2, s.indexOf('%')));
						}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		}
		else
		{
			////System.out.println(1);
			try {
				String osname=System.getProperty("os.name").toLowerCase();
				if(osname.indexOf("win") >= 0)
				{
					//System.out.println("Windows");
				Process p = Runtime.getRuntime().exec("ping -6 -n 10 " + command);
				BufferedReader inputStream = new BufferedReader(
						new InputStreamReader(p.getInputStream()));

				String s = "";
				// reading output stream of the command
				while ((s = inputStream.readLine()) != null) {
				//	//System.out.println(s);
					if (s.indexOf("% loss") != -1)
						return (s.substring(s.indexOf('(') + 1, s.indexOf('%')));
				}
				}
				else if(osname.indexOf("nix") >= 0 || osname.indexOf("nux") >= 0 || osname.indexOf("aix") > 0 )
				{
					////System.out.println("Ubuntu");
					Process p = Runtime.getRuntime().exec("ping6 -I wlan0 -c 10 " + command);
					////System.out.println("ping6 -I wlan0  -c 10 " + command);
					BufferedReader inputStream = new BufferedReader(
							new InputStreamReader(p.getInputStream()));

					String s = "";
					// reading output stream of the command
					while ((s = inputStream.readLine()) != null) {
						////System.out.println(s);
						if (s.indexOf("% packet loss") != -1)
							{////System.out.println(s);
							if(s.indexOf("errors,")!=-1)
							{
								
								return (s.substring(s.indexOf(", ",s.indexOf("s, ")+1)+2, s.indexOf('%')));
							}
							else
								return (s.substring(s.indexOf(", ",s.indexOf(", ")+1)+2, s.indexOf('%')));
							}
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return "";
	}
	public static void main(String args[])
	{
		System.out.println(ping("fe80::86a6:c8ff:fe46:418c"));
	}
}
