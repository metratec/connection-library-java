/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH All rights reserved.
 *******************************************************************************/
package com.metratec.lib.connection;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Hashtable;
import javax.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A connection to a slave device behind a MPS Master (MPS Beacon, BraceID, etc.).
 *
 * The connection to the master can be established by any means. The MPS must be correctly set up
 * before calling MpsTunnelConnection.connect(). The link to the slave device can either be
 * established in advance or you pass an EID to the constructor.
 *
 * This class may than be used to opaquely tunnel data to and from the slave device.
 *
 * Unfortunately at least with some versions of the MPS, due to shortcomings in the
 * retransmission algorithms used, frames may be duplicated, which manifests itself
 * in both outgoing and incoming data duplication not present in other ICommConnection
 * implementations.
 *
 * @note You may almost certainly have to set the receive timeout to 61000ms if you
 * don't know how the beacons/clients are configured.
 */
public class MpsTunnelConnection extends ICommConnection {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  /** Connection to the master device (MPS) */
  protected ICommConnection masterConn;
  /** Slave EID to connect to */
  protected String slaveEID = null;
  /** Maximum BINXT frame size in bytes (ie. twice as much hexadecimal-encoded). */
  protected int maxFrameSize = 96;

  protected class CircularBuffer {
    private byte[] buf;
    private int writePos = 0;
    private int readPos = 0;

    public CircularBuffer(int maxSize) {
      buf = new byte[maxSize];
    }

    public int available() {
      return writePos >= readPos ? writePos - readPos : buf.length - readPos + writePos;
    }

    public void write(byte b) throws IOException {
      buf[writePos] = b;
      writePos = (writePos + 1) % buf.length;
      if (writePos == readPos) {
        throw new IOException("Buffer overflow");
      }
    }

    public void write(byte[] data) throws IOException {
      for (byte b : data) {
        write(b);
      }
    }

    public int read() {
      if (readPos == writePos) {
        return -1;
      }
      byte b = buf[readPos];
      readPos = (readPos + 1) % buf.length;
      return b & 0xFF;
    }
  }

  /**
   * Buffer backing read operations.
   *
   * There is java.nio.ByteBuffer but it is not really designed for frequent writes/reads of
   * different lengths. Using ByteBuffer would require us to "compress" the buffer after every read
   * which is quite inefficient. ArrayDeque<Byte> however, would be inefficient because it boxes
   * every byte in an object. Instead, we simply implement our own byte-array backed circular queue
   * (Round Robin Buffer).
   */
  protected CircularBuffer downstreamBuf = new CircularBuffer(100 * 1024);

  /**
   * Read a CR-terminated line from the underlying transport.
   *
   * @returns The line including CR or null in case of read timeouts.
   */
  protected String masterConnRecvLine() throws IOException {
    InputStream masterInStream = masterConn.getInputStream();
    StringBuilder data = new StringBuilder();
    int c;

    do {
      try {
        c = masterInStream.read();
      } catch (SocketTimeoutException e) {
        /*
         * If masterConn is TcpConnection, masterInStream will
         * be a Socket.getInputStream() which throws this exception
         * in case of timeouts instead of returning -1.
         * See also AbstractTcpConnection.recv().
         */
        getLogger().trace("{} recv - TimeoutException {}", toString(), e.getMessage());
        return null;
      }
      if (c < 0) {
        if (getLogger().isTraceEnabled()) {
          getLogger().trace("{} recv - no data (null)", toString());
        }
        return null;
      }
      data.append((char) c);
    } while (c != '\r');
    if (getLogger().isTraceEnabled()) {
      String recv = data.toString();
      try {
        getLogger().trace("{} recv {}", toString(), recv.substring(0, recv.length() - 1));
      } catch (IndexOutOfBoundsException e) {
        getLogger().trace("{} recv {}", toString(), recv);
      }
    }
    return data.toString();
  }

