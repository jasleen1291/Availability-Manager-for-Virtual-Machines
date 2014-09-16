import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import com.vmware.vim25.HttpNfcLeaseDeviceUrl;
import com.vmware.vim25.HttpNfcLeaseInfo;
import com.vmware.vim25.HttpNfcLeaseState;
import com.vmware.vim25.OvfCreateImportSpecParams;
import com.vmware.vim25.OvfCreateImportSpecResult;
import com.vmware.vim25.OvfFileItem;
import com.vmware.vim25.OvfNetworkMapping;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.HttpNfcLease;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
/**
 * Deploy VM or vApp from local disk to an ESX(i) server
 * http://vijava.sf.net
 * @author Steve Jin (sjin@vmware.com)
 */

/**
 * Deploy VM or vApp from local disk to an ESX(i) server
 * http://vijava.sf.net
 * @author Steve Jin (sjin@vmware.com)
 */

public class ImportLocalOvfVApp 
{
	private static final int CHUCK_LEN = 64 * 1024;
	
	public static LeaseProgressUpdater leaseUpdater;

	public ImportLocalOvfVApp(String ovfLocal,HostSystem host,Folder vmFolder,String newVmName) throws Exception{

		
		ServiceInstance si = GetServiceInstance.getServiceInstance();
		
		String hostip=host.getName();
		

		OvfCreateImportSpecParams importSpecParams = new OvfCreateImportSpecParams();
		importSpecParams.setHostSystem(host.getMOR());
		importSpecParams.setLocale("US");
		importSpecParams.setEntityName(newVmName);
		importSpecParams.setDeploymentOption("");
		OvfNetworkMapping networkMapping = new OvfNetworkMapping();
		networkMapping.setName("Network 1");
		networkMapping.setNetwork(host.getNetworks()[0].getMOR()); // network);
		importSpecParams.setNetworkMapping(new OvfNetworkMapping[] { networkMapping });
		importSpecParams.setPropertyMapping(null);

		String ovfDescriptor = readOvfContent(ovfLocal);
		if (ovfDescriptor == null) 
		{
			si.getServerConnection().logout();
			return;
		}
		
		ovfDescriptor = escapeSpecialChars(ovfDescriptor);
		//System.out.println("ovfDesc:" + ovfDescriptor);
			
		ResourcePool rp = ((ComputeResource)host.getParent()).getResourcePool();

		OvfCreateImportSpecResult ovfImportResult = si.getOvfManager().createImportSpec(
				ovfDescriptor, rp, host.getDatastores()[0], importSpecParams);

		if(ovfImportResult==null)
		{
			si.getServerConnection().logout();
			return;
		}
		
		long totalBytes = addTotalBytes(ovfImportResult);
		//System.out.println("Total bytes: " + totalBytes);

		HttpNfcLease httpNfcLease = null;

		httpNfcLease = rp.importVApp(ovfImportResult.getImportSpec(), vmFolder, host);
			
		// Wait until the HttpNfcLeaseState is ready
		HttpNfcLeaseState hls;
		for(;;)
		{
			hls = httpNfcLease.getState();
			if(hls == HttpNfcLeaseState.ready || hls == HttpNfcLeaseState.error)
			{
				break;
			}
		}
		
		if (hls.equals(HttpNfcLeaseState.ready)) 
		{
			//System.out.println("HttpNfcLeaseState: ready ");
			HttpNfcLeaseInfo httpNfcLeaseInfo = (HttpNfcLeaseInfo) httpNfcLease.getInfo();
		//	printHttpNfcLeaseInfo(httpNfcLeaseInfo);

			leaseUpdater = new LeaseProgressUpdater(httpNfcLease, 5000);
			leaseUpdater.start();

			HttpNfcLeaseDeviceUrl[] deviceUrls = httpNfcLeaseInfo.getDeviceUrl();
			
			long bytesAlreadyWritten = 0;
			for (HttpNfcLeaseDeviceUrl deviceUrl : deviceUrls) 
			{
				String deviceKey = deviceUrl.getImportKey();
				for (OvfFileItem ovfFileItem : ovfImportResult.getFileItem()) 
				{
					if (deviceKey.equals(ovfFileItem.getDeviceId())) 
					{
						//System.out.println("Import key==OvfFileItem device id: " + deviceKey);
						String absoluteFile = new File(ovfLocal).getParent() + File.separator + ovfFileItem.getPath();
						String urlToPost = deviceUrl.getUrl().replace("*", hostip);
						uploadVmdkFile(ovfFileItem.isCreate(), absoluteFile, urlToPost, bytesAlreadyWritten, totalBytes);
						bytesAlreadyWritten += ovfFileItem.getSize();
						//System.out.println("Completed uploading the VMDK file:" + absoluteFile);
					}
				}
			}

			leaseUpdater.interrupt();
			httpNfcLease.httpNfcLeaseProgress(100);
			httpNfcLease.httpNfcLeaseComplete();
		}
		si.getServerConnection().logout();
	}
	public static void main(String[] args) throws Exception 
	{
		
	}
	
	
	public static long addTotalBytes(OvfCreateImportSpecResult ovfImportResult)
	{
		OvfFileItem[] fileItemArr = ovfImportResult.getFileItem();
		
		long totalBytes = 0;
		if (fileItemArr != null) 
		{
			for (OvfFileItem fi : fileItemArr) 
			{
				//printOvfFileItem(fi);
				totalBytes += fi.getSize();
			}
		}
		return totalBytes;
	}
	
