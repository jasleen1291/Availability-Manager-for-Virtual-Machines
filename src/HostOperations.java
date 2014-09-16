import java.security.MessageDigest;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.vmware.vim25.ComputeResourceConfigSpec;
import com.vmware.vim25.HostConnectSpec;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.Task;

public class HostOperations {
	public static boolean reconnectHost(HostSystem host,int port, Folder hostFolder,String username, String password)
	{
		
		HostConnectSpec spec = new HostConnectSpec();
		spec.setHostName(host.getName());
		spec.setUserName(username);
		spec.setPassword(password);
		String sslThumbprint=null;
		try {
			sslThumbprint = getSSLCertForHost(host.getName(), port);
		} catch (Exception e) {
			System.out.println("Could not retrieve SSL certificate. Now Exiting");
			System.exit(0);
			e.printStackTrace();
		}


		spec.setSslThumbprint(sslThumbprint);
	//	ComputeResourceConfigSpec compResSpec = new ComputeResourceConfigSpec();
		Task task = null;
		String result=null;
		try
		{
		task=		host.reconnectHost_Task(spec);
		result = task.waitForMe();
		}
		catch(Exception e)
		{
			System.out.println("The given host cannnot be connected");
			
		}
		
		if (result == Task.SUCCESS) {
			System.out.println("Host Connected Successfully");
			return true;
		} else {
			System.out.println("Host Could not be connected");
			return false;
		}


	}
public static void removeHost(HostSystem host) throws Exception
{
	try
	{
	Task task=host.disconnectHost();
	task.waitForMe();
	}
	catch(Exception r)
	{}
	Task task2=host.getParent().destroy_Task();
	task2.waitForMe();
}
	public static boolean addHost(String host,int port, Folder hostFolder,String username, String password) throws Exception
	{
		
		HostConnectSpec spec = new HostConnectSpec();
		spec.setHostName(host);
		spec.setUserName(username);
		spec.setPassword(password);
		String sslThumbprint=null;
		try {
			sslThumbprint = getSSLCertForHost(host, port);
		} catch (Exception e) {
			System.out.println("Could not retrieve SSL certificate. Now Exiting");
			System.exit(0);
			e.printStackTrace();
		}


		spec.setSslThumbprint(sslThumbprint);
		ComputeResourceConfigSpec compResSpec = new ComputeResourceConfigSpec();
		Task task = null;
		String result=null;
		
		task=		hostFolder.addStandaloneHost_Task(spec, compResSpec, true);
		result = task.waitForMe();
		
		
		
		if (result == Task.SUCCESS) {
			System.out.println("Host Added Successfully");
			return true;
		} else {
			System.out.println("Host Could not be added");
			return false;
		}


	}

	private static String getSSLCertForHost(String host, int port) throws Exception {
		String sslThumbprint = null;
		TrustManager trm = new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs,
					String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs,
					String authType) {
			}
		};

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, new TrustManager[] { trm }, null);
		SSLSocketFactory factory = sc.getSocketFactory();
		SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
		socket.startHandshake();
		SSLSession session = socket.getSession();
		java.security.cert.Certificate[] servercerts = session
				.getPeerCertificates();
		for (int i = 0; i < servercerts.length; i++) {
			MessageDigest mDigest = MessageDigest.getInstance("SHA1");
			byte[] result = mDigest.digest(servercerts[i].getEncoded());
			StringBuffer sb = new StringBuffer();
			for (int j = 0; j < result.length; j++) {

				sb.append(Integer.toString((result[j] & 0xff) + 0x100, 16)
						.substring(1));
				if (j != result.length - 1)
					sb.append(":");

			}
			sslThumbprint = sb.toString();
			sb.substring(sb.lastIndexOf(":") - 1);
			sslThumbprint = sb.toString();

		}
		socket.close();
		return sslThumbprint;
	}
	
}
