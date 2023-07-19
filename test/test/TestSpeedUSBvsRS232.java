/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/
package test;

import java.util.ArrayList;

import com.metratec.lib.connection.CommConnectionException;
import com.metratec.lib.connection.ICommConnection;
import com.metratec.lib.connection.Rs232Connection;
import com.metratec.lib.connection.UsbConnection;

import jd2xx.JD2XX.DeviceInfo;

@SuppressWarnings("javadoc")
public class TestSpeedUSBvsRS232 {
  UsbConnection connectionUSB;
  Rs232Connection connectionRS232;

  public static void main(String[] args) {
    TestSpeedUSBvsRS232 test = new TestSpeedUSBvsRS232();
    try {
      int count = 100;
      test.initUSB();
      test.testSpeed(0, count);
      test.closeUSB();

      test.initRS232();
      test.testSpeed(1, count);
      test.closeRS232();
    } catch (CommConnectionException e) {
      e.printStackTrace();
    }

  }

  public TestSpeedUSBvsRS232() {

  }

  public void initRS232() throws CommConnectionException {
    String[] devices = null;
    devices = Rs232Connection.getSerialPorts();

    if (devices.length > 0) {
      System.out.println("Connect with: " + devices[1]);
      connectionRS232 = new Rs232Connection(devices[1], 115200, 8, 1, 0, 0);
      connectionRS232.connect();
    } else {
      System.out.println("no USB devices ");
    }
  }

  public void initUSB() throws CommConnectionException {
    ArrayList<DeviceInfo> devices = null;
    devices = UsbConnection.getUSBDevices();
    if (devices.size() > 0) {
      System.out.println("Connect with: " + devices.get(0).description);
      connectionUSB = new UsbConnection(devices.get(0));
      connectionUSB.connect();
    } else {
      System.out.println("no USB devices ");
    }
  }

  public void closeUSB() throws CommConnectionException {
    connectionUSB.disconnect();
  }

  public void closeRS232() throws CommConnectionException {
    connectionRS232.disconnect();
  }

  /**
   * 
   * @param device 0..USB 1..RS232
   * @param count number of runs
   * @throws CommConnectionException
   */
  public void testSpeed(int device, int count) throws CommConnectionException {
    ICommConnection connection = null;
    switch (device) {
      case 0: // USB
        connection = connectionUSB;
        break;
      case 1: // RS232
        connection = connectionRS232;
        break;
      default:
        System.out.println("Wrong connection Number");
        break;
    }
    long timeStart = 0;
    long timeEnd = 0;

    timeStart = System.currentTimeMillis();
    for (int i = 0; i < count; i++) {
      connection.send("RSN\r");
      connection.recv(13);
    }
    timeEnd = System.currentTimeMillis();
    switch (device) {
      case 0: // USB
        System.out.println("USB Time for " + count + " runs: " + (timeEnd - timeStart) + " "
            + timeStart + " " + timeEnd);
        break;
      case 1: // RS232
        System.out.println("RS232 Time for " + count + " runs: " + (timeEnd - timeStart) + " "
            + timeStart + " " + timeEnd);
        break;
      default:
        System.out.println("Wrong connection Number");
        break;
    }

  }
}
