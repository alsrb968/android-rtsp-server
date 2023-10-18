package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.ResetBehavior;

import android.content.Context;
import kr.co.makeitall.arduino.ArduinoUploader.ArduinoSketchUploader;
import kr.co.makeitall.arduino.ArduinoUploader.ArduinoUploaderException;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.SerialPortConfig;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.WaitHelper;
import kr.co.makeitall.arduino.ArduinoUploader.Help.ISerialPortStream;
import kr.co.makeitall.arduino.ArduinoUploader.Help.SerialStreamHelper;
import kr.co.makeitall.arduino.ArduinoUploader.IArduinoUploaderLogger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ResetThrough1200BpsBehavior<E extends ISerialPortStream> implements IResetBehavior {
    private static IArduinoUploaderLogger getLogger() {
        return ArduinoSketchUploader.getLogger();
    }

    private Class<E> inferedClass;
    private Context mContext;

    public ResetThrough1200BpsBehavior(Class<E> typeParameterClass, Context context) {
        this.inferedClass = typeParameterClass;
        this.mContext = context;
    }

    @Override
    public final ISerialPortStream Reset(ISerialPortStream serialPort, SerialPortConfig config) {
        final int timeoutVirtualPortDiscovery = 10000;
        final int virtualPortDiscoveryInterval = 100;
        if (getLogger() != null)
            getLogger().onInfo("Issuing forced 1200bps reset...");
        String currentPortName = serialPort.getPortName();
        String[] originalPorts = serialPort.getPortNames();
        // Close port ...
        serialPort.close();

        // And now open port at 1200 bps
        serialPort = SerialStreamHelper.newInstance(inferedClass, mContext, currentPortName, 1200);
        // new SerialPortStream(currentPortName, 1200);
        serialPort.setDtrEnable(true);
        serialPort.setRtsEnable(true);
        serialPort.open();
        // Close and wait for a new virtual COM port to appear ...
        serialPort.close();

        // Remove duplicates from an array in Java [duplicate]
        Set<String> ports = new HashSet<String>();
        for (String port : serialPort.getPortNames()) {
            ports.add(port);
        }
        // excpet
        Iterator<String> itr = ports.iterator();
        while (itr.hasNext()) {
            String sp = (String) itr.next();
            for (String pt : originalPorts) {
                if (sp.equals(pt))
                    itr.remove();
            }
        }
        String[] uniquePorts = ports.toArray(new String[ports.size()]);
        final String portSingleOrDefault;
        if (uniquePorts.length > 0) {
            portSingleOrDefault = uniquePorts[0];
        } else
            portSingleOrDefault = null;

        String newPort = WaitHelper.WaitFor(timeoutVirtualPortDiscovery, virtualPortDiscoveryInterval,
                () -> portSingleOrDefault,
                (i, item, interval) -> item == null ? String.format("T+%1$s - Port not found", i * interval)
                        : String.format("T+%1$s - Port found: %2$s", i * interval, item));

        if (newPort == null) {
            throw new ArduinoUploaderException(String
                    .format("No (unambiguous) virtual COM port detected (after %1$sms).", timeoutVirtualPortDiscovery));
        }

        E tempVar = SerialStreamHelper.newInstance(inferedClass, mContext, newPort, config.getBaudRate());
        // new SerialPortStream(newPort, config.getBaudRate());
        tempVar.setNumDataBits(8);
        tempVar.setParity(ISerialPortStream.NO_PARITY);
        tempVar.setNumStopBits(ISerialPortStream.ONE_STOP_BIT);
        tempVar.setDtrEnable(true);
        tempVar.setRtsEnable(true);
        return tempVar;
    }

}
