package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.STK500v2.Messages;

import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Request;
import kr.co.makeitall.arduino.ArduinoUploader.Hardware.Memory.IMemory;

public class ExecuteProgramPageRequest extends Request {

    //ORIGINAL LINE: internal ExecuteProgramPageRequest(byte writeCmd, IMemory memory, IReadOnlyCollection<byte> data)
    public ExecuteProgramPageRequest(byte writeCmd, IMemory memory, final byte[] data) {
        int len = data.length;
        final byte mode = (byte) 0xc1;
        byte[] headerBytes = new byte[]{writeCmd, (byte) (len >> 8), (byte) (len & 0xff), mode, memory.getDelay(), memory.getCmdBytesWrite()[0], memory.getCmdBytesWrite()[1], memory.getCmdBytesRead()[0], memory.getPollVal1(), memory.getPollVal2()};
        setBytes(Concat(headerBytes, data));
    }

    static byte[] Concat(byte[] a, byte[] b) {
        byte[] output = new byte[a.length + b.length];
        for (int i = 0; i < a.length; i++)
            output[i] = a[i];
        for (int j = 0; j < b.length; j++)
            output[a.length + j] = b[j];
        return output;
    }
}
