/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/
package test;

import com.metratec.lib.connection.ICommConnection;
import com.metratec.lib.connection.UsbConnection;

/**
 * @author man
 *
 */
public class BenchmarkUSB {
  private static final int ITERATIONS = 1000;

  /**
   * @param args program arguments - not used
   * @throws Exception if an error occurs
   */
  public static void main(String[] args) throws Exception {
    ICommConnection comm;
    long startTime;
    long estimatedTime = 0;

    comm = new UsbConnection(args[0], 115200);
    // comm = new RS232Connection(args[0], 115200, 8, 1, 0, 0);
    comm.connect();

    for (int i = 0; i < ITERATIONS; i++) {
      comm.send("REV\r");

      startTime = System.nanoTime();
      String s = comm.recv(13);
      estimatedTime += System.nanoTime() - startTime;

      if (!s.equals("Comet_ISO_B     0105\r"))
        throw new Exception("Unexpected REV response");
    }

    comm.disconnect();

    System.out.println("Estimated runtime: " + estimatedTime / ITERATIONS);
  }
}

