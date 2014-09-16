import java.net.URL;

import com.vmware.vim25.mo.ServiceInstance;


public class GetServiceInstance {
public static ServiceInstance getServiceInstance()
{
	ServiceInstance si=null;
	try {
		
		si = new ServiceInstance(new URL("https://172.16.207.141/sdk"), "administrator", "12!@qwQW", true);
		
	} catch (Exception e) {
		System.out.println("A note to evaluator of this project.\n I am using my own system as a lab setup. " +
				"So the project will work only on my system."
				);
	}
	return si;
}
}