	private static void uploadVmdkFile(boolean put, String diskFilePath, String urlStr, 
			long bytesAlreadyWritten, long totalBytes) throws IOException 
	{
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() 
		{
			public boolean verify(String urlHostName, SSLSession session) 
			{
				return true;
			}
		});
		
		HttpsURLConnection conn = (HttpsURLConnection) new URL(urlStr).openConnection();
		conn.setDoOutput(true);
		conn.setUseCaches(false);
		conn.setChunkedStreamingMode(CHUCK_LEN);
		conn.setRequestMethod(put? "PUT" : "POST"); // Use a post method to write the file.
		conn.setRequestProperty("Connection", "Keep-Alive");
		conn.setRequestProperty("Content-Type",	"application/x-vnd.vmware-streamVmdk");
		conn.setRequestProperty("Content-Length", Long.toString(new File(diskFilePath).length()));
		
		BufferedOutputStream bos = new BufferedOutputStream(conn.getOutputStream());
		
		BufferedInputStream diskis = new BufferedInputStream(new FileInputStream(diskFilePath));
		int bytesAvailable = diskis.available();
		int bufferSize = Math.min(bytesAvailable, CHUCK_LEN);
		byte[] buffer = new byte[bufferSize];
		
		long totalBytesWritten = 0;
		while (true) 
		{
			int bytesRead = diskis.read(buffer, 0, bufferSize);
			if (bytesRead == -1) 
			{
				//System.out.println("Total bytes written: " + totalBytesWritten);
				break;
			}

			totalBytesWritten += bytesRead;
			bos.write(buffer, 0, bufferSize);
			bos.flush();
			//System.out.println("Total bytes written: " + totalBytesWritten);
			int progressPercent = (int) (((bytesAlreadyWritten + totalBytesWritten) * 100) / totalBytes);
			leaseUpdater.setPercent(progressPercent);			
		}
		
		diskis.close();
		bos.flush();
		bos.close();
		conn.disconnect();
	}
	
	public static String readOvfContent(String ovfFilePath)	throws IOException 
	{
		StringBuffer strContent = new StringBuffer();
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(ovfFilePath)));
		String lineStr;
		while ((lineStr = in.readLine()) != null) 
		{
			strContent.append(lineStr);
		}
		in.close();
		return strContent.toString();
	}

	

	

	public static String escapeSpecialChars(String str)
	{
		str = str.replaceAll("<", "&lt;");
		return str.replaceAll(">", "&gt;"); // do not escape "&" -> "&amp;", "\"" -> "&quot;"
	}
}