package kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers;

import android.content.Context;
import kr.co.makeitall.arduino.ArduinoUploader.ArduinoUploaderException;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.IRequest;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.Protocols.Response;
import kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.ResetBehavior.IResetBehavior;
import kr.co.makeitall.arduino.ArduinoUploader.Hardware.IMcu;
import kr.co.makeitall.arduino.ArduinoUploader.Help.ISerialPortStream;
import kr.co.makeitall.arduino.ArduinoUploader.Help.SerialStreamHelper;
import kr.co.makeitall.arduino.CSharpStyle.BitConverter;

import java.util.concurrent.TimeoutException;

public abstract class ArduinoBootloaderProgrammer<E extends ISerialPortStream> extends BootloaderProgrammer<E> {
    protected kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.SerialPortConfig SerialPortConfig;

    private Class<E> inferedClass;
    private Context mContext;

    protected ArduinoBootloaderProgrammer(kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.SerialPortConfig serialPortConfig, IMcu mcu) {
        super(mcu);
        SerialPortConfig = serialPortConfig;
    }

    protected ArduinoBootloaderProgrammer(Class<E> clazz, kr.co.makeitall.arduino.ArduinoUploader.BootloaderProgrammers.SerialPortConfig serialPortConfig, IMcu mcu) {
        this(serialPortConfig, mcu);
        inferedClass = clazz;
    }

    private E SerialPort;

    protected final E getSerialPort() {
        return SerialPort;
    }

    protected final void setSerialPort(E value) {
        SerialPort = value;
    }

