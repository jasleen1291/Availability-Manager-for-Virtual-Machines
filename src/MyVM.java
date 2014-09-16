import java.io.File;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.axis.utils.StringUtils;

import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.InsufficientResourcesFault;
import com.vmware.vim25.InvalidState;
import com.vmware.vim25.NotFound;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.SnapshotFault;
import com.vmware.vim25.TaskInProgress;
import com.vmware.vim25.VirtualMachineQuickStats;
import com.vmware.vim25.VmConfigFault;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class MyVM {
	// instance variables - replace the example below with your own
	private String vmname;
	private ServiceInstance si;
	private  VirtualMachine vm;
	File ovfDirectory;
	DateFormat dateFormat = 
            new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	HostSystem host=null;
	public HostSystem getHost() {
		return host;
	}

	public void setHost(HostSystem host) {
		this.host = host;
	}

	public MyVM(VirtualMachine vm) {
		super();
		this.vm = vm;
		this.vmname=vm.getName();
		ovfDirectory=new File(vmname);
		if(!ovfDirectory.exists())
			ovfDirectory.mkdir();
	}

	/**
	 * Destructor for objects of class MyVM
	 */
	protected void finalize() throws Throwable {
		this.si.getServerConnection().logout(); // do finalization here
		super.finalize(); // not necessary if extending Object.
	}

	/**
	 * Power On the Virtual Machine
	 */
	public boolean powerOn() {
		try {
			//System.out.println("Powering on"+vmname);
			Task task = vm.powerOnVM_Task(null);
			if (task.waitForMe() == Task.SUCCESS) {
				//System.out.println(vmname + " powered on");
				//Thread.sleep(60000);
				return true;
			}
		} catch (Exception e) {
			//System.out.println(e.toString());
		}
		return false;
	}

	/**
	 * Power Off the Virtual Machine
	 */
	public void powerOff() {
		try {
			System.out.println("command: powered off");
			Task task = vm.powerOffVM_Task();
			if (task.waitForMe() == Task.SUCCESS) {
				System.out.println(vmname + " powered off");
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	/**
	 * Reset the Virtual Machine
	 */

	public void reset() {
		try {
			System.out.println("command: reset");
			Task task = vm.resetVM_Task();
			if (task.waitForMe() == Task.SUCCESS) {
				System.out.println(vmname + " reset");
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	/**
	 * Suspend the Virtual Machine
	 */

	public void suspend() {
		try {
			//System.out.println("command: suspend");
			Task task = vm.suspendVM_Task();
			if (task.waitForMe() == Task.SUCCESS) {
				//System.out.println(vmname + " suspended");
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	/**
	 * Put VM & Guest OS on Standby
	 */
	public void standBy() {
		try {
			System.out.println("command: stand by");
			vm.standbyGuest();
			System.out.println(vmname + " guest OS stoodby");
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}
	
	public void printStats()
	{
		
		System.out.println("\n*******************Virtual Machine Statistic for "+vmname+"******************* at "+dateFormat.format(new Date()));
		GuestInfo guestInfo=vm.getGuest();
		System.out.println("Guest State "+guestInfo.getGuestState());
		System.out.println(guestInfo.getHostName());
		System.out.println(guestInfo.toolsRunningStatus);
		System.out.println(guestInfo.toolsVersion);
		VirtualMachineQuickStats vmqs= vm.getSummary().getQuickStats();
		System.out.println("Ballooned Memory\t"+vmqs.getBalloonedMemory());
		//System.out.println("Compressed Memory\t"+vmqs.getCompressedMemory());
		System.out.println("Consumed Overhead Memory\t"+vmqs.getConsumedOverheadMemory());
		System.out.println("Distributed CPU Entitlement\t"+vmqs.getDistributedCpuEntitlement());
		System.out.println("Guest Memory Usage\t"+vmqs.getGuestMemoryUsage());
		System.out.println("Host Memory Usage\t"+vmqs.getHostMemoryUsage());
		System.out.println("Overall CPU Demand\t"+vmqs.getOverallCpuDemand());
		System.out.println("Private memory\t"+vmqs.getPrivateMemory());
		System.out.println("Shared Memory\t"+vmqs.getSharedMemory());
		System.out.println("Static CPU Entitlement\t"+vmqs.getStaticCpuEntitlement());
		System.out.println("Static Memory Entitlement\t"+vmqs.getStaticMemoryEntitlement());
		System.out.println("Swapped Memory\t"+vmqs.getSwappedMemory());
		//System.out.println("Uptime seconds\t"+vmqs.getUptimeSeconds());
		System.out.println("Number of CPU\t"+vm.getSummary().config.getNumCpu());
		System.out.println("Number of ethernet cards\t"+vm.getSummary().config.getNumEthernetCards());
		System.out.println("Number of virtual Disks\t"+vm.getSummary().config.getNumVirtualDisks());
		
		
	}
	public VirtualMachine getVm() {
		synchronized(vm)
		{
		return vm;
		}
	}

	public void setVm(VirtualMachine vm) {
		this.vm = vm;
	}

	public File getOvfDirectory() {
		return ovfDirectory;
	}

	public void setOvfDirectory(File ovfDirectory) {
		this.ovfDirectory = ovfDirectory;
	}

	public boolean isAvailable()
	{//System.out.println(vm.getGuest().getIpAddress());
		try
		{
			if(StringUtils.isEmpty(vm.getGuest().getIpAddress())||vm.getGuest().getIpAddress().equals("null"))
				return false;
		for (int i = 0; i < 5; i++) {
			
			String result = PingExample.ping(vm.getGuest()
					.getIpAddress());
			if (!result.equals("100"))
				{
				
				return true;
				
				}
			System.out.println("Virtual Machine unresponsive, Pinging again in 1 min");
			Thread.sleep(60000);
		}
		}
		catch(Exception e)
		{
			//System.out.println(e);
		}
		return false;
	}
	public void takeSnaphot()
	{
		System.out.println("Refreshing backup for "+vm.getName());
		if(vm.getGuest().getGuestState().equalsIgnoreCase("running"))
			{
		try
		{
		String name = vm.getName() + "Snapshot"
				+ System.nanoTime();
		String desc = "Refreshing the Backup Cache";
		
		Task task =vm.createSnapshot_Task(name,
				desc, false, false);
		// vmlist.get(i).m

		if (task.waitForMe() == Task.SUCCESS) {
			System.out.println("\nSnapshot was created.");
		}
		suspend();
		//System.out.println(host);
		new ExportOvfToLocal(vmname, host.getName(), ovfDirectory.getAbsolutePath());
		powerOn();
		}
		catch(Exception e)
		{
			//System.out.println(e);
			
		}
			}
	}
	public void revertToLastSnapshot()
	{
		Task revertTask;
		try {
			revertTask = vm.revertToCurrentSnapshot_Task(host);
			if (revertTask.waitForMe() == Task.SUCCESS) {
				System.out.println("Reverted Back to Snapshot");
				powerOn();
			}
		} catch (VmConfigFault e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SnapshotFault e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TaskInProgress e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidState e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InsufficientResourcesFault e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotFound e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeFault e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
