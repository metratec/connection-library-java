package com.metratec.lib.connection;

import gnu.io.SerialPort;

/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/

/**
 * Abstract parent class for the serial connections
 *
 * @author man
 */
public abstract class SerialConnection extends ICommConnection {

  public static final int  DATABITS_5             = SerialPort.DATABITS_5;
  public static final int  DATABITS_6             = SerialPort.DATABITS_6;
  public static final int  DATABITS_7             = SerialPort.DATABITS_7;
  public static final int  DATABITS_8             = SerialPort.DATABITS_8;
  public static final int  PARITY_NONE            = SerialPort.PARITY_NONE;
  public static final int  PARITY_ODD             = SerialPort.PARITY_ODD;
  public static final int  PARITY_EVEN            = SerialPort.PARITY_EVEN;
  public static final int  PARITY_MARK            = SerialPort.PARITY_MARK;
  public static final int  PARITY_SPACE           = SerialPort.PARITY_SPACE;
  public static final int  STOPBITS_1             = SerialPort.STOPBITS_1;
  public static final int  STOPBITS_2             = SerialPort.STOPBITS_2;
  public static final int  STOPBITS_1_5           = SerialPort.STOPBITS_1_5;
  public static final int  FLOWCONTROL_NONE       = SerialPort.FLOWCONTROL_NONE;
  public static final int  FLOWCONTROL_RTSCTS_IN  = SerialPort.FLOWCONTROL_RTSCTS_IN;
  public static final int  FLOWCONTROL_RTSCTS_OUT = SerialPort.FLOWCONTROL_RTSCTS_OUT;
  public static final int  FLOWCONTROL_XONXOFF_IN = SerialPort.FLOWCONTROL_XONXOFF_IN;
  public static final int  FLOWCONTROL_XONXOFF_OUT= SerialPort.FLOWCONTROL_XONXOFF_OUT;


  private int baudrate;
  private int dataBit;
  private int stopBit;
  private int parity;
  private int flowControl;

  /**
   * Construct a new SerialConnection instance
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
  SerialConnection(int baudrate, int dataBit, int stopBit, int parity, int flowControl) {
    super();
    this.baudrate = baudrate;
    this.dataBit = dataBit;
    this.stopBit = stopBit;
    this.parity = parity;
    this.flowControl = flowControl;
  }

  /**
   * @return the baud rate of the connected device
   */
  public int getBaudrate() {
    return baudrate;
  }

  /**
   * @param baudrate baud rate of the connected device
   */
  public void setBaudrate(int baudrate) {
    this.baudrate = baudrate;
  }

  /**
   * @return the number of data bits
   */
  public int getDataBit() {
    return dataBit;
  }

  /**
   * @param dataBit number of data bits
   */
  public void setDataBit(int dataBit) {
    this.dataBit = dataBit;
  }

  /**
   * @return the value of StopBits, a value larger 2 means 1.5 stopbits
   */
  public int getStopBit() {
    return stopBit;
  }

  /**
   * @param stopBit value of StopBits, a value larger 2 means 1.5 stopbits
   */
  public void setStopBit(int stopBit) {
    this.stopBit = stopBit;
  }

  /**
   * @return the parity bit (0 for no, 1 for ODD, 2 for EVEN, 3 for MARK, 4 for SPACE)
   */
  public int getParity() {
    return parity;
  }

  /**
   * @param parity parity bit (0 for no, 1 for ODD, 2 for EVEN, 3 for MARK, 4 for SPACE)
   */
  public void setParity(int parity) {
    this.parity = parity;
  }

  /**
   * @return the flow Control (0 for no, ..)
   */
  public int getFlowControl() {
    return flowControl;
  }

  /**
   * @param flowControl flow Control (0 for no, ..)
   */
  public void setFlowControl(int flowControl) {
    this.flowControl = flowControl;
  }

}