  protected void addDownstreamFrame(String line) throws IOException {
    assert line.startsWith("BINXR ");
    downstreamBuf.write(DatatypeConverter.parseHexBinary(line.substring(6, line.length() - 1)));
  }

  /**
   * An OutputStream implementing MPS tunneling (serializing data into MPS binary frames).
   *
   * This class does NOT implement buffering and is only exposed to the outside world wrapped in a
   * BufferedOutputStream.
   */
  private class TunnelOutputStream extends OutputStream {
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      /*
       * NOTE: Using the masterConn's OutputStream instead of the ICommConnection methods simplifies
       * error handling.
       */
      OutputStream masterOutStream = masterConn.getOutputStream();

      /*
       * The BINXT command takes at most maxFrameSize bytes of data
       * (ie. twice as much hexadecimal-encoded).
       *
       * TODO: The Arrays.copyOfRange could be avoided here by using repeated writes
       * (masterOutStream should be buffered).
       */
      while (len > 0) {
        byte[] frame = Arrays.copyOfRange(b, off, off+Math.min(len, maxFrameSize));
        if (getLogger().isTraceEnabled()) {
          getLogger().trace("Tunneled {} send BINXT {}", masterConn, DatatypeConverter.printHexBinary(frame));
        }
        masterOutStream.write("BINXT ".getBytes());
        masterOutStream.write(DatatypeConverter.printHexBinary(frame).getBytes());
        masterOutStream.write('\r');
        masterOutStream.flush();

        long timeStamp = System.nanoTime();

        /*
         * Once we receive OK, this supposedly means that the frame has been reliably sent and
         * received by the slave device. We must wait for OK in order to implement a primitive kind
         * of flow control (not to overload the MPS and slave device). Sending may also fail and
         * TOE-errors are always possible. In the meantime we may also well receive asynchronous
         * messages including downstream traffic.
         */
        for (;;) {
          String line = masterConnRecvLine();
          if (line == null
              || (System.nanoTime() - timeStamp)/1000000 >= masterConn.getRecvTimeout()) {
            /*
             * Receive/Send timeout. Theoretically, the MPS could constantly send something except
             * for the BINXT OK. We therefore enforce another timeout here to avoid indefinite
             * blocking. A write timeout would be more fitting, than the receive timeout, but
             * ICommConnection only defines read timeouts.
             */
            if (getLogger().isTraceEnabled()) {
              getLogger().trace("Tunneled {} TimeoutException - {} {}", masterConn, masterConn.getRecvTimeout(), (System.nanoTime() - timeStamp)/1000000);
            }
            throw new IOException("Timeout while receiving MPS frame ACK");
          } else if (line.equals("BINXT OK\r")) {
            /* frame ACKed by slave device */
            break;
          } else if (line.startsWith("BINXT ")) {
            /* all other BINXT responses are considered critical errors */
            throw new IOException("Unexpected error while sending MPS frame " + "("
                + line.substring(6, line.length() - 1) + ")");
          } else if (line.equals("TOE\r")) {
            /* Timeout Exception: Link is broken */
            throw new IOException("Broken link to slave device");
          } else if (line.startsWith("BINXR ")) {
            /* downstream frame received */
            addDownstreamFrame(line);
          }
        }

        off += maxFrameSize;
        len -= maxFrameSize;
      }
    }

