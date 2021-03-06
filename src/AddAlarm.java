import java.rmi.RemoteException;

import com.vmware.vim25.Action;
import com.vmware.vim25.AlarmAction;
import com.vmware.vim25.AlarmSetting;
import com.vmware.vim25.AlarmSpec;
import com.vmware.vim25.AlarmTriggeringAction;
import com.vmware.vim25.DuplicateName;
import com.vmware.vim25.InvalidName;
import com.vmware.vim25.MethodAction;
import com.vmware.vim25.MethodActionArgument;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.StateAlarmExpression;
import com.vmware.vim25.StateAlarmOperator;
import com.vmware.vim25.mo.Alarm;
import com.vmware.vim25.mo.AlarmManager;
import com.vmware.vim25.mo.VirtualMachine;

public class AddAlarm {
	public static void addAlarmToVM(AlarmManager alarmMgr, VirtualMachine vm) {
		AlarmSpec spec = new AlarmSpec();

		StateAlarmExpression expression = createStateAlarmExpression();
		spec.setName("VmPowerStateAlarm_"+vm.getName());
	    spec.setDescription("Monitor VM state and send email " +
	    		"and power it on if VM powers off");
	    spec.setEnabled(true);
	    AlarmSetting as = new AlarmSetting();
	    as.setReportingFrequency(0); //as often as possible
	    as.setToleranceRange(0);
	 //   AlarmAction methodAction = createAlarmTriggerAction(            createPowerOnAction());
	    //spec.setAction(methodAction);
	    spec.setSetting(as);
	    spec.setExpression(expression);
	    try {
			Alarm alrm=alarmMgr.createAlarm(vm, spec);
			//System.out.println("Alarm added successfully\t"+alrm);
		} catch (InvalidName e) {
			System.out.println("Invalid Name");
			e.printStackTrace();
		} catch (DuplicateName e) {
			//System.out.println("Duplicate Name");
			//e.printStackTrace();
		} catch (RuntimeFault e) {
			System.out.println("Runtime fault");
			e.printStackTrace();
		} catch (RemoteException e) {
			System.out.println("remote exception");
			e.printStackTrace();
		}
	}

	private static StateAlarmExpression createStateAlarmExpression() {
		StateAlarmExpression expression = new StateAlarmExpression();
		expression.setType("VirtualMachine");
		expression.setStatePath("runtime.powerState");
		expression.setOperator(StateAlarmOperator.isUnequal);
		expression.setRed("poweredOn");
		return expression;
	}
	
}
