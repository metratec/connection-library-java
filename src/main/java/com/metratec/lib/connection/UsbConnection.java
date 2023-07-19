package com.metratec.lib.connection;

/*******************************************************************************
 * Copyright (c) 2023 metraTec.com
 *
 * All rights reserved.
 *******************************************************************************/

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jd2xx.JD2XX;
import jd2xx.JD2XX.DeviceInfo;
import jd2xx.JD2XXInputStream;
import jd2xx.JD2XXOutputStream;

/**
 * A connection to a usb device
 *
 * @author man
 */
@SuppressWarnings("PMD.EmptyCatchBlock")
public class UsbConnection extends SerialConnection {

  private final Logger logger = LoggerFactory.getLogger(UsbConnection.class);
  /** output stream */
  protected OutputStream outputstream = null;
  /** output stream */
  protected InputStream inputstream = null;

  private String usbDeviceSerialNumber;


  private JD2XX jd = null;
  private int recvTimeout = 500;
  private int sendTimeout = 500;
  private int connectTimeout = 1000;

  private static class VidPid {
    public int vid;
    public int pid;

    VidPid(int vid, int pid) {
      this.vid = vid;
      this.pid = pid;
    }
  }

  /**
   * List of non-standard VID/PID pairs that must be registered with
   * {@link JD2XX#setVIDPID(int,int)}.
   */
  private static List<VidPid> additionalVidPids = new ArrayList<>(4);

  static {
    /*
     * Initialize list of additional VIDs/PIDs with the ones used by metraTec devices.
     */
    for (int pid = 0xB000; pid < 0xB004; pid++) {
      addVIDPID(0x0403, pid);
    }
  }

  /**
   * Construct a new USBConnection object
   *
   * @param usbDevice DeviceInfo
   */
  public UsbConnection(DeviceInfo usbDevice) {
    this(usbDevice.serial, 115200, 8, 1, 0, 0);
  }

  /**
   * Construct a new USBConnection object
   *
   * @param usbDevice DeviceInfo
   * @param baudrate baud rate
   */
  public UsbConnection(DeviceInfo usbDevice, int baudrate) {
    this(usbDevice.serial, baudrate, 8, 1, 0, 0);
  }

  /**
   * Construct a new USBConnection object
   *
   * @param usbDevice DeviceInfo
   * @param baudrate baud rate of the connected device, can read with the {@link #getBaudrate()}
   *        method.
   *
   * @param dataBit Number of data bits, can read with the {@link #getDataBit()} method.
   *
   * @param stopBit Value of StopBits, can read with the {@link #getStopBit()} method. A value
   *        larger 2 means 1.5 stopbits.
   *
   * @param parity Parity-Bit (0 for no, 1 for ODD, 2 for EVEN, 3 for MARK, 4 for SPACE), can read
   *        with the {@link #getParity()} method.
   *
   * @param flowControl Flow Control (0 for no, ..), can read with the {@link #getFlowControl()}
   *        method.
   */
  public UsbConnection(DeviceInfo usbDevice, int baudrate, int dataBit, int stopBit, int parity,
      int flowControl) {
    this(usbDevice.serial, baudrate, dataBit, stopBit, parity, flowControl);
  }

  /**
   * Construct a new USBConnection object
   *
   * @param usbDeviceSerialNumber usb serial number
   */
  public UsbConnection(String usbDeviceSerialNumber) {
    this(usbDeviceSerialNumber, 115200, 8, 1, 0, 0);
  }

  /**
   * Construct a new USBConnection object
   *
   * @param usbDeviceSerialNumber usb serial number
   * @param baudrate baud rate of the connected device, can read with the {@link #getBaudrate()}
   *        method.
   */
  public UsbConnection(String usbDeviceSerialNumber, int baudrate) {
    this(usbDeviceSerialNumber, baudrate, 8, 1, 0, 0);
  }

