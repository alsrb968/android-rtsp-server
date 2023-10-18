package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.AVR109.Constants;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Request;
import kr.co.makeitall.arduino.ArduinoUploader.Hardware.Memory.MemoryType;

public class StartBlockReadRequest extends Request
{
	public StartBlockReadRequest(MemoryType memType, int blockSize)
	{
		setBytes(new byte[] {Constants.CmdStartBlockRead, (byte)(blockSize >> 8), (byte)(blockSize & 0xff), (byte)(memType == MemoryType.Flash ? 'F' : 'E')});
	}
}
