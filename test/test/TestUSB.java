/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/
package test;

import java.io.IOException;
import java.util.ArrayList;

import com.metratec.lib.connection.CommConnectionException;
import com.metratec.lib.connection.UsbConnection;

import jd2xx.JD2XX.DeviceInfo;

@SuppressWarnings("javadoc")
public class TestUSB {

  /**
   * @param args
   */
  public static void main(String[] args) {
    boolean run = true;
    while (run) {
      ArrayList<DeviceInfo> devices = null;
      try {
        devices = UsbConnection.getUSBDevices();
      } catch (CommConnectionException e) {
        // "no rxtxSerial library found";
        System.out.println(e.getErrorCode() + " " + e.getMessage());
        run = false;
        break;
      }

      for (DeviceInfo s : devices) {
        System.out.println(s.description);
        System.out.println(s.id);
        System.out.println(s.type);
        System.out.println(s.serial);
        System.out.println(s.flags);
        System.out.println(s.handle);
        System.out.println(s.index);
        System.out.println(s.location);
        System.out.println();
      }
      System.out.println("Connect to " + devices.get(0).serial + " (Please press enter)");
      try {
        System.in.read();
      } catch (IOException e2) {
      }
      UsbConnection connection = new UsbConnection(devices.get(0).serial);
      try {
        connection.connect();
      } catch (CommConnectionException e) {
        System.out.println("connect(): " + e.getErrorCode() + " " + e.getMessage());
      }

      // boolean test =true;

      try {

        System.out.println("Send RSN to " + devices.get(0).serial + " (Please press enter)");
        try {
          System.in.read();
        } catch (IOException e2) {
        }
        try {
          System.in.read();
        } catch (IOException e2) {
        }
        connection.send("RSN\r");

        System.out.println("Recv answer from " + devices.get(0).serial + " (Please press enter)");
        try {
          System.in.read();
        } catch (IOException e2) {
        }
        try {
          System.in.read();
        } catch (IOException e2) {
        }
        System.out.println(connection.recv(13));
        connection.disconnect();
        // connection.dataAvailable();
        connection.connect();
        connection.send("RSN\r");
        System.out.println(connection.recv(13));
        try {
          System.in.read();
        } catch (IOException e) {
          e.printStackTrace();
        }
        int count = 0;
        while (true) {
          System.out.println("Count " + (++count));
          connection.recv();
        }
      } catch (CommConnectionException e) {
        System.out.println(e.getErrorCode() + " " + e.getMessage());
        try {
          connection.disconnect();
        } catch (CommConnectionException e1) {
          System.out.println(e.getErrorCode() + " " + e.getMessage());
        }
      }

      // while(test)
      // {
      // try
      // {
      // System.out.println(connection.recv());
      // }
      // catch (CommConnectionException e)
      // {
      // System.out.println(e.getErrorCode()+" "+e.getMessage());
      // }
      //
      // }
      try {
        connection.disconnect();
      } catch (CommConnectionException e) {
        System.out.println(e.getErrorCode() + " " + e.getMessage());
      }

      run = false;

    }
  }

}
