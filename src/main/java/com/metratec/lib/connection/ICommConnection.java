package com.metratec.lib.connection;

/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Hashtable;

/**
 * Connection interface
 *
 * @author Matthias Neumann (neumann@metratec.com)
 */
// NOTE: this may be an Interface in Java 8, using "default" methods
public abstract class ICommConnection {
  /*
   * Error Codes. If you touch them, you should also edit the descriptions in
   * CommConnectionException.getErrorDescription().
   */

  // Serial - connect
  /**
   * specified serial port not exist
   */
  public static final int SERIAL_PORT_NOT_EXIST = 0x00000011; // Specified Serial port " + _portName
                                                              // + " does not exist, available " +
                                                              // tmp
  /**
   * could not set interface parameter
   */
  public static final int SERIAL_PARAMETER_NOT_SET = 0x00000012; // Could not set interface
                                                                 // parameter
  /**
   * no access to the input/output stream
   */
  public static final int SERIAL_NO_ACCESS = 0x00000013; // No access to Output-/InputStream
  /**
   * could not initialize the rs232 connection
   */
  public static final int SERIAL_NOT_INITIALISED = 0x00000014; // Could not initialise RS232
  // connection

  // USB
  /**
   * Set baudrate for the usb connection failed
   */
  public static final int USB_SET_BAUDRATE = 0x00000022;
  /**
   * set data characteristics for the usb connection failed
   */
  public static final int USB_SET_DATA_CHARACTERISTICS = 0x00000023;
  /**
   * set flow control for the usb connection failed
   */
  public static final int USB_SET_FLOWCONTROL = 0x00000024;
  /**
   * set timeouts for the usb connection failed
   */
  public static final int USB_SET_TIMEOUTS = 0x00000025;

  // ETHERNET
  /**
   * Ethernet connection timeout
   */
  public static final int ETHERNET_TIMEOUT = 0x00000031;
  /**
   * host not found
   */
  public static final int ETHERNET_UNKNOWN_HOST = 0x00000032;

  // USER
  /**
   * for individual use
   */
  public static final int USER_ERRORCODE_01 = 0x00000041;
  /**
   * for individual use
   */
  public static final int USER_ERRORCODE_02 = 0x00000042;
  /**
   * for individual use
   */
  public static final int USER_ERRORCODE_03 = 0x00000043;
  /**
   * for individual use
   */
  public static final int USER_ERRORCODE_04 = 0x00000044;
  /**
   * for individual use
   */
  public static final int USER_ERRORCODE_05 = 0x00000045;

  // General
  /**
   * error which is not specifically defined
   */
  public static final int UNHANDLED_ERROR = 0x00000001;
  /**
   * could not find the needed java library for the connection
   */
  public static final int NO_LIBRARY_FOUND = 0x00000002;
  /**
   * no devices for a connection found
   */
  public static final int NO_DEVICES_FOUND = 0x00000003;
  /**
   * device is in use, could no connect
   */
  public static final int DEVICE_IN_USE = 0x00000004;
  /**
   * wrong parameter
   */
  public static final int WRONG_PARAMETER = 0x00000005;
  /**
   * lost the connection
   */
  public static final int CONNECTION_LOST = 0x00000006;
  /**
   * error while setting parameters
   */
  public static final int SET_CONFIGURATION = 0x00000009;
  /**
   * connection is not initializes
   */
  public static final int NOT_INITIALISED = 0x00000007;
  /**
   * no data arrive during the receive timeout
   */
  public static final int RECV_TIMEOUT = 0x00000008;
  /**
   * not available - method is not available
   */
  public static final int NOT_AVAILABLE = 0x0000000A;

  /**
   * This method opens a connection. Parameters are passed through the constructor
   *
   * @throws CommConnectionException possible Errorcodes:
   *         <ul>
   *         <li>for TCP Connection:
   *         <ul>
   *         <li>ETHERNET_UNKNOWN_HOST</li>
   *         <li>ETHERNET_TIMEOUT</li>
   *         <li>WRONG_PARAMETER</li>
   *         </ul>
   *         </li>
   *         <li>for USB Connection:
   *         <ul>
   *         <li>DEVICE_IN_USE</li>
   *         <li>NO_LIBRARY_FOUND</li>
   *         <li>NO_DEVICES_FOUND</li>
   *         <li>USB_SET_BAUDRATE</li>
   *         <li>USB_SET_DATA_CHARACTERISTICS</li>
   *         <li>USB_SET_FLOWCONTROL</li>
   *         <li>USB_SET_TIMEOUTS</li>
   *         </ul>
   *         </li>
   *         <li>for RS232 Connection:
   *         <ul>
   *         <li>NO_LIBRARY_FOUND</li>
   *         <li>SERIAL_PORT_NOT_EXIST</li>
   *         <li>SERIAL_PARAMETER_NOT_SET</li>
   *         <li>DEVICE_IN_USE</li>
   *         <li>SERIAL_NO_ACCESS</li>
   *         <li>SERIAL_NO_INITIALISE</li>
   *         </ul>
   *         </li>
   *         </ul>
   */
  public abstract void connect() throws CommConnectionException;

