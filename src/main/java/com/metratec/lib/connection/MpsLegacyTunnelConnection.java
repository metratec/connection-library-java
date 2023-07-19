/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH All rights reserved.
 *******************************************************************************/
package com.metratec.lib.connection;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * A connection to a slave device behind a MPS Master (MPS Beacon, BraceID, etc.).
 *
 * The connection to the master can be established by any means. The MPS must be correctly set up
 * before calling MpsTunnelConnection.connect(). The link to the slave device can either be
 * established in advance or you pass an EID to the constructor.
 *
 * This class may then be used to opaquely tunnel data to and from the slave device. This class uses
 * DAT command to communicate to the slave device instead of BINXT commands used by the
 * `MpsTunnelConnection`. Thus the class supports legacy beacons/MPS version that only use the DAT
 * protocol.
 *
 * Every datastream must at the time of the flush, this includes `send()` call, must end in CR and
 * have CRs in the right distance. This also means that `MpsLegacyTunnelConnection` may not be used
 * as a drop-in for existing ICommConnections in every situation (if the code breaks these
 * restrictions).
 * Just as `MpsTunnelConnection`, both outgoing and incoming data may be duplicated, making this
 * even less fit to drop into some existing ICommConnection-user.
 * `MpsTunnelConnection` should always be preferred if the circumstances allow it.
 *
 * bug
 * Furthermore, you cannot currently send more than one CR-terminated line before flushing
 * and users must check for errors, which are MPS-specific, like `TOE` manually in their
 * code.
 * This can and should all be handled within the class.
 */
public class MpsLegacyTunnelConnection extends MpsTunnelConnection {
  @Override
  protected void addDownstreamFrame(String line) throws IOException {
    if (getLogger().isTraceEnabled()) {
      getLogger().trace("{} recv {}", toString(), line);
    }
    assert line.startsWith("DAT ");
    // The message format is: DAT <SRC-EID> <MSG> -xxx
    // In linked mode there is no <SRC-EID>
    // The message itself might contain multiple spaces, so we need to combine each part of it.
    String str[] = line.split(" ");
    if (str.length >= 3) {
      int start = linked ? 1 : 2;
      String resp = String.join(" ", Arrays.copyOfRange(str, start, str.length - 1));
      downstreamBuf.write(resp.getBytes());
      downstreamBuf.write((byte)13);
    } else {
      System.out.println("Debug: " + line);
      downstreamBuf.write(line.substring(4).getBytes());
    }
  }

  /**
   * An OutputStream implementing MPS tunneling (serializing data into MPS frames).
   *
   * This class does NOT implement buffering and is only exposed to the outside world wrapped in a
   * BufferedOutputStream.
   */
  private class TunnelOutputStream extends OutputStream {
    /** Maximum length of a DAT frame in raw bytes */
    private static final int MAX_FRAME_SIZE = 96;

    private int indexOf(byte[] b, int off, int len, int c) {
      for (int i = off; i < len; i++) {
        if (b[i] == c) {
          return i;
        }
      }
      return -1;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      /*
       * NOTE: Using the masterConn's OutputStream instead of the ICommConnection methods simplifies
       * error handling.
       */
      OutputStream masterOutStream = masterConn.getOutputStream();

      /*
       * The DAT command takes at most MAX_FRAME_SIZE bytes of data. We assume that each frame of
       * data is terminated by a CR. If a CR is missing then an exception is thrown.
       *
       * TODO: The Arrays.copyOfRange could be avoided here by using repeated writes
       * (masterOutStream should be buffered).
       */
      while (len > 0) {
        int frameLength = Math.min(len, MAX_FRAME_SIZE);
        // find the first occurence of CR
        int index = indexOf(b, off, off+frameLength, 13);
        if (index == -1) {
          // CR was not found in the current frame
          throw new IOException("CR not present in the frame");
        }
        byte[] frame = Arrays.copyOfRange(b, off, index);
        masterOutStream.write("DAT ".getBytes());
        if (slaveEID != null && !linked) {
          // addressed send
          masterOutStream.write((slaveEID + " ").getBytes());
        }
        masterOutStream.write(frame);
        masterOutStream.write('\r');
        if (getLogger().isTraceEnabled()) {
          getLogger().trace("Tunneled {} send DAT {} {}", masterConn,
              slaveEID != null ? slaveEID : "", new String(frame));
        }
        masterOutStream.flush();

        off += MAX_FRAME_SIZE;
        len -= MAX_FRAME_SIZE;
      }
    }

    @Override
    public void write(int b) throws IOException {
      write(new byte[] {(byte) b});
    }
  }

  private OutputStream outStream = new BufferedOutputStream(new TunnelOutputStream());
  private InputStream inStream = new TunnelInputStream("DAT ");
  private boolean linked = false;

  /**
   * Construct a MPS Tunneling Connection.
   *
   * A link to the slave device will be established during connect().
   *
   * @param connection The connection to the master device (MPS).
   * @param slaveEID The slave's EID (8 bytes hexadecimal string) or null.
   */
  public MpsLegacyTunnelConnection(ICommConnection connection, String slaveEID) {
    super(connection, slaveEID);
  }

  /**
   * Construct a MPS Tunneling Connection.
   *
   * The link to the slave device must already be established.
   *
   * @param connection The connection to the master device (MPS).
   */
  public MpsLegacyTunnelConnection(ICommConnection connection) {
    super(connection);
  }

  /**
   * Sets whether to use linked communication or not.
   * This must only be set before a connection established. Once the connection is established one
   * this value can only be modified after a disconnect is called.
   * @param linked true if linked communication should be used, false otherwise
   */
  public void setLinked(boolean linked) {
    this.linked = linked;
  }

  /**
   * Returns whether to use linked communication.
   * @return true if linked communication is being used, false otherwise
   */
  public boolean isLinked() {
    return linked;
  }

  @Override
  public void connect() throws CommConnectionException {
    if (!masterConn.isConnected()) {
      if (linked) {
        super.connect();
      } else {
        masterConn.connect();
        unlink();
      }
    }
  }

  @Override
  public void disconnect() throws CommConnectionException {
    if (linked) {
      unlink();
    }
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
  public void send(byte[] senddata) throws CommConnectionException {
    if (senddata[senddata.length - 1] != 13) {
      throw new CommConnectionException(UNHANDLED_ERROR, "Data must be carriage return terminated");
    }
    super.send(senddata);
  }

  @Override
  public String toString() {
    return "Legacy Tunneled " + masterConn;
  }
}
