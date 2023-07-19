/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/
package test;

import com.metratec.lib.connection.CommConnectionException;
import com.metratec.lib.connection.Rs232Connection;

import gnu.io.SerialPort;

@SuppressWarnings("javadoc")
public class TestRS232 {
  public static void main(String[] args) {
    boolean run = true;
    System.out.println("SerialPort.DATABITS_5 " + SerialPort.DATABITS_5);
    System.out.println("SerialPort.DATABITS_6 " + SerialPort.DATABITS_6);
    System.out.println("SerialPort.DATABITS_7 " + SerialPort.DATABITS_7);
    System.out.println("SerialPort.DATABITS_8 " + SerialPort.DATABITS_8);
    System.out.println("SerialPort.FLOWCONTROL_NONE " + SerialPort.FLOWCONTROL_NONE);
    System.out.println("SerialPort.FLOWCONTROL_RTSCTS_IN " + SerialPort.FLOWCONTROL_RTSCTS_IN);
    System.out.println("SerialPort.FLOWCONTROL_RTSCTS_OUT " + SerialPort.FLOWCONTROL_RTSCTS_OUT);
    System.out.println("SerialPort.FLOWCONTROL_XONXOFF_IN " + SerialPort.FLOWCONTROL_XONXOFF_IN);
    System.out.println("SerialPort.FLOWCONTROL_XONXOFF_OUT " + SerialPort.FLOWCONTROL_XONXOFF_OUT);
    System.out.println("SerialPort.PARITY_EVEN " + SerialPort.PARITY_EVEN);
    System.out.println("SerialPort.PARITY_MARK " + SerialPort.PARITY_MARK);
    System.out.println("SerialPort.PARITY_NONE " + SerialPort.PARITY_NONE);
    System.out.println("SerialPort.PARITY_ODD " + SerialPort.PARITY_ODD);
    System.out.println("SerialPort.PARITY_SPACE " + SerialPort.PARITY_SPACE);
    System.out.println("SerialPort.STOPBITS_1 " + SerialPort.STOPBITS_1);
    System.out.println("SerialPort.STOPBITS_1_5 " + SerialPort.STOPBITS_1_5);
    System.out.println("SerialPort.STOPBITS_2 " + SerialPort.STOPBITS_2);

    while (run) {
      String[] devices = null;
      try {
        devices = Rs232Connection.getSerialPorts();
      } catch (CommConnectionException e) {
        // "no rxtxSerial library found";
        System.out.println(e.getErrorCode() + " " + e.getMessage());
        run = false;
        break;
      }

      for (String s : devices)
        System.out.println(s);

      // Connect
      Rs232Connection connection = new Rs232Connection("COM10", 115200, 8, 1, 0, 0);
      try {
        connection.connect();
      } catch (CommConnectionException e) {
        System.out.println(e.getErrorCode() + " " + e.getMessage());
      }
      boolean test = true;
      while (test) {
        try {
          connection.send("RSN\r");
          System.out.println(connection.recv(13));
          connection.disconnect();
          // connection.dataAvailable();
          connection.connect();
          connection.send("RSN\r");
          System.out.println(connection.recv(13));
          test = false;
        } catch (CommConnectionException e) {
          System.out.println(e.getErrorCode() + " " + e.getMessage());
          test = false;
        }
        // try
        // {
        // while(test)
        // {
        // System.out.println(connection.recv());
        // }
        // }
        // catch (CommConnectionException e)
        // {
        // System.out.println(e.getErrorCode()+" "+e.getMessage());
        // test=false;
        // }
      }
      try {
        connection.disconnect();
      } catch (CommConnectionException e) {
        e.printStackTrace();
      } ;
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }
      try {
        connection.connect();
      } catch (CommConnectionException e) {
        e.printStackTrace();
      }
      try {
        connection.disconnect();
      } catch (CommConnectionException e) {
        e.printStackTrace();
      }
      run = false;
    }

    // RS232Connection connection = new RS232Connection(portName, baudrate, dataBit, stopBit,
    // parity, flowControl)
  }
}
