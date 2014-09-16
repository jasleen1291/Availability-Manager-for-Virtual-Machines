import java.io.File;
import java.util.ArrayList;

import com.vmware.vim25.AlarmState;
import com.vmware.vim25.mo.Alarm;
import com.vmware.vim25.mo.AlarmManager;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * General Note to Evaluator:::: I have used my own system as lab. I havent use
 * uniersity's lab resources. You can call me any time for demo.
 * 
 * 
 * 
 * **/
public class Main {
	ArrayList<MyVM> vmlist;
	ServiceInstance si;
	AlarmManager alarmManager;
	ArrayList<HostSystem> hostlist;
	Datacenter datacenter = null;
	Thread pingManager;
	ManagedEntity[] hosts;
	String processed=null,pinging=null;
	// HostSystem host;
	Folder rootFolder;
	void startAvailabiltyManager() throws Exception {
		vmlist = new ArrayList<MyVM>();
		si = GetServiceInstance.getServiceInstance();
		rootFolder = si.getRootFolder();
		hostlist = new ArrayList<>();
		alarmManager = si.getAlarmManager();
		
		queryHosts();
		ManagedEntity[] datacenters = new InventoryNavigator(rootFolder)
				.searchManagedEntities("Datacenter");
		datacenter = (Datacenter) datacenters[0];
		
		// /if(hosts!=null)
		// host=(HostSystem)hosts[0];
		

		
		pingManager = new Thread(new Runnable() {

			@Override
			public void run() {
				while(true)
				{
					
					//System.out.println(1);
					for(int i=0;i<hostlist.size();i++)
					{
						
						System.out.println("Checking status of host "+hostlist.get(i).getName());
						boolean status=checkHost(hostlist.get(i));
						if(!status)
						{
							System.out.println("No available host");
							return;
						}
						else
						{
							try
							{
							VirtualMachine vms[]=hostlist.get(i).getVms();
							
							for (int j = 0; j < vms.length; j++) {
								Alarm alarm[] = alarmManager.getAlarm(vms[j]);

								VirtualMachine vm = (VirtualMachine) vms[j];
								//System.out.println(vm.getName());
								//System.out.println(processed);
								//System.out.println();
								if(processed!=null&&vm.getName().equalsIgnoreCase(processed))
									continue;
								
								// System.out.println(vm.getName()+"\t"+alarm.length);
								if (alarm.length < 1)
									AddAlarm.addAlarmToVM(alarmManager, vm);
								MyVM myvm=null;
								
									for(int v=0;v<vmlist.size();v++)
									{
										if(vmlist.get(v).getVm().getName().equalsIgnoreCase(vm.getName()))
											{myvm=vmlist.get(v);
											myvm.setHost(hostlist.get(i));
											}
											
									}
								if(myvm==null)
								{
									myvm = new MyVM(vm);
									myvm.setHost(hostlist.get(i));
									vmlist.add(myvm);
								}
								Thread.sleep(2000);
								boolean ping=true;;
								pinging=vm.getName();
								AlarmState[] alm = (vm.getTriggeredAlarmState());
								if (alm != null) {
									for (int k = 0; k < alm.length; k++) {
										if (alm[k].overallStatus.toString()
												.equalsIgnoreCase("red")) {
											System.out.println("Virtual Machine " + vm.getName() 
													+ " is not powered on. Powering it on\n");
											ping=myvm.powerOn();
										}

									}
									//System.out.println("1");
								}
								if (ping) {
									boolean alive = false;
									for (int r = 0; r < 5; r++) {
										try {
											// System.out.println("2");
											System.out.println("\nPing attempt "
													+ (r + 1) + " on " + vm.getName()
													+ " \n");
											String result = null;

											if (vm.getGuest() != null) {
												if (vm.getGuest().getIpAddress() != null) {
													result = PingExample.ping(vm
															.getGuest().getIpAddress());
													{
														if (!result.equals("100")) {
															myvm.printStats();
															alive = true;
															break;
														}
													}
												}
											}

										} catch (Exception e) {
											e.printStackTrace();
										}
										if (r != 4) {
											System.out.println("Virtual Machine "
													+ vm.getName()
													+ " unresponsive, Pinging again in 1 min\n");

											Thread.sleep(1000*60);
										}

									}
									if (!alive) {
										System.out.println("Pinging failed on 5 consecutive attempts. Reverting to last snapshot");
										

										myvm.revertToLastSnapshot();
									} 
									
							}
							}
							}
							catch(Exception e)
							{
								
							}
						}
						pinging=null;
					}
			try {
					Thread.sleep(1000*60*2);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
			}
				
			}
			
		});
		pingManager.setName("Ping Manager");
		pingManager.start();
		Thread snapshotManager=new Thread(new Runnable() {
			
			@Override
			public void run() {
				while(true)
				{
					while(vmlist.isEmpty())
					{
						try {
							Thread.sleep(1000*60);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
					}
					boolean started=false;
					for(int i=0;i<vmlist.size();i++)
					{
						String vmname=vmlist.get(i).getVm().getName();
						if(pinging!=null&&vmname.equalsIgnoreCase(pinging))
							continue;
						processed=vmname;
						started=true;
						vmlist.get(i).takeSnaphot();
						processed=null;
					}
					if(started)
					{
					try {
						Thread.sleep(1000*60*10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
					}
					else
					{
						try {
							Thread.sleep(1000*60*2);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
					}
				}
				
			}
		});
		snapshotManager.start();
			}

	
	
	private boolean checkHost(HostSystem host) {
	System.out.println("Pinging Host");
		AlarmState alm[] = host.getTriggeredAlarmState();
		for (int i = 0; i < 4; i++) {
			String result = null;

			result = PingExample.ping(host.getName());
			
			if (!result.equals("100")) {
				if (alm != null) {
					for (int j = 0; j< alm.length; j++) {
						if (alm[j].overallStatus.toString().equalsIgnoreCase(
								"yellow")||alm[j].overallStatus.toString().equalsIgnoreCase(
										"red")) { // Disconnected
							System.out
									.println("Host is disconnected\nAttempting reconnection");
							try {
								HostOperations.reconnectHost(host, 443,
										datacenter.getHostFolder(), "root",
										"1234567");
								return true;
							} catch (Exception e) {
								return false;

							}

						}
						
						
					}
				}
				else
				{
					System.out.println("Host is alive and connected");
					return true;
				}

			}

			if (i != 4) {
				System.out.println("Host " + host.getName()
						+ " unresponsive, Pinging again in 1 min");
				try {
					pingManager.sleep(60);
				} catch (Exception e) {

				}
			}
			

		}
		
				try {
					String hostname=host.getName();
						System.out.println("Host has failed. Proceeding towards removing it\n");
						ManagedEntity[] vms = new InventoryNavigator(rootFolder)
						.searchManagedEntities(new String[][] { { "VirtualMachine",
								"name" }, }, true);
						String names[]=new String[vms.length];
						for(int i=0;i<vms.length;i++)
						{
							VirtualMachine vm=(VirtualMachine)vms[i];
							names[i]=new String(vm.getName());
						}
						HostOperations.removeHost(host);
						hostlist.remove(host);
						System.out.println("Host Removed Succesfully");
						queryHosts();
						HostSystem failoverhost=null;
						if(hostlist.isEmpty())
						{
							System.out.println("No other host found. Adding another host");
							HostOperations.addHost(getFailoverHost(hostname), 443, datacenter.getHostFolder(), "root", "1234567");
						
						}
						queryHosts();
						failoverhost=hostlist.get(0);
						
						for(int i=0;i<names.length;i++)
						{
							try
							{
							//System.out.println(names[i]);
							File directory=new File(names[i]);
							if(directory.exists())
							{
								File ovf=new File(directory, names[i]+ ".ovf");
								File vmdk=new File(directory,"disk-0.vmdk");
								if(ovf.exists()&&vmdk.exists())
								{
									System.out.println("Exporting virtual machine "+names[i]+" from saved file to failover host");
									new ImportLocalOvfVApp(ovf.getAbsolutePath(), failoverhost,datacenter.getVmFolder(), names[i]);
								}
							}
							else
							{
								
							}
							}
							catch(Exception e)
							{
								
							}
						}
						return true;
					} catch (Exception e) {
						// TODO Auto-generated catch block
						
						e.printStackTrace();
						return false;
					}
	}

	public static void main(String[] args) throws Exception {
		new Main().startAvailabiltyManager();
	}
public void queryHosts() throws Exception
{
	hosts = new InventoryNavigator(rootFolder)
	.searchManagedEntities("HostSystem");
	if(!hostlist.isEmpty())
	hostlist.clear();
	for (int h = 0; h < hosts.length; h++) {
		HostSystem host = (HostSystem) hosts[h];
		hostlist.add(host);
		if (alarmManager.getAlarm(host).length < 1)
			AddAlarmHost.addAlarmToVM(alarmManager, host);
	}
}
public String getFailoverHost(String ip)
{
	if(ip.equalsIgnoreCase("172.16.207.133"))
		return "172.16.207.143";
	else
		return "172.16.207.133";
	}
}
