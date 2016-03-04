package name.antonsmirnov.firmata.serial;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;
import jssc.SerialPortEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

/**
 * "ISerial" implementation using jSSC serial library
 */
public class JsscSerial implements ISerial, SerialPortEventListener {

    private final static String defaultPortName = "COM1";
    private final static int defaultBaudRate = 9600;
    private final static char defaultParity = 'N';
    private final static int defaultDataBits = 8;
    private final static float defaultStopBits = 1;
    private static final Logger logger = LoggerFactory.getLogger(JsscSerial.class);

    private int baudRate;
    private int parity;
    private int dataBits;
    private int stopBits;
    private SerialPort port;
    private boolean isStopping;
    private List<ISerialListener> listeners;
    private List<Byte> buffer;
   
    public JsscSerial() {
        this(defaultPortName, defaultBaudRate, defaultParity, defaultDataBits, defaultStopBits);
    }

    public JsscSerial(int baudRate) {
        this(defaultPortName, baudRate, defaultParity, defaultDataBits, defaultStopBits);
    }

    public JsscSerial(String portName, int baudRate) {
        this(portName, baudRate, defaultParity, defaultDataBits, defaultStopBits);
    }

    public JsscSerial(String portName) {
        this(portName, defaultBaudRate, defaultParity, defaultDataBits, defaultStopBits);
    }
    
    public JsscSerial(String portName, int baudRate, char parity, int dataBits, float stopBits) {
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        
        this.parity = SerialPort.PARITY_NONE;
        if (parity == 'O') this.parity = SerialPort.PARITY_ODD;
        if (parity == 'E') this.parity = SerialPort.PARITY_EVEN;
        if (parity == 'M') this.parity = SerialPort.PARITY_MARK;
        if (parity == 'S') this.parity = SerialPort.PARITY_SPACE;

        this.stopBits = SerialPort.STOPBITS_1;
        if (stopBits == 1.5f) this.stopBits = SerialPort.STOPBITS_1_5;
        if (stopBits == 2) this.stopBits = SerialPort.STOPBITS_2;

        this.dataBits = SerialPort.DATABITS_8;
        if (dataBits == 5) this.dataBits = SerialPort.DATABITS_5;
        if (dataBits == 6) this.dataBits = SerialPort.DATABITS_6;
        if (dataBits == 7) this.dataBits = SerialPort.DATABITS_7;

        port = new SerialPort(portName);
        buffer = Collections.synchronizedList(new LinkedList<Byte>());
        listeners = new ArrayList<ISerialListener>();
    }

    public void start() throws SerialException {
        isStopping = false;
        try {
            port.openPort();
            port.setParams(baudRate, dataBits, stopBits, parity);
            port.setEventsMask(SerialPort.MASK_RXCHAR);
            port.addEventListener(this);
        } catch (SerialPortException e) {
            throw new SerialException(e);
        }
    }
    
    public void stop() throws SerialException {
        isStopping = true;
        try {
            port.closePort();
        } catch (SerialPortException e) {
            throw new SerialException(e);
        }
    }
 
    public boolean isStopping() {
        return isStopping;
    }   

    public void addListener(ISerialListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(ISerialListener listener) {
        listeners.remove(listener);
    }

    public void write(byte[] outcomingBytes) throws SerialException {
        try {
            port.writeBytes(outcomingBytes);
        } catch (SerialPortException e) {
            throw new SerialException(e);
        }
    }
    
    public void write(int outcomingByte) throws SerialException {
        try {
            port.writeInt(outcomingByte);
        } catch (SerialPortException e) {
            throw new SerialException(e);
        }
    }
   
    public int read() {
        if (buffer.isEmpty()) {
            return -1;
        }
        return buffer.remove(0) & 0xFF;
    }
    
    public void clear() {
        buffer.clear();
    }
    
    public int available() {
        return buffer.size();
    }

    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR()) {
            int bytesAvailable = event.getEventValue();
            try {
                byte available[] = port.readBytes(bytesAvailable);
                for (int i = 0; i < bytesAvailable; i++) {
                    buffer.add(available[i]);

                    for (ISerialListener listener : listeners) {
                        listener.onDataReceived(this);
                    }
                }
            } catch (SerialPortException e) {
                logger.error("Error encountered handling serial event: " + e);
            }
        }
    }
    
}
