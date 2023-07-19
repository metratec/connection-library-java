/*******************************************************************************
 * Copyright (c) 2023 by metraTec GmbH
 * All rights reserved.
 *******************************************************************************/
package test;

import java.util.Map;

import com.metratec.lib.connection.PrinterConnection;

/**
 * @author man
 *
 */
public class TestPrinter {
  /**
   * @param args program arguments - not used
   * @throws Exception if an error occurs
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("usage: java TestPrinter [name]");

      /* list available printers */
      System.out.println("Available printers:");
      for (String name : PrinterConnection.getPrinterList()) {
        System.out.println(name);
      }
      return;
    }

    PrinterConnection conn = new PrinterConnection(args[0]);

    conn.connect();

    System.out.println("Attributes of printer \"" + args[0] + "\":");
    for (Map.Entry<String, Object> a : conn.getInfo().entrySet()) {
      System.out.println(a.getKey() + " = " + a.getValue().toString());
    }

    /*
     * Assuming that the selected printer is ZPL-compatible, this will print a revision label.
     */
    conn.send("^XA^LH155,0^FO0,35^A0N,17,17^FDRev. 010401030107^FS^XZ");

    conn.disconnect();
  }
}
