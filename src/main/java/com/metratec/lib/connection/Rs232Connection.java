package com.metratec.lib.connection;

/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH All rights reserved.
 *******************************************************************************/

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

/**
 * A connection to a rs232 device
 *
 * @author man
 *
 */
public class Rs232Connection extends SerialConnection {

  private final Logger logger = LoggerFactory.getLogger(Rs232Connection.class);
  /** output stream */
  protected OutputStream outputstream = null;
  /** input stream */
  protected InputStream inputstream = null;
  // protected boolean _init = false;

  private CommPortIdentifier portId = null;
  private SerialPort serialPort = null;
  private String portName;
  private int recvTimeout = 500;
  private int connectTimeout = 1000;

  /**
   * Construct a new RS232Connection object, with the given parameters
   *
   * @param portName Name of the used ports (COM1,COM2, /dev/ttyS0, /dev/ttyS1, ...), can read with
   *        the {@link #getPortName()} method.
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
  public Rs232Connection(String portName, int baudrate, int dataBit, int stopBit, int parity,
      int flowControl) {
    super(baudrate, dataBit, stopBit, parity, flowControl);
    this.portName = portName;
  }

  /**
   * get the available Serial Ports
   *
   * @return String array with available ports
   *
   * @throws CommConnectionException possible Errorcodes:
   *         <ul>
   *         <li>NO_LIBRARY_FOUND if no rxtx libary found</li>
   *         </ul>
   */
  public static String[] getSerialPorts() throws CommConnectionException {
    try {
      Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
      StringBuilder tmp = new StringBuilder();
      CommPortIdentifier serialPortId;
      while (portList.hasMoreElements()) {
        serialPortId = (CommPortIdentifier) portList.nextElement();
        if (serialPortId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
          tmp.append(serialPortId.getName());
          tmp.append("\r");
        }
      }
      if (0 == tmp.length()) {
        return new String[0];
      } else {
        return tmp.toString().split("\r");
      }
    } catch (UnsatisfiedLinkError e) {
      throw new CommConnectionException(NO_LIBRARY_FOUND, e.getMessage());
    } catch (NoClassDefFoundError e) {
      throw new CommConnectionException(NO_LIBRARY_FOUND, e.getMessage());
    }
  }

  @Override
  public void connect() throws CommConnectionException {
    try {
      // NOTE: It is necessary to fetch all available port identifiers before trying to get the
      // specific identifier. Otherwise a reconnection attempt after unplugging the connected port
      // results in a crash as the RXTX library wrongly believes the unplugged port still exists.
      CommPortIdentifier.getPortIdentifiers();
      // Obtains a CommPortIdentifier object by using a port name
      portId = CommPortIdentifier.getPortIdentifier(portName);
      // open the serial port - (String,int) (owner of this port,time in
      // milliseconds to block waiting for port open)
      serialPort = portId.open("metratec", connectTimeout);

      serialPort.enableReceiveTimeout(recvTimeout);

      outputstream = new BufferedOutputStream(serialPort.getOutputStream());

      inputstream = new BufferedInputStream(serialPort.getInputStream());

      serialPort.setSerialPortParams(getBaudrate(), getDataBit(), getStopBit(), getParity());

      // _init = true;
    } catch (UnsatisfiedLinkError e) {
      throw new CommConnectionException(NO_LIBRARY_FOUND, e.getMessage());
    } catch (NoClassDefFoundError e) {
      throw new CommConnectionException(NO_LIBRARY_FOUND, e.getMessage());
    } catch (NoSuchPortException e) {
      Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
      StringBuilder tmp = new StringBuilder();
      CommPortIdentifier serialPortId;
      while (portList.hasMoreElements()) {
        serialPortId = (CommPortIdentifier) portList.nextElement();
        if (serialPortId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
          tmp.append(serialPortId.getName());
          tmp.append(" ");
        }
      }
      throw new CommConnectionException(SERIAL_PORT_NOT_EXIST,
          "Specified Serial port " + portName + " does not exist, available " + tmp.toString());
      // java.lang.Exception
    } catch (UnsupportedCommOperationException e) {
      throw new CommConnectionException(SERIAL_PARAMETER_NOT_SET,
          "Could not set interface parameter " + e.getMessage());
      // java.lang.Exception
    } catch (PortInUseException e) {
      throw new CommConnectionException(DEVICE_IN_USE, "Serial Port in use " + e.getMessage());
      // java.lang.Exception
    } catch (IOException e) {
      throw new CommConnectionException(SERIAL_NO_ACCESS,
          "No access to Output-/InputStream " + e.getMessage());
      // java.io.IOException
    } catch (Exception e) {
      throw new CommConnectionException(SERIAL_NOT_INITIALISED,
          "Could not initialise RS232 connection " + e.getMessage());
    }

  }

