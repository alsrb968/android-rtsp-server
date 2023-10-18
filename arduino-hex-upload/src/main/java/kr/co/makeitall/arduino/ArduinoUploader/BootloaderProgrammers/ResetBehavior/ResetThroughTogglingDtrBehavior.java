package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.ResetBehavior;



import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.SerialPortConfig;
import kr.co.makeitall.arduino.ArduinoUploader.Help.ISerialPortStream;

public class ResetThroughTogglingDtrBehavior implements IResetBehavior {
	private boolean Toggle;

	private boolean getToggle() {
		return Toggle;
	}

	public ResetThroughTogglingDtrBehavior(boolean toggle) {
		Toggle = toggle;
	}

	@Override
	public final ISerialPortStream Reset(ISerialPortStream serialPort, SerialPortConfig config) {
		serialPort.setDtrEnable(getToggle());
		return serialPort;
	}
}
