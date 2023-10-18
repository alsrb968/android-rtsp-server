package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v1.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Request;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v1.Constants;

public class GetSyncRequest extends Request
{
	public GetSyncRequest()
	{
		setBytes(new byte[] {Constants.CmdStkGetSync, Constants.SyncCrcEop});
	}
}