  @Override
  public void disconnect() throws CommConnectionException {
    if (serialPort != null) {
      serialPort.close();

    }
    serialPort = null;
    inputstream = null;
    outputstream = null;
  }

  /**
   *
   * @return the Name of the used serial port
   */
  public String getPortName() {
    return portName;
  }

  /**
   * Sets the used serial port
   *
   * @param portName Name of the serial port
   */
  public void setPortName(String portName) {
    this.portName = portName;
  }

  @Override
  public InputStream getInputStream() {
    return inputstream;
  }

  @Override
  public OutputStream getOutputStream() {
    return outputstream;
  }

  /**
   * @return the {@link SerialPort}
   */
  public SerialPort getSerialPort() {
    return serialPort;
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
      if (null == e.getMessage()) {
        throw new CommConnectionException(CONNECTION_LOST, "Input/output error");
      } else {
        if (e.getMessage().equals("No error in readByte")) {
          throw new CommConnectionException(CONNECTION_LOST, "Input/output error");
        } else {
          throw new CommConnectionException(UNHANDLED_ERROR, e.getMessage());
        }
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
      outputstream.flush();
    } catch (NullPointerException e) {
      if (senddata == null) {
        throw new CommConnectionException(WRONG_PARAMETER, "data are null");
      } else {
        throw new CommConnectionException(NOT_INITIALISED, "not initialize");
      }
    } catch (IOException e) {
      if (e.getMessage().startsWith("Input/output error")) {
        serialPort.close();
        throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
      } else {
        throw new CommConnectionException(UNHANDLED_ERROR, e.getMessage());
      }
    }
  }

  @Override
  public int dataAvailable() throws CommConnectionException {
    try {
      return inputstream.available();
    } catch (NullPointerException e) {
      throw new CommConnectionException(NOT_INITIALISED, "not initialize");
    } catch (IOException e) {
      throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
    }

  }

  @Override
  public Hashtable<String, Object> getInfo() {
    Hashtable<String, Object> info = new Hashtable<>();
    info.put("type", "rs232");
    info.put("baudrate", getBaudrate());
    info.put("portname", portName);
    info.put("databit", getDataBit());
    info.put("stopbit", getStopBit());
    info.put("parity", getParity());
    info.put("flowcontrol", getFlowControl());

    return info;
  }

  @Override
  public void setSettings(Hashtable<String, String> settings) {
    String databit = settings.get("databit");
    if (null != databit) {
      try {
        setDataBit(Integer.parseInt(databit));
      } catch (NumberFormatException e) {
        throw new NumberFormatException("Databit should be a number");
      }
    }
    String stopbit = settings.get("stopbit");
    if (null != stopbit) {
      try {
        setStopBit(Integer.parseInt(stopbit));
      } catch (NumberFormatException e) {
        throw new NumberFormatException("Stopbit should be a number");
      }
    }
    String parity = settings.get("parity");
    if (null != parity) {
      try {
        setParity(Integer.parseInt(parity));
      } catch (NumberFormatException e) {
        throw new NumberFormatException("Parity should be a number");
      }
    }
    String flowcontrol = settings.get("flowcontrol");
    if (null != flowcontrol) {
      try {
        setFlowControl(Integer.parseInt(flowcontrol));
      } catch (NumberFormatException e) {
        throw new NumberFormatException("Flowcontrol should be a number");
      }
    }
    String baudrate = settings.get("baudrate");
    if (null != baudrate) {
      try {
        setBaudrate(Integer.parseInt(baudrate));
      } catch (NumberFormatException e) {
        throw new NumberFormatException("Baudrate should be a number");
      }
    }
    String portname = settings.get("portname");
    if (null != portname) {
      this.portName = portname;
    }
  }

  @Override
  public int getRecvTimeout() {
    return recvTimeout;
  }

  @Override
  public void setRecvTimeout(int timeout) throws CommConnectionException {
    recvTimeout = timeout;
    if (serialPort != null) {
      try {
        serialPort.enableReceiveTimeout(recvTimeout);
      } catch (UnsupportedCommOperationException e) {
        throw new CommConnectionException(SET_CONFIGURATION, e.getMessage());
      }
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

  @Override
  public String toString() {
    return getPortName();
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