  /**
   * Closes the communication interface
   *
   * @throws CommConnectionException possible Errorcodes:
   *         <ul>
   *         <li>for TCP Connection:
   *         <ul>
   *         <li>UNHANDLED_ERROR</li>
   *         </ul>
   *         </li>
   *         <li>for USB Connection:
   *         <ul>
   *         <li>UNHANDLED_ERROR</li>
   *         </ul>
   *         </li>
   *         </ul>
   */
  public abstract void disconnect() throws CommConnectionException;

  /**
   * Sends data to the connected device
   *
   * @param senddata data/command send to the the connected device
   *
   * @throws CommConnectionException possible Errorcodes:
   *         <ul>
   *         <li>for TCP Connection:
   *         <ul>
   *         <li>WRONG_PARAMETER</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         <li>for USB Connection:
   *         <ul>
   *         <li>WRONG_PARAMETER</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         <li>for RS232 Connection:
   *         <ul>
   *         <li>WRONG_PARAMETER</li>
   *         <li>CONNECTION_LOST</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         </ul>
   */
  public void send(String senddata) throws CommConnectionException {
    send(senddata.getBytes(Charset.forName("ISO-8859-1")));
  }

  /**
   * Sends data to the connected device
   *
   * @param senddata data/command send to the connected device
   * @throws CommConnectionException possible Errorcodes:
   *         <ul>
   *         <li>for TCP Connection:
   *         <ul>
   *         <li>WRONG_PARAMETER</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         <li>for USB Connection:
   *         <ul>
   *         <li>WRONG_PARAMETER</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         <li>for RS232 Connection:
   *         <ul>
   *         <li>WRONG_PARAMETER</li>
   *         <li>CONNECTION_LOST</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         </ul>
   */
  public abstract void send(byte[] senddata) throws CommConnectionException;

  /**
   * Receives data from the connected device until one of the terminator signs is found.
   *
   * @param terminators A list of terminator signs. Note that this may be a list of parameters or an
   *        array.
   * @return String with the data, including the termination sign
   * @throws CommConnectionException possible Errorcodes:
   *         <ul>
   *         <li>for TCP Connection:
   *         <ul>
   *         <li>CONNECTION_LOST</li>
   *         <li>RECV_TIMEOUT</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         <li>for USB Connection:
   *         <ul>
   *         <li>CONNECTION_LOST</li>
   *         <li>RECV_TIMEOUT</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         <li>for RS232 Connection:
   *         <ul>
   *         <li>CONNECTION_LOST</li>
   *         <li>RECV_TIMEOUT</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         </ul>
   */
  public String recv(int... terminators) throws CommConnectionException {
    return receive(terminators).toString();
  }

  /**
   * Receives data from the connected device until one of the terminator signs is found.
   *
   * The result is returned as a StringBuilder for performance reasons.
   *
   * @param terminators A list of terminator signs. Note that this may be a list of parameters or an
   *        array.
   * @return a StringBuilder object, including the termination sign
   * @throws CommConnectionException possible Errorcodes:
   *         <ul>
   *         <li>for TCP Connection:
   *         <ul>
   *         <li>CONNECTION_LOST</li>
   *         <li>RECV_TIMEOUT</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         <li>for USB Connection:
   *         <ul>
   *         <li>CONNECTION_LOST</li>
   *         <li>RECV_TIMEOUT</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         <li>for RS232 Connection:
   *         <ul>
   *         <li>CONNECTION_LOST</li>
   *         <li>RECV_TIMEOUT</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         </ul>
   */
  public StringBuilder receive(int... terminators) throws CommConnectionException {
    StringBuilder data = new StringBuilder();
    Arrays.sort(terminators);
    while (true) {
      int c = recv();

      if (c < 0) {
        throw new CommConnectionException(RECV_TIMEOUT, data.toString());
      }

      data.append((char) c);

      if (Arrays.binarySearch(terminators, c) >= 0) {
        return data;
      }
    }
  }