    public void setClazz(Class<E> clazz) {
        inferedClass = clazz;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    @Override
    public void Open() {
        String portName = SerialPortConfig.getPortName();
        int baudRate = SerialPortConfig.getBaudRate();
        if (getLogger() != null)
            getLogger().onInfo(String.format("Opening serial port %1$s - baudrate %2$s", portName, baudRate));
        // setSerialPort(new SerialPortStream(portName, baudRate));
        setSerialPort(SerialStreamHelper.newInstance(inferedClass, mContext, portName, baudRate));
        getSerialPort().setReadTimeout(SerialPortConfig.getReadTimeOut());
        getSerialPort().setWriteTimeout(SerialPortConfig.getWriteTimeOut());

        IResetBehavior preOpen = SerialPortConfig
                .getPreOpenResetBehavior();
        if (preOpen != null) {
            if (getLogger() != null)
                getLogger().onInfo(String.format("Executing Post Open behavior (%1$s)...", preOpen));
            setSerialPort((E) preOpen.Reset(getSerialPort(), SerialPortConfig));
        }
        try {
            getSerialPort().open();
        } catch (IllegalStateException ex) {
            throw new ArduinoUploaderException(
                    String.format("Unable to open serial port %1$s - %2$s.", portName, ex.getMessage()));
        }
        if (getLogger() != null)
            getLogger().onTrace(String.format("Opened serial port %1$s with baud rate %2$s!", portName, baudRate));

        IResetBehavior postOpen = SerialPortConfig
                .getPostOpenResetBehavior();
        if (postOpen != null) {
            if (getLogger() != null)
                getLogger().onInfo(String.format("Executing Post Open behavior (%1$s)...", postOpen));
            setSerialPort((E) postOpen.Reset(getSerialPort(), SerialPortConfig));
        }

        int sleepAfterOpen = SerialPortConfig.getSleepAfterOpen();
        if (SerialPortConfig.getSleepAfterOpen() <= 0) {
            return;
        }

        if (getLogger() != null)
            getLogger().onTrace(String.format("Sleeping for %1$s ms after open...", sleepAfterOpen));
        try {
            Thread.sleep(sleepAfterOpen);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private int newInstance(String portName, int baudRate) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void EstablishSync() {
        // Do nothing.
    }

    @Override
    public void Close() {
        IResetBehavior preClose = SerialPortConfig
                .getCloseResetAction();
        if (preClose != null) {
            if (getLogger() != null)
                getLogger().onInfo("Resetting...");
            setSerialPort((E) preClose.Reset(getSerialPort(), SerialPortConfig));
        }
        if (getLogger() != null)
            getLogger().onInfo("Closing serial port...");
        getSerialPort().setDtrEnable(false);
        getSerialPort().setRtsEnable(false);

        try {
            getSerialPort().close();
        } catch (RuntimeException e) {
            e.printStackTrace();
            // Ignore
        }
    }

    protected void Send(IRequest request) {
        byte[] bytes = request.getBytes();
        int length = bytes.length;
        if (getLogger() != null)
            getLogger().onTrace(String.format("Sending %1$s bytes: %2$s", length, System.getProperty("line.separator"))
                    + String.format("%1$s", BitConverter.toString(bytes)));
        // getSerialPort().Write(bytes, 0, length);//) la offset tu 0
        getSerialPort().writeBytes(bytes, length, 0);
    }

    protected <TResponse extends Response> TResponse Receive(TResponse responseType) {
        return Receive(responseType, 1);
    }

    // https://stackoverflow.com/questions/2619429/c-sharp-to-java-where-t-new-syntax
    // public class TypeStuff {
    //
    // private static final long serialVersionUID = 1L;
    //
    // // *** PUBLIC STATIC VOID MAIN ***
    // public static void main(String[] args) {
    // TypeStuff ts = new TypeStuff();
    // ts.run();
    // }
    //
    // // *** CONSTRUCTOR ***
    // public TypeStuff() {
    // }
    //
    // public void run() {
    // Fruit banana = new Banana();
    // Fruit dupe = newT(banana);
    // System.out.println("dupe.getColor()=" + dupe.getColor());
    // Fruit orange = new Orange();
    // Fruit dupe2 = newT(orange);
    // System.out.println("dupe2.getColor()=" + dupe2.getColor());
    // }
    //
    // public <T extends Fruit> T newT(T fruit) {
    // T dupe = null;
    // try {
    // Class clazz = fruit.getClass();
    // dupe = (T) clazz.newInstance();
    // } catch (Exception ex) {
    // } finally {
    // return dupe;
    // }
    // }
    //
    // }
    // //C#
    // public E MakeForm<E>() where E : Form { }
    // MyFormType form = MakeForm<MyFormType>();
    //
    //// Java
    // public <E extends Form> E makeForm(Class<E> formType) {
    // //return new instance of formType
    // }
    // makeForm(SomeForm.class).specificSomeFormMethod();

    @SuppressWarnings("unchecked")
    protected final <TResponse extends Response> TResponse Receive(TResponse responseType, int length) {
        TResponse tempVar = null;
        byte[] bytes = ReceiveNext(length);
        if (bytes == null) {
            return null;
        }
        try {
            Class clazz = responseType.getClass();
            tempVar = (TResponse) clazz.newInstance();
            // tempVar = clazz.getConstructor(String.class).newInstance(p);//su dung khi can
            // them bien dau vao de khoi tao
            tempVar.setBytes(bytes);
        } catch (Exception ex) {
            ex.printStackTrace();

        } finally {
            return tempVar;
        }
    }

    protected final int ReceiveNext() {
        byte[] bytes = new byte[1];
        try {
            // getSerialPort().Read(bytes, 0, 1);//0 la offset
            int numRead = getSerialPort().readBytes(bytes, 1, 0);
            if (numRead < 0)
                throw new TimeoutException();
            if (getLogger() != null)
                getLogger().onTrace(String.format("Receiving byte: %1$s", BitConverter.toString(bytes)));
            return bytes[0];
        } catch (TimeoutException ex) {
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    protected final byte[] ReceiveNext(int length) {
        byte[] bytes = new byte[length];
        int retrieved = 0;
        try {
            while (retrieved < length) {
                // retrieved += getSerialPort().Read(bytes, retrieved, length -
                // retrieved);//retrieved la offset
                int numRead = getSerialPort().readBytes(bytes, length - retrieved, retrieved);
                if (numRead < 0) {
                    throw new TimeoutException();
                }
                retrieved += numRead;
            }
            if (getLogger() != null)
                getLogger().onTrace(String.format("Receiving bytes: %1$s", BitConverter.toString(bytes)));
            return bytes;
        } catch (TimeoutException ex) {
            throw new ArduinoUploaderException("Time out read data!");
        }
    }

}