  /**
   * Construct a new USBConnection object
   *
   * @param usbDeviceSerialNumber usb serial number
   *
   * @param baudrate baud rate of the connected device, can read with the {@link #getBaudrate()}
   *        method.
   *
   * @param dataBit Number of data bits, can read with the {@link #getDataBit()} method.
   *
   * @param stopBit Value of StopBits, can read with the {@link #getStopBit()} method. A value
   *        larger 2 means 1.5 stopbits.
   *
   * @param parity Parity-Bit (0 for no, 1 for ODD, 2 for EVEN, 3 for MARK, 4 for SPACE), can read
   *        with the {@link #getParity()} method.
   *
   * @param flowControl Flow Control (0 for no, ..), can read with the {@link #getFlowControl()}
   *        method.
   */
  public UsbConnection(String usbDeviceSerialNumber, int baudrate, int dataBit, int stopBit,
      int parity, int flowControl) {
    super(baudrate, dataBit, stopBit, parity, flowControl);
    this.usbDeviceSerialNumber = usbDeviceSerialNumber;
  }

  /**
   * Add VID/PID pair to the list of supported USB devices.
   * <p>
   * Depending on the operating system, all FTDI devices might be usable by default. On some systems
   * like Linux and OS X however, only the default FTDI VID/PIDs and all additional non-standard
   * VID/PIDs used by metraTec devices are supported by default. Therefore in order to write
   * platform-independent code, you should call this method for every non-standard and non-metraTec
   * VID/PID you intend to use (e.g. HAMEG power supply PIDs).
   *
   * @param vid Vendor ID
   * @param pid Product ID
   */
  public static void addVIDPID(int vid, int pid) {
    additionalVidPids.add(new VidPid(vid, pid));
  }

  /**
   * Constructor for JD2XX instances with common error handling.
   *
   * This cannot be implemented as a derivation of the JD2XX class because Java does not allow us to
   * catch exceptions from a super() call.
   */
  static private JD2XX initJD2XX() throws CommConnectionException {
    JD2XX ret;

    /*
     * First time instantiation calls the static constructor, which tries to load the JNIs
     */
    try {
      ret = new JD2XX();
    } catch (UnsatisfiedLinkError e) {
      throw new CommConnectionException(NO_LIBRARY_FOUND, e.getMessage());
    } catch (NoClassDefFoundError e) {
      throw new CommConnectionException(NO_LIBRARY_FOUND, e.getMessage());
    }

    return ret;
  }

  /**
   *
   * @return ArrayList with Device Infos
   * @throws CommConnectionException possible Errorcodes:
   *         <ul>
   *         <li>NO_LIBRARY_FOUND</li>
   *         <li>UNHANDLED_ERROR</li>
   *         </ul>
   */
  static public ArrayList<DeviceInfo> getUSBDevices() throws CommConnectionException {
    Set<String> serials = new HashSet<>();
    ArrayList<DeviceInfo> devices = new ArrayList<>();

    JD2XX jdtmp = initJD2XX();

    /*
     * Try once for each metraTec VID/PID combination. This is necessary since setVIDPID() will only
     * register a single combination on OS X.
     */
    for (VidPid pair : additionalVidPids) {
      try {
        jdtmp.setVIDPID(pair.vid, pair.pid);
      } catch (IOException e) {
      }

      int number = jdtmp.createDeviceInfoList();

      for (int i = 0; i < number; i++) {
        try {
          DeviceInfo device = jdtmp.getDeviceInfoDetail(i);
          if (!serials.contains(device.serial)) {
            serials.add(device.serial);
            devices.add(device);
          }
        } catch (IOException e) {
        }
      }
    }

    return devices;
  }

  /**
   *
   * @return LinkedList(String) with available devices
   * @throws CommConnectionException possible Errorcodes:
   *         <ul>
   *         <li>NO_LIBRARY_FOUND</li>
   *         <li>UNHANDLED_ERROR</li>
   *         </ul>
   */
  public static LinkedList<String> getUSBDevicesList() throws CommConnectionException {
    LinkedList<String> ret = new LinkedList<>();

    for (DeviceInfo device : getUSBDevices()) {
      if (!device.description.isEmpty()) {
        ret.add(device.description);
      }
    }

    return ret;
  }

  @Override
  public void disconnect() throws CommConnectionException {
    if (inputstream != null) {
      try {
        jd.close();
        inputstream = null;
        outputstream = null;
      } catch (IOException e) {
        throw new CommConnectionException(UNHANDLED_ERROR, e.getMessage());
      }
    }
  }

