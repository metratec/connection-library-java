package com.metratec.lib.connection;

/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/

import java.io.IOException;

/**
 * The Exception Class for the communication interfaces
 *
 * @author Matthias Neumann (neumann@metratec.com)
 *
 * @version 1.2
 */
public class CommConnectionException extends IOException {

  private static final long serialVersionUID = 1L;
  private int errorcode = 0;

  /**
   * Constructs a new communication exception.
   *
   * @see java.lang.Exception
   */
  public CommConnectionException() {
    super();
  }

  /**
   * Constructs a new communication exception with the specified detail message.
   *
   * @param s the detail message. The detail message is saved for later retrieval by the
   *        {@link #getMessage()} method.
   *
   * @see java.lang.Exception#Exception(String message)
   */
  public CommConnectionException(String s) {
    super(s);
  }

  /**
   * Constructs a new communication exception with the specified error code.
   *
   * @param errorcode the error code. The detail message is saved for later retrieval by the
   *        {@link #getErrorCode()} method.
   */
  public CommConnectionException(int errorcode) {
    super();
    this.errorcode = errorcode;
  }

  /**
   * Constructs a new communication exception with the specified detail message and error code.
   *
   * @param errorcode the error code. The detail message is saved for later retrieval by the
   *        {@link #getErrorCode()} method.
   *
   * @param s the detail message. The detail message is saved for later retrieval by the
   *        {@link #getMessage()} method.
   */
  public CommConnectionException(int errorcode, String s) {
    super(s);
    this.errorcode = errorcode;
  }

  /**
   * @return the error code (<tt>'0'</tt> if not initialize).
   */
  public int getErrorCode() {
    return errorcode;
  }

  /**
   * Get human readable description of the error code.
   *
   * Returns a verbose description of the error code that can be retrieved with the
   * {@link #getErrorCode()} method. This is largely the same information as contained in the
   * comments of the ICommConnection interface.
   *
   * @return Verbose description of error code.
   */
  public String getErrorDescription() {
    switch (errorcode) {
      // Serial - connect
      case ICommConnection.SERIAL_PORT_NOT_EXIST:
        return "Specified serial port does not exist.";
      case ICommConnection.SERIAL_PARAMETER_NOT_SET:
        return "Could not set interface parameter.";
      case ICommConnection.SERIAL_NO_ACCESS:
        return "No access to the input/output stream.";
      case ICommConnection.SERIAL_NOT_INITIALISED:
        return "Could not initialize the RS232 connection.";

      // USB
      case ICommConnection.USB_SET_BAUDRATE:
        return "Setting baudrate for the USB connection failed.";
      case ICommConnection.USB_SET_DATA_CHARACTERISTICS:
        return "Setting data characteristics for the USB connection failed.";
      case ICommConnection.USB_SET_FLOWCONTROL:
        return "Setting flow control for the USB connection failed.";
      case ICommConnection.USB_SET_TIMEOUTS:
        return "Setting timeouts for the USB connection failed.";

      // ETHERNET
      case ICommConnection.ETHERNET_TIMEOUT:
        return "Ethernet connection timeout.";
      case ICommConnection.ETHERNET_UNKNOWN_HOST:
        return "Host not found.";

      // USER
      case ICommConnection.USER_ERRORCODE_01:
      case ICommConnection.USER_ERRORCODE_02:
      case ICommConnection.USER_ERRORCODE_03:
      case ICommConnection.USER_ERRORCODE_04:
      case ICommConnection.USER_ERRORCODE_05:
        return String.format("User-specified error (%d).",
            errorcode - ICommConnection.USER_ERRORCODE_01);

      // General
      case ICommConnection.UNHANDLED_ERROR:
        return "Unhandled Error: " + getMessage();
      case ICommConnection.NO_LIBRARY_FOUND:
        return "Could not find the required Java library for the connection.";
      case ICommConnection.NO_DEVICES_FOUND:
        return "No devices for a connection found.";
      case ICommConnection.DEVICE_IN_USE:
        return "Device is in use, could no connect.";
      case ICommConnection.WRONG_PARAMETER:
        return "Wrong parameter.";
      case ICommConnection.CONNECTION_LOST:
        return "Lost the connection.";
      case ICommConnection.SET_CONFIGURATION:
        return "Error while setting parameters.";
      case ICommConnection.NOT_INITIALISED:
        return "Connection is not initialized.";
      case ICommConnection.RECV_TIMEOUT:
        return "Receiving data timed out.";
      default:
        return String.format("Unknown error (%d).", errorcode);
    }


  }
}
