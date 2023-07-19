/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/
package test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.metratec.lib.connection.CommConnectionException;
import com.metratec.lib.connection.ICommConnection;
import jd2xx.JD2XX;
import jd2xx.JD2XX.DeviceInfo;
import jd2xx.JD2XXInputStream;
import jd2xx.JD2XXOutputStream;

/**
 * Implementation of the USB interface
 *
 * @author man
 * @version 1.3
 */
public class USBConnectionTest extends ICommConnection {
  private final Logger logger = LoggerFactory.getLogger(USBConnectionTest.class);

  private JD2XXOutputStream outputstream = null;
  private JD2XXInputStream inputstream = null;
  private int baudrate = 0;
  private String usbDeviceSerialNumber = "";
  private JD2XX jd = null;
  private int recvTimeout = 500;
  private int connectTimeout = 1000;

  /**
   * Construct a new USBConnection object
   * 
   * @param usbDevice DeviceInfo
   * @param baudrate baud rate
   */
  public USBConnectionTest(DeviceInfo usbDevice, int baudrate) {
    // (String portName, int baudrate, int dataBit,
    // int stopBit, int parity, int flowControl)
    this.baudrate = baudrate;
    this.usbDeviceSerialNumber = usbDevice.serial;

  }

  /**
   * Construct a new USBConnection object
   * 
   * @param usbDevice DeviceInfo
   */
  public USBConnectionTest(DeviceInfo usbDevice) {
    // (String portName, int baudrate, int dataBit,
    // int stopBit, int parity, int flowControl)
    this.baudrate = 115200;
    this.usbDeviceSerialNumber = usbDevice.serial;

  }

  /**
   * Construct a new USBConnection object
   * 
   * @param usbDeviceSerialNumber usb serial number
   * @param baudrate baud rate
   */
  public USBConnectionTest(String usbDeviceSerialNumber, int baudrate) {
    // (String portName, int baudrate, int dataBit,
    // int stopBit, int parity, int flowControl)
    this.baudrate = baudrate;
    this.usbDeviceSerialNumber = usbDeviceSerialNumber;

  }