    @Override
    public void write(int b) throws IOException {
      write(new byte[] {(byte) b});
    }
  }

  /**
   * An InputStream for reading tunnelled data via a MPS.
   *
   * Since we must support the read() method, returning a single byte while we can receive an entire
   * downstream frame, this class is necessarily already buffered (using CircularBuffer).
   */
  protected class TunnelInputStream extends InputStream {
    private final String responsePrefix;

    TunnelInputStream(String responsePrefix) {
      this.responsePrefix = responsePrefix;
    }

    @Override
    public int available() {
      return downstreamBuf.available();
    }

    @Override
    public int read() throws IOException {
      if (downstreamBuf.available() == 0) {
        /*
         * Read or wait for the next downstream frame. All lines except BINXR and TOE are ignored.
         */
        long timeStamp = System.nanoTime();
        String line;

        do {
          line = masterConnRecvLine();
          if (line == null
              || (System.nanoTime() - timeStamp)/1000000 >= masterConn.getRecvTimeout()) {
            /*
             * Receive timeout. Theoretically, the MPS could constantly send something except for
             * the BINXR. We therefore enforce another read timeout here to avoid indefinite
             * blocking.
             */
            line = "BINXR 8200C44F\r";
            //return -1;
          } else if (line.equals("TOE\r")) {
            /* Timeout Exception: Link is broken */
            throw new IOException("Broken link to slave device");
          }
        } while (!line.startsWith(responsePrefix));

        addDownstreamFrame(line);
      }

      return downstreamBuf.read();
    }
  }

  private OutputStream outStream = new BufferedOutputStream(new TunnelOutputStream());
  private InputStream inStream = new TunnelInputStream("BINXR ");

  /**
   * The default link timeout takes into account that the beacon ping interval
   * might be up to 60s (see SPI command) and we don't know what's configured on the beacon.
   */
  private int linkTimeout = 61000;

  /**
   * Construct a MPS Tunneling Connection.
   *
   * A link to the slave device will be established during connect().
   *
   * @param connection The connection to the master device (MPS).
   * @param slaveEID The slave's EID (8 bytes hexadecimal string) or null.
   */
  public MpsTunnelConnection(ICommConnection connection, String slaveEID) {
    assert connection != null;
    masterConn = connection;
    assert slaveEID == null || slaveEID.length() == 16;
    this.slaveEID = slaveEID;
  }

  /**
   * Construct a MPS Tunneling Connection.
   *
   * The link to the slave device must already be established.
   *
   * @param connection The connection to the master device (MPS).
   */
  public MpsTunnelConnection(ICommConnection connection) {
    this(connection, null);
  }

  /**
   * Gets the link timeout for linking to a slave device.
   *
   * @return the link timeout (in milliseconds)
   */
  public int getLinkTimeout() {
    return linkTimeout;
  }

  /**
   * Sets the link timeout for linking to a slave device.
   * The parameter controls the time to wait until a response to the link command arrives.
   * @param linkTimeout the link timeout (in milliseconds)
   */
  public void setLinkTimeout(int linkTimeout) {
    this.linkTimeout = linkTimeout;
  }

  /**
   * Gets the maximum BINXT frame size.
   *
   * @return frame size in bytes
   */
  public int getMaxFrameSize() {
    return maxFrameSize;
  }

  /**
   * Sets the maximum BINXT frame size.
   * @param maxFrameSize frame size in bytes
   */
  public void setMaxFrameSize(int maxFrameSize) {
    this.maxFrameSize = maxFrameSize;
  }

  @Override
  public void connect() throws CommConnectionException {
    masterConn.connect();
    if (slaveEID == null) {
      return;
    }
    unlink();
    link();
  }

  protected void unlink() throws CommConnectionException {
    long timeStamp = System.nanoTime();
    String response;
    /*
     * There might already be a connection, so try to unlink first. If there was no connection, ULK
     * ERR will be returned.
     */
    masterConn.send("ULK\r");
    getLogger().trace("{} send ULK", toString());
    do {
      response = masterConn.recv('\r');
      if ((System.nanoTime() - timeStamp)/1000000 >= linkTimeout) {
        throw new CommConnectionException(UNHANDLED_ERROR,
            "Timeout during unlinking");
      }
    } while (!response.startsWith("ULK "));
    if (!response.equals("ULK OK\r") && !response.equals("ULK ERR\r")) {
      throw new CommConnectionException(UNHANDLED_ERROR,
          "Unexpected ULK response: " + response.substring(4, response.length() - 1));
    }
  }

  protected void link() throws CommConnectionException {
    long timeStamp = System.nanoTime();
    String response;

    if (slaveEID == null) {
      return;
    }

    masterConn.send("LNK " + slaveEID + "\r");
    getLogger().trace("{} send LNK {}", toString(), slaveEID);
    do {
      response = masterConn.recv('\r');
      if ((System.nanoTime() - timeStamp)/1000000 >= linkTimeout) {
        throw new CommConnectionException(UNHANDLED_ERROR,
            "Timeout during linking to slave device");
      }
    } while (!response.startsWith("LNK "));
    if (!response.equals("LNK " + slaveEID + " OK\r")) {
      throw new CommConnectionException(UNHANDLED_ERROR,
          "Unexpected LNK response: " + response.substring(4, response.length() - 1));
    }
  }

  @Override
  public boolean isConnected() {
    return masterConn.isConnected();
  }

  @Override
  public void disconnect() throws CommConnectionException {
    masterConn.disconnect();
  }

  @Override
  public InputStream getInputStream() {
    return inStream;
  }

  @Override
  public OutputStream getOutputStream() {
    return outStream;
  }

  @Override
  public int dataAvailable() throws CommConnectionException {
    try {
      return getInputStream().available();
    } catch (IOException e) {
      throw new CommConnectionException(CONNECTION_LOST, e.getMessage());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.metratec.lib.connection.ICommConnection#recv(byte[], int, int)
   */
  @Override
  public void recv(byte[] b, int off, int len) throws CommConnectionException {
    // TODO Auto-generated method stub
    try {
      super.recv(b, off, len);
    } finally {
      if (getLogger().isTraceEnabled()) {
        getLogger().trace("{} recv {} ({})", toString(),DatatypeConverter.printHexBinary(b), new String(b));
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
    // TODO Auto-generated method stub
    StringBuilder s = super.receive(terminators);
    if (getLogger().isTraceEnabled()) {
      getLogger().trace("{} recv {}", toString(), s.toString());
    }
    return s;
  }

  @Override
  public int recv() throws CommConnectionException {
    try {
      return getInputStream().read();
    } catch (IOException e) {
      if (e.getMessage() == null) {
        throw new CommConnectionException(CONNECTION_LOST, "Input/output error");
      }

      throw new CommConnectionException(UNHANDLED_ERROR, e.getMessage());
    }
  }

  @Override
  public void send(byte[] senddata) throws CommConnectionException {
    if (getLogger().isTraceEnabled()) {
      getLogger().trace("{} send {}", toString(), new String(senddata));
    }
    try {
      getOutputStream().write(senddata);
      getOutputStream().flush();
    } catch (NullPointerException e) {
      if (senddata == null) {
        throw new CommConnectionException(WRONG_PARAMETER, "senddata is null");
      }

      throw new CommConnectionException(NOT_INITIALISED, "not initialized");
    } catch (IOException e) {
      throw new CommConnectionException(UNHANDLED_ERROR, e.getMessage());
    }
  }

  @Override
  public Hashtable<String, Object> getInfo() {
    return masterConn.getInfo();
  }

  @Override
  public void setSettings(Hashtable<String, String> settings) {
    masterConn.setSettings(settings);
  }

  @Override
  public void setRecvTimeout(int timeout) throws CommConnectionException {
    masterConn.setRecvTimeout(timeout);
  }

  @Override
  public int getRecvTimeout() {
    return masterConn.getRecvTimeout();
  }

  @Override
  public void setConnectionTimeout(int time) {
    masterConn.setConnectionTimeout(time);
  }

  @Override
  public int getConnectionTimeout() {
    return masterConn.getConnectionTimeout();
  }

  @Override
  public String toString() {
    return "Tunneled " + masterConn;
  }

  protected Logger getLogger() {
    return logger;
  }

  /**
   * @return the masterConn
   */
  public ICommConnection getMasterConn() {
    return masterConn;
  }

}
