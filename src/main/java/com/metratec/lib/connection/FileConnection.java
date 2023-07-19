package com.metratec.lib.connection;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Hashtable;

/**
 * This is an implementation of ICommConnection that relies only on
 * standard JRE file APIs.
 * This is useful on embedded systems instead of Rs232Connection/UsbConnection
 * since it does not require any JNI.
 * Line settings can be set externally on the device node using `stty` before
 * launching the program.
 */
public class FileConnection extends ICommConnection {
  protected OutputStream outputstream = null;
  protected InputStream inputstream = null;

  private String portName;

  private int recvTimeout = 2000;

  public FileConnection(String portName) {
    this.portName = portName;
  }

  /**
   * This method opens a connection. Parameters are passed through the constructor
   */
  public void connect() throws CommConnectionException {
    File file = new File(portName);
    try {
      outputstream = new FileOutputStream(file);
      inputstream = new FileInputStream(file);
    } catch (FileNotFoundException e) {
      throw new CommConnectionException(SERIAL_PORT_NOT_EXIST,
          "Specified serial port " + portName + " could not be opened: " + e.getMessage());
    }
  }

  @Override
  public void disconnect() throws CommConnectionException {
    try {
      if(null != inputstream){
        inputstream.close();
        inputstream = null;
      }
      if(null != outputstream){
        outputstream.close();
        outputstream = null;
      }
    } catch (IOException e) {
      throw new CommConnectionException(SERIAL_NO_ACCESS,
          "No access to Output-/InputStream " + e.getMessage());
    }
  }

  @SuppressWarnings("PMD.EmptyCatchBlock")
  @Override
  public void send(byte[] senddata) throws CommConnectionException {
    try {
      outputstream.write(senddata);
      outputstream.flush();
    } catch (NullPointerException e) {
      if (senddata == null) {
        throw new CommConnectionException(WRONG_PARAMETER, "data is null");
      } else {
        throw new CommConnectionException(NOT_INITIALISED, "not initialized");
      }
    } catch (IOException e) {
      try {
        disconnect();
      } catch (CommConnectionException e2) {}
      throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
    }
  }

  @SuppressWarnings("PMD.EmptyCatchBlock")
  @Override
  public int recv() throws CommConnectionException {
    try {
      long maxTimeMillis = System.currentTimeMillis() + recvTimeout;
      int c;
      while(-1 == (c = inputstream.read()) && System.currentTimeMillis() < maxTimeMillis){
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
        }
      }
      if (c < 0) {
        try {
          disconnect();
        } catch (CommConnectionException e) {}
        throw new CommConnectionException(CONNECTION_LOST, "device closed");
      }
      return c;
    } catch (IOException e) {
      try {
        disconnect();
      } catch (CommConnectionException e2) {}
      throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
    }
  }

  @Override
  public boolean isConnected() {
    return inputstream != null;
  }

  @Override
  public Hashtable<String, Object> getInfo() {
    Hashtable<String, Object> info = new Hashtable<>();
    info.put("type", "file");
    info.put("portname", portName);
    return info;
  }

  @Override
  public void setSettings(Hashtable<String, String> settings) {
    String portname = settings.get("portname");
    if (null != portname) {
      this.portName = portname;
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
  public void setRecvTimeout(int timeout) {
    recvTimeout = timeout;
  }

  @Override
  public int getRecvTimeout() {
    return recvTimeout;
  }

  @Override
  public void setConnectionTimeout(int timeout) {}

  @Override
  public int getConnectionTimeout() {
    return 0;
  }
}