  /**
   * Construct a new USBConnection object
   * 
   * @param usbDeviceSerialNumber usb serial number
   */
  public USBConnectionTest(String usbDeviceSerialNumber) {
    // (String portName, int baudrate, int dataBit,
    // int stopBit, int parity, int flowControl)
    this.baudrate = 115200;
    this.usbDeviceSerialNumber = usbDeviceSerialNumber;

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
    ArrayList<DeviceInfo> devices = new ArrayList<>();
    JD2XX jdtmp = null;
    try {
      jdtmp = new JD2XX();
      int number = jdtmp.createDeviceInfoList();
      for (int i = 0; i < number; i++) {
        try {
          devices.add(jdtmp.getDeviceInfoDetail(i));
        } catch (IOException e) {
          // byte[] b = e.getMessage().getBytes();
          // if(0 != b.length)
          // {
          // if(b[0] == 0x3F)
          // throw new CommConnectionException(DEVICE_IN_USE, "A device is present, but it is not
          // possible to connect, still connected?");
          // else
          // throw new CommConnectionException(NO_DEVICES_FOUND, e.getMessage());
          // }
          // throw new CommConnectionException(DEVICE_IN_USE, "A device is present, but it is not
          // possible to connect, still connected?");
        }
      }
    } catch (UnsatisfiedLinkError e) {
      throw new CommConnectionException(NO_LIBRARY_FOUND, e.getMessage());
    } catch (NoClassDefFoundError e) {
      throw new CommConnectionException(NO_LIBRARY_FOUND, e.getMessage());
    }

    try {
      jdtmp.close();
    } catch (IOException e) {
      throw new CommConnectionException(UNHANDLED_ERROR, e.getMessage());
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
  static public LinkedList<String> getUSBDevicesList() throws CommConnectionException {
    LinkedList<String> devices_tmp = new LinkedList<>();
    JD2XX jdtmp = null;
    try {
      jdtmp = new JD2XX();
      int number = jdtmp.createDeviceInfoList();
      for (int i = 0; i < number; i++) {
        try {
          String tmp = jdtmp.getDeviceInfoDetail(i).description;
          if (0 < tmp.length())
            devices_tmp.add(tmp);
        } catch (IOException e) {
          // byte[] b = e.getMessage().getBytes();
          // if(0 != b.length)
          // {
          // if(b[0] == 0x3F)
          // throw new CommConnectionException(DEVICE_IN_USE, "A device is present, but it is not
          // possible to connect, still connected?");
          // else
          // throw new CommConnectionException(NO_DEVICES_FOUND, e.getMessage());
          // }
          // throw new CommConnectionException(DEVICE_IN_USE, "A device is present, but it is not
          // possible to connect, still connected?");
        }
      }
    } catch (UnsatisfiedLinkError e) {
      throw new CommConnectionException(NO_LIBRARY_FOUND, e.getMessage());
    } catch (NoClassDefFoundError e) {
      throw new CommConnectionException(NO_LIBRARY_FOUND, e.getMessage());
    }

    try {
      jdtmp.close();
    } catch (IOException e) {
      throw new CommConnectionException(UNHANDLED_ERROR, e.getMessage());
    }
    return devices_tmp;
  }

  @Override
  public void connect() throws CommConnectionException {
    // open stream
    if (inputstream == null) {

      try {
        jd = new JD2XX();
        jd.openBySerialNumber(usbDeviceSerialNumber);
        // _jd.openByDescription(_usbDevice);
      } catch (UnsatisfiedLinkError e) {
        throw new CommConnectionException(NO_LIBRARY_FOUND, e.getMessage());
      } catch (NoClassDefFoundError e) {
        throw new CommConnectionException(NO_LIBRARY_FOUND, e.getMessage());
      } catch (IOException e) {
        if (e.getMessage().startsWith("invalid handle")) {
          throw new CommConnectionException(DEVICE_IN_USE, e.getMessage());
        } else
          throw new CommConnectionException(NO_DEVICES_FOUND,
              e.getMessage() + " (" + usbDeviceSerialNumber + ")");
      }
      try {
        jd.setBaudRate(baudrate);
      } catch (IOException e) {
        try {
          jd.close();
        } catch (IOException e1) {
          throw new CommConnectionException(USB_SET_BAUDRATE, e.getMessage());
        }
        throw new CommConnectionException(USB_SET_BAUDRATE, e.getMessage());
      }
      try {
        jd.setDataCharacteristics(8, JD2XX.STOP_BITS_1, JD2XX.PARITY_NONE);
      } catch (IOException e) {
        try {
          jd.close();
        } catch (IOException e1) {
          throw new CommConnectionException(USB_SET_DATA_CHARACTERISTICS, e.getMessage());
        }
        throw new CommConnectionException(USB_SET_DATA_CHARACTERISTICS, e.getMessage());
      }
      try {
        jd.setFlowControl(JD2XX.FLOW_NONE, 0, 0);
      } catch (IOException e) {
        try {
          jd.close();
        } catch (IOException e1) {
          throw new CommConnectionException(USB_SET_FLOWCONTROL, e.getMessage());
        }
        throw new CommConnectionException(USB_SET_FLOWCONTROL, e.getMessage());
      }
      try {
        jd.setTimeouts(recvTimeout, 1);
      } catch (IOException e) {
        try {
          jd.close();
        } catch (IOException e1) {
          throw new CommConnectionException(USB_SET_TIMEOUTS, e.getMessage());
        }
        throw new CommConnectionException(USB_SET_TIMEOUTS, e.getMessage());
      }
      inputstream = new JD2XXInputStream(jd);
      outputstream = new JD2XXOutputStream(jd);

    }
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
  public int recv() throws CommConnectionException {
    try {
      return inputstream.read();
    } catch (NullPointerException e) {
      throw new CommConnectionException(NOT_INITIALISED, "not initialize");
    } catch (IOException e) {
      if (e.getMessage().equals("io error")) // means no Data Available
      {
        return -1;
      } else if (e.getMessage().equals("io error (4)")) {
        throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
      } else
        throw new CommConnectionException(UNHANDLED_ERROR, e.getMessage());
    }
  }

  @Override
  public void send(String senddata) throws CommConnectionException {
    try {
      outputstream.write(senddata.getBytes());
      // _outputstream.flush();
    } catch (NullPointerException e) {
      throw new CommConnectionException(NOT_INITIALISED, "not initialize");
    } catch (IOException e) {
      if (e.getMessage().equals("io error (4)")) {
        throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
      } else
        throw new CommConnectionException(UNHANDLED_ERROR, e.getMessage());
    }
  }

  @Override
  public void send(byte[] senddata) throws CommConnectionException {
    try {
      outputstream.write(senddata);
      // _outputstream.write(senddata, 0, senddata.length);
      // _outputstream.flush();
    } catch (NullPointerException e) {
      throw new CommConnectionException(NOT_INITIALISED, "not initialize");
    } catch (IOException e) {
      if (e.getMessage().equals("io error (4)")) {
        throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
      } else
        throw new CommConnectionException(UNHANDLED_ERROR, e.getMessage());
    }
  }

  @Override
  public int dataAvailable() throws CommConnectionException {
    try {
      int[] tmp = jd.getStatus();
      return tmp[0];
    } catch (IOException e) {
      if (e.getMessage().startsWith("invalid handle (1)"))
        throw new CommConnectionException(NOT_INITIALISED, "not initialize");
      else
        throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
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
      info.put("baudrate", baudrate);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return info;
  }

  @Override
  public void setSettings(Hashtable<String, String> settings) {
    String baudrate = settings.get("baudrate");
    if (null != baudrate) {
      try {
        this.baudrate = Integer.parseInt(baudrate);
      } catch (NumberFormatException e) {

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
    recvTimeout = timeout;
    if (jd != null) {
      try {
        jd.setTimeouts(recvTimeout, 1);
      } catch (IOException e) {
        try {
          jd.close();
        } catch (IOException e1) {
          throw new CommConnectionException(SET_CONFIGURATION, e.getMessage());
        }
        throw new CommConnectionException(SET_CONFIGURATION, e.getMessage());
      }
    }
  }

  /**
   * @return the used baud rate
   */
  public int getBaudrate() {
    return baudrate;
  }

  /**
   * set the baud rate to use
   * 
   * @param baudrate baud rate
   */
  public void setBaudrate(int baudrate) {
    this.baudrate = baudrate;
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
  public boolean isConnected() {
    return inputstream == null ? false : true;
  }

}