  /**
   * Receives a single byte.
   *
   * @return byte or -1 if no Data available (timeout)
   * @throws CommConnectionException possible Errorcodes:
   *         <ul>
   *         <li>for TCP Connection:
   *         <ul>
   *         <li>CONNECTION_LOST</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         <li>for USB Connection:
   *         <ul>
   *         <li>CONNECTION_LOST</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         <li>for RS232 Connection:
   *         <ul>
   *         <li>CONNECTION_LOST</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         </ul>
   */
  public abstract int recv() throws CommConnectionException;

  /**
   * Receives until buffer is filled or timeout occurrs.
   *
   * @param b Byte array to fill
   * @param off Offset into array (it is filled beginning with the offset)
   * @param len Number of bytes to read.
   * @throws CommConnectionException possible Errorcodes:
   *         <ul>
   *         <li>for TCP Connection:
   *         <ul>
   *         <li>CONNECTION_LOST</li>
   *         <li>RECV_TIMEOUT</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         <li>for USB Connection:
   *         <ul>
   *         <li>CONNECTION_LOST</li>
   *         <li>RECV_TIMEOUT</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         <li>for RS232 Connection:
   *         <ul>
   *         <li>CONNECTION_LOST</li>
   *         <li>RECV_TIMEOUT</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         </ul>
   */
  public void recv(byte[] b, int off, int len) throws CommConnectionException {
    while (len > 0) {
      int c = recv();

      if (c < 0) {
        throw new CommConnectionException(RECV_TIMEOUT);
      }

      b[off++] = (byte) c;
      len--;
    }
  }

  /**
   * Receive bytes until buffer is filled or timeout occurrs.
   *
   * @param b Buffer to fill.
   * @throws CommConnectionException possible Errorcodes:
   *         <ul>
   *         <li>for TCP Connection:
   *         <ul>
   *         <li>CONNECTION_LOST</li>
   *         <li>RECV_TIMEOUT</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         <li>for USB Connection:
   *         <ul>
   *         <li>CONNECTION_LOST</li>
   *         <li>RECV_TIMEOUT</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         <li>for RS232 Connection:
   *         <ul>
   *         <li>CONNECTION_LOST</li>
   *         <li>RECV_TIMEOUT</li>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         </ul>
   */
  public void recv(byte[] b) throws CommConnectionException {
    recv(b, 0, b.length);
  }

  /**
   *
   * @return true if connected else false
   */
  public abstract boolean isConnected();

  /**
   * Gets Device Informations
   *
   * @return Hashtable with the information
   */
  public abstract Hashtable<String, Object> getInfo();

  /**
   * Sets connections settings
   *
   * @param settings Hashtable with the special settings
   */
  public abstract void setSettings(Hashtable<String, String> settings);

  /**
   * 
   * @return the {@link InputStream} of this connection
   */
  public abstract InputStream getInputStream();

  /**
   * 
   * @return the {@link OutputStream} of this connection
   */
  public abstract OutputStream getOutputStream();

  /**
   * @return number of available signs
   * @throws CommConnectionException possible Errorcodes:
   *         <ul>
   *         <li>for all connections
   *         <ul>
   *         <li>CONNECTION_LOST</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   *         </li>
   *         </ul>
   */
  public abstract int dataAvailable() throws CommConnectionException;

  /**
   * set the receive timeout for read data, if the timeout expires an CommConnectionException is
   * raised with errorcode RECV_TIMEOUT, the connection is still valid
   *
   * @param timeout time in milliseconds
   * @throws CommConnectionException possible Errorcodes:
   *         <ul>
   *         <li>for all connections
   *         <ul>
   *         <li>SET_CONFIGURATION</li>
   *         </ul>
   *         </li>
   *         </ul>
   */
  public abstract void setRecvTimeout(int timeout) throws CommConnectionException;

  /**
   * @return the receive timeout
   */
  public abstract int getRecvTimeout();

  /**
   * Sets the waiting time for the connection
   *
   * @param timeout connection timeout
   */
  public abstract void setConnectionTimeout(int timeout);

  /**
   * @return the waiting time for the connection
   */
  public abstract int getConnectionTimeout();
}