  @Override
  public void connect() throws CommConnectionException {
    boolean connected = false;

    if (inputstream != null) {
      /* stream already opened */
      return;
    }

    /* already throws a CommConnectionException */
    jd = initJD2XX();

    /*
     * Try once for each metraTec VID/PID combination. This is necessary since setVIDPID() will only
     * register a single combination on OS X.
     */
    for (VidPid pair : additionalVidPids) {
      try {
        jd.setVIDPID(pair.vid, pair.pid);
      } catch (IOException e) {
      }

      try {
        jd.openBySerialNumber(usbDeviceSerialNumber);
        connected = true;
        break;
      } catch (IOException e) {
        if (e.getMessage().startsWith("invalid handle")) {
          throw new CommConnectionException(DEVICE_IN_USE, e.getMessage());
        }
      }
    }
    if (!connected) {
      throw new CommConnectionException(NO_DEVICES_FOUND,
          "No device found with serial number " + usbDeviceSerialNumber);
    }

    try {
      try {
        jd.setBaudRate(getBaudrate());
      } catch (IOException e) {
        throw new CommConnectionException(USB_SET_BAUDRATE, e.getMessage());
      }

      try {
        int jd2xxStopBits;

        switch (getStopBit()) {
          case 1:
            jd2xxStopBits = JD2XX.STOP_BITS_1;
            break;
          case 2:
            jd2xxStopBits = JD2XX.STOP_BITS_2;
            break;
          default:
            jd2xxStopBits = JD2XX.STOP_BITS_1_5;
            break;
        }

        /*
         * NOTE: RS232 parity values correspond with the JD2XX.PARITY_... constants.
         */
        jd.setDataCharacteristics(getDataBit(), jd2xxStopBits, getParity());
      } catch (IOException e) {
        throw new CommConnectionException(USB_SET_DATA_CHARACTERISTICS, e.getMessage());
      }

      try {
        /*
         * Assume that flowControl values correspond with the JD2XX_FLOW_... constants (e.g. 0 for
         * no flow control)
         */
        jd.setFlowControl(getFlowControl(), 0, 0);
      } catch (IOException e) {
        throw new CommConnectionException(USB_SET_FLOWCONTROL, e.getMessage());
      }

      try {
        jd.setTimeouts(recvTimeout, sendTimeout);
      } catch (IOException e) {
        throw new CommConnectionException(USB_SET_TIMEOUTS, e.getMessage());
      }
    } catch (CommConnectionException e) {
      /*
       * Clean up: close connection
       */
      try {
        jd.close();
      } catch (IOException ioex) {
        throw new CommConnectionException(e.getErrorCode(), ioex.getMessage());
      }

      /* forward exception */
      throw e;
    }

    inputstream = new BufferedInputStream(new JD2XXInputStream(jd));
    outputstream = new BufferedOutputStream(new JD2XXOutputStream(jd));
  }

  @Override
  public InputStream getInputStream() {
    return inputstream;
  }

  @Override
  public OutputStream getOutputStream() {
    return outputstream;
  }

  @Override
  public boolean isConnected() {
    return inputstream != null;
  }

  @Override
  public int recv() throws CommConnectionException {
    try {
      return inputstream.read();
    } catch (NullPointerException e) {
      throw new CommConnectionException(NOT_INITIALISED, "not initialize");
    } catch (IOException e) {
      if (e.getMessage().equals("io error")) {
        // means no Data Available
        return -1;
      } else if (e.getMessage().equals("io error (4)")) {
        throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
      } else {
        throw new CommConnectionException(UNHANDLED_ERROR, e.getMessage());
      }
    }
  }

  @Override
  public void send(byte[] senddata) throws CommConnectionException {
    if (getLogger().isTraceEnabled()) {
      getLogger().trace("{} send {}", toString(), new String(senddata));
    }
    try {
      outputstream.write(senddata);
      // _outputstream.write(senddata, 0, senddata.length);
      outputstream.flush();
    } catch (NullPointerException e) {
      if (senddata == null) {
        throw new CommConnectionException(WRONG_PARAMETER, "data are null");
      } else {
        throw new CommConnectionException(NOT_INITIALISED, "not initialize");
      }
    } catch (IOException e) {
      if (e.getMessage().equals("io error (4)")) {
        throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
      } else {
        throw new CommConnectionException(UNHANDLED_ERROR, e.getMessage());
      }
    }
  }

