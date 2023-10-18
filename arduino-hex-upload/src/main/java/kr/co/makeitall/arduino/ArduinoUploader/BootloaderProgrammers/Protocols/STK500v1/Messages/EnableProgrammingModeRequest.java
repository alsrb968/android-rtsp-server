package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v1.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Request;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v1.Constants;

public class EnableProgrammingModeRequest extends Request
{
	public EnableProgrammingModeRequest()
	{
		setBytes(new byte[] {Constants.CmdStkEnterProgmode, Constants.SyncCrcEop});
	}
}
