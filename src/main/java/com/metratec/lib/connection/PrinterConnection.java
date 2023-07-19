package com.metratec.lib.connection;

/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.PrinterName;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A connection to a printer.
 *
 * This allows you to start print jobs using programs already supporting the ICommConnection API.
 * The primary application of this connection type is for sending raw commands to raw printers
 * supporting a printer language like ZPL. ZPL printers will often expose their ZPL command
 * interface as TCP ports, serial connections or ordinary printer devices connected via USB. When
 * setting up those printers as raw printers, the internal command language can still be accessed
 * even though the device is not connected serially. Using a PrinterConnection, code that sends
 * commands via one of the other methods (Ethernet, Serial) can also be used with ordinary
 * USB-connected printers and the device may be identified by its user-specified name. Another
 * advantage is the ability to react to errors that happen after spooling of the print job.
 * <p>
 * While this connection is primarily for raw printing, it may also be used for ordinary printing.
 * <p>
 * Due to restrictions of the Java printer APIs, this connection is unidirectional - data cannot be
 * received from the printer and the connection behaves as if there is never any data to receive.
 * Also, every {@link #send(byte[])} call will spool its own print job - it is not possible to
 * combine multiple calls into one job since the ICommConnection API has no concept of "flushing"
 * and spooling on {@link #disconnect()} might not be what the user expects.
 */
public class PrinterConnection extends ICommConnection {
  private final Logger logger = LoggerFactory.getLogger(PrinterConnection.class);

  private String printerName;
  private PrintService printService;

  /**
   * Bogus receive timeout. You will never receive anything on this connection.
   */
  private int recvTimeout = 500;

  /**
   * Connection timeout. Like other implementations, we simply ignore it, as "connecting" is very
   * fast.
   */
  private int connectTimeout = 1000;

  /**
   * Return list of all possible printer names that can be passed to
   * {@link #PrinterConnection(String)}.
   *
   * These are the "user-friendly" names, usually as set up by the user when installing the printer.
   * Not all of them might be set up as raw printers and there is no way to identify raw printers
   * programmatically (as this depends on the device type, operating system and driver used).
   *
   * @return List of all printer names set up on the system.
   */
  public static List<String> getPrinterList() {
    PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
    List<String> ret = new ArrayList<>(printServices.length);

    for (PrintService ps : printServices) {
      PrinterName psName = ps.getAttribute(PrinterName.class);
      ret.add(psName.getValue());
    }

    return ret;
  }

  /**
   * Construct a new printer connection.
   *
   * The name used here is only expected to exist when the connection is connected via
   * {@link #connect()}.
   *
   * @param name The "user-friendly" name of the printer. It can be one of the names returned by
   *        {@link #getPrinterList()}.
   */
  public PrinterConnection(String name) {
    printerName = name;
  }

  @Override
  public void connect() throws CommConnectionException {
    /*
     * Find the printer corresponding to `printerName`. Doing this only now means we can throw
     * exceptions if the printer hasn't been found. Also applications expect the named connection to
     * exist only now.
     */
    printService = null;
    for (PrintService ps : PrintServiceLookup.lookupPrintServices(null, null)) {
      PrinterName psName = ps.getAttribute(PrinterName.class);

      if (psName.getValue().equals(printerName)) {
        printService = ps;
        return;
      }
    }

    throw new CommConnectionException(NO_DEVICES_FOUND,
        "Printer \"" + printerName + "\" not found.");
  }

  @Override
  public void disconnect() throws CommConnectionException {
    printService = null;
  }

  @Override
  public boolean isConnected() {
    return printService != null;
  }

  /**
   * Gets device information.
   *
   * For this implementation, all the attributes of the corresponding
   * {@link javax.print.PrintService} will be returned.
   *
   * @return Hashtable with the information
   */
  @Override
  public Hashtable<String, Object> getInfo() {
    Hashtable<String, Object> ret = new Hashtable<>();

    ret.put("type", "printer");

    if (!isConnected()) {
      /* we cannot throw exceptions in this method */
      return ret;
    }

    /* add all the attributes of the printService */
    for (Attribute attr : printService.getAttributes().toArray()) {
      ret.put(attr.getName(), attr);
    }

    return ret;
  }

  @Override
  public void setSettings(Hashtable<String, String> settings) {
    /*
     * Not yet implemented and probably not required.
     */
  }

  /**
   * Sends data to the connected device.
   *
   * On the printer implementation, each call to this method will spool a new print job.
   *
   * @param senddata Data to be sent to the device.
   *
   * @throws CommConnectionException Possible Errorcodes:
   *         <ul>
   *         <li>UNHANDLED_ERROR</li>
   *         <li>NOT_INITIALISE</li>
   *         </ul>
   */
  @Override
  public void send(byte[] senddata) throws CommConnectionException {
    if (!isConnected()) {
      throw new CommConnectionException(NOT_INITIALISED, "Not initialized");
    }
    if (getLogger().isTraceEnabled()) {
      getLogger().trace("{} send {}", toString(), new String(senddata));
    }

    SimpleDoc doc = new SimpleDoc(senddata, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);

    PrintRequestAttributeSet reqAttrs = new HashPrintRequestAttributeSet();

    /* the job name will be the FQ name of this class */
    reqAttrs.add(new JobName(getClass().toString(), Locale.getDefault()));

    DocPrintJob job = printService.createPrintJob();

    /*
     * The print job listener methods may be called in another thread, so we need a way to
     * synchronize with it.
     */
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<PrintJobEvent> jobEvent = new AtomicReference<>();

    job.addPrintJobListener(new PrintJobListener() {
      @Override
      public void printDataTransferCompleted(PrintJobEvent event) {}

      @Override
      public void printJobCompleted(PrintJobEvent event) {
        /* let send() return sucessfully */
        latch.countDown();
      }

      @Override
      public void printJobFailed(PrintJobEvent event) {
        /* Let send() return unsuccessfully. */
        jobEvent.set(event);
        latch.countDown();
      }

      @Override
      public void printJobCanceled(PrintJobEvent event) {
        /* Let send() return unsuccessfully. */
        jobEvent.set(event);
        latch.countDown();
      }

      @Override
      public void printJobRequiresAttention(PrintJobEvent event) {
        /* Let send() return unsuccessfully. */
        jobEvent.set(event);
        latch.countDown();
      }

      @Override
      public void printJobNoMoreEvents(PrintJobEvent event) {
        /*
         * We cannot know if the job was processed successfully but we also cannot assume it has
         * failed, so let send() return successfully.
         */
        latch.countDown();
      }
    });

    try {
      /* spools the print job */
      job.print(doc, reqAttrs);

      /*
       * The print job may be processed in another thread, so we wait for it to terminate, so we can
       * handle print errors. Also we will only return after the job completed successfully. FIXME:
       * We could implement a "send" timeout easily here.
       */
      latch.await();
    } catch (PrintException e) {
      throw new CommConnectionException(UNHANDLED_ERROR,
          "Error spooling print job: " + e.getMessage());
    } catch (InterruptedException e) {
      /*
       * Sadly we cannot just let this be thrown. Ignoring it would be just as bad, so throw a
       * CommConnectionException instead.
       */
      throw new CommConnectionException(UNHANDLED_ERROR, "Interruption: " + e.getMessage());
    }

    if (jobEvent.get() != null) {
      /* event saved by the PrintJobListener */
      throw new CommConnectionException(UNHANDLED_ERROR, jobEvent.get().toString());
    }
  }

  /**
   * Receives one character from the device.
   *
   * On this implementation, never will ever be returned, so it will wait for the receive timeout
   * configured and always return -1.
   *
   * @return Always -1 (timeout).
   *
   * @throws CommConnectionException When connection is uninitialized or the current thread is
   *         interrupted.
   */
  @Override
  public int recv() throws CommConnectionException {
    if (!isConnected()) {
      throw new CommConnectionException(NOT_INITIALISED, "Not initialized");
    }

    /*
     * We cannot receive on a raw printer, but applications may still call recv() to flush the
     * receive buffer. This sometimes exploits the fact that recv() returns -1 only after a timeout.
     * So instead of throwing an UnsupportedOperationException, we let the connection behave like a
     * bidirectional channel with no data.
     */
    try {
      Thread.sleep(recvTimeout);
    } catch (InterruptedException e) {
      /*
       * Sadly we cannot just let this be thrown. Ignoring it would be just as bad, so throw a
       * CommConnectionException instead.
       */
      throw new CommConnectionException(UNHANDLED_ERROR, "Interruption: " + e.getMessage());
    }

    return -1;
  }

  @Override
  public int dataAvailable() throws CommConnectionException {
    if (!isConnected()) {
      throw new CommConnectionException(NOT_INITIALISED, "Not initialized");
    }

    /* see above */
    return 0;
  }

  @Override
  public void setRecvTimeout(int timeout) throws CommConnectionException {
    if (timeout <= 0) {
      throw new CommConnectionException(SET_CONFIGURATION, "Timeout must be greater than 0");
    }

    recvTimeout = timeout;
  }

  @Override
  public int getRecvTimeout() {
    return recvTimeout;
  }

  @Override
  public void setConnectionTimeout(int timeout) {
    /* NOTE: We cannot throw exceptions here */
    connectTimeout = timeout;
  }

  @Override
  public int getConnectionTimeout() {
    return connectTimeout;
  }

  @Override
  public InputStream getInputStream() {
    /*
     * Better throw an exception now, than later when the user tries to use the object returned.
     */
    throw new UnsupportedOperationException("getInputStream() unsupported by PrinterConnection");
    // return null;
  }

  @Override
  public OutputStream getOutputStream() {
    throw new UnsupportedOperationException("getOutputStream() unsupported by PrinterConnection");
    // return null;
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
        getLogger().trace("{} recv {}", toString(), new String(b));
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

  private Logger getLogger() {
    return logger;
  }

  @Override
  public String toString() {
    return printerName;
  }
}