  @Override
  public int dataAvailable() throws CommConnectionException {
    try {
      return jd.getQueueStatus();
    } catch (IOException e) {
      if (e.getMessage().startsWith("invalid handle (1)")) {
        throw new CommConnectionException(NOT_INITIALISED, "not initialize");
      } else {
        throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
      }
    }
  }

  @Override
  public Hashtable<String, Object> getInfo() {
    Hashtable<String, Object> info = new Hashtable<>();
    info.put("type", "usb");
    try {
      DeviceInfo deviceinfo = jd.getDeviceInfo();
      info.put("device", deviceinfo.description);
      info.put("serialnumber", deviceinfo.serial);
      info.put("baudrate", getBaudrate());
    } catch (IOException e) {
      if (logger.isDebugEnabled()) {
        String message = this.toString() + " error get device info " + e.getMessage();
        logger.debug(message, logger.isTraceEnabled() ? e : null);
      }
    }
    return info;
  }

  @Override
  public void setSettings(Hashtable<String, String> settings) {
    String baudrate = settings.get("baudrate");
    if (null != baudrate) {
      try {
        setBaudrate(Integer.parseInt(baudrate));
      } catch (NumberFormatException e) {
        throw new NumberFormatException("Baudrate should be a number");
      }
    }
    // String device = settings.get("device");
    // if(null != device)
    // {
    // _usbDevice = device;
    // }
  }

  @Override
  public int getRecvTimeout() {
    return recvTimeout;
  }

  @Override
  public void setRecvTimeout(int timeout) throws CommConnectionException {
    if (0 >= timeout) {
      throw new CommConnectionException(SET_CONFIGURATION, "Timeout must be greater than 0");
    }
    recvTimeout = timeout;
    if (jd != null) {
      try {
        jd.setTimeouts(recvTimeout, sendTimeout);
      } catch (IOException e) {
        throw new CommConnectionException(SET_CONFIGURATION, e.getMessage());
      }
    }
  }

  /**
   * @return the send timeout
   */
  public int getSendTimeout() {
    return sendTimeout;
  }

  /**
   * Set the send timeout
   * 
   * @param timeout send timeout
   * @throws CommConnectionException if an error occurs
   */
  public void setSendTimeout(int timeout) throws CommConnectionException {
    if (0 >= timeout) {
      throw new CommConnectionException(SET_CONFIGURATION, "Timeout must be greater than 0");
    }
    sendTimeout = timeout;
    if (jd == null) {
      return;
    }
    try {
      jd.setTimeouts(recvTimeout, sendTimeout);
    } catch (IOException e) {
      throw new CommConnectionException(SET_CONFIGURATION, e.getMessage());
    }

  }

  @Override
  public void setConnectionTimeout(int time) {
    connectTimeout = time;
  }

  @Override
  public int getConnectionTimeout() {
    return connectTimeout;
  }

  /**
   * Get the USB device's serial number.
   *
   * @return the serial number.
   */
  public String getSerialNumber() {
    return usbDeviceSerialNumber;
  }

  /**
   * Set the USB device's serial number.
   *
   * @param serialNumber the serial number.
   */
  public void setSerialNumber(String serialNumber) {
    usbDeviceSerialNumber = serialNumber;
  }

  /**
   * @return the jd2xx
   */
  public JD2XX getJD2XX() {
    return jd;
  }

  @Override
  public String toString() {
    return usbDeviceSerialNumber;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.metratec.lib.connection.ICommConnection#recv(byte[], int, int)
   */
  @Override
  public void recv(byte[] b, int off, int len) throws CommConnectionException {
    try {
      super.recv(b, off, len);
    } finally {
      if (getLogger().isTraceEnabled()) {
        getLogger().trace("{} recv {}", toString(), new String(b));
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.metratec.lib.connection.ICommConnection#receive(int[])
   */
  @Override
  public StringBuilder receive(int... terminators) throws CommConnectionException {
    StringBuilder s = super.receive(terminators);
    if (getLogger().isTraceEnabled()) {
      getLogger().trace("{} recv {}", toString(), s.toString());
    }
    return s;
  }

  private Logger getLogger() {
    return logger;
  }
}
