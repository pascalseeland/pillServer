/*
        +-----------------------------------------------------------------------------+
        | ILIAS open source                                                           |
        +-----------------------------------------------------------------------------+
        | Copyright (c) 1998-2001 ILIAS open source, University of Cologne            |
        |                                                                             |
        | This program is free software; you can redistribute it and/or               |
        | modify it under the terms of the GNU General Public License                 |
        | as published by the Free Software Foundation; either version 2              |
        | of the License, or (at your option) any later version.                      |
        |                                                                             |
        | This program is distributed in the hope that it will be useful,             |
        | but WITHOUT ANY WARRANTY; without even the implied warranty of              |
        | MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the               |
        | GNU General Public License for more details.                                |
        |                                                                             |
        | You should have received a copy of the GNU General Public License           |
        | along with this program; if not, write to the Free Software                 |
        | Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. |
        +-----------------------------------------------------------------------------+
*/

package de.ilias;

import de.ilias.services.lucene.index.IndexHolder;
import de.ilias.services.rpc.RPCServer;
import de.ilias.services.settings.ClientSettings;
import de.ilias.services.settings.ConfigurationException;
import de.ilias.services.settings.IniFileParser;
import de.ilias.services.settings.ServerSettings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.quartz.SchedulerException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @version $Id$
 */
public class ILServer {

  private static final String version = "4.4.0.1";

  private final String[] arguments;

  private static final Logger logger = LogManager.getLogger(ILServer.class);

  public ILServer(String[] args) {

    arguments = args;
  }

  @SuppressWarnings("unused")
  public static void main(String[] args) {

    ILServer server;

    server = new ILServer(args);
    server.handleRequest();
  }

  /**
   *
   */
  private void handleRequest() {

    if (arguments.length < 1) {
      logger.error(getUsage());
      return;
    }

    String command;
    if (arguments.length == 1) {
      command = arguments[0];
      if (command.compareTo("version") == 0) {
        logger.info("ILIAS java server version \"" + version + "\"");
        return;
      }
    }
    command = arguments[1];
    if (command.compareTo("start") == 0) {
      if (arguments.length != 2) {
        logger.error("Usage: java -jar ilServer.jar PATH_TO_SERVER_INI start");
        return;
      }
      startServer();
    } else if (command.compareTo("stop") == 0) {
      if (arguments.length != 2) {
        logger.error("Usage: java -jar ilServer.jar PATH_TO_SERVER_INI stop");
        return;
      }
      stopServer();
    } else if (command.compareTo("createIndex") == 0) {
      if (arguments.length != 3) {
        logger.error("Usage java -jar ilServer.jar PATH_TO_SERVER_INI createIndex CLIENT_KEY");
        return;
      }
      createIndexer(false);
    } else if (command.compareTo("updateIndex") == 0) {
      if (arguments.length < 3) {
        logger.error("Usage java -jar ilServer.jar PATH_TO_SERVER_INI updateIndex CLIENT_KEY");
        return;
      }
      createIndexer(true);
    } else if (command.compareTo("search") == 0) {
      if (arguments.length != 4) {
        logger.error("Usage java -jar ilServer.jar PATH_TO_SERVER_INI CLIENT_KEY search QUERY_STRING");
        return;
      }
      startSearch();
    } else {
      logger.error(getUsage());
    }
  }

  private void createIndexer(boolean incremental) {

    XmlRpcClient client;
    IniFileParser parser;

    try {
      parser = new IniFileParser();
      parser.parseServerSettings(arguments[0], true);

      if (!ClientSettings.exists(arguments[2])) {
        throw new ConfigurationException("Unknown client given: " + arguments[2]);
      }

      client = initRpcClient();
      Object[] params = new Object[2];
      params[0] = arguments[2];
      params[1] = incremental;
      client.execute("RPCIndexHandler.index", params);
    } catch (Exception e) {
      logger.error(e);
      logger.fatal(e.getMessage());
      System.exit(1);
    }
  }

  private void startSearch() {

    XmlRpcClient client;
    IniFileParser parser;

    try {
      parser = new IniFileParser();
      parser.parseServerSettings(arguments[0], true);

      if (!ClientSettings.exists(arguments[2])) {
        throw new ConfigurationException("Unknown client given: " + arguments[2]);
      }

      client = initRpcClient();
      Object[] params = new Object[3];
      params[0] = arguments[2];
      params[1] = arguments[3];
      params[2] = 1;
      String response = (String) client.execute("RPCSearchHandler.search", params);
      logger.debug(response);
    } catch (Exception e) {
      logger.error(e);
      logger.fatal(e.getMessage());
      System.exit(1);
    }
  }

  /**
   * Start RPC services
   */
  private void startServer() {

    ServerSettings settings;
    RPCServer rpc;
    XmlRpcClient client;
    IniFileParser parser;

    try {

      parser = new IniFileParser();
      parser.parseServerSettings(arguments[0], true);

      client = initRpcClient();

      // Check if server is already running
      try {
        client.execute("RPCAdministration.status", Collections.EMPTY_LIST);
        logger.error("Server already started. Aborting");
        System.exit(1);
      } catch (XmlRpcException e) {
        logger.info("No server running. Starting new instance...");
      }

      settings = ServerSettings.getInstance();
      logger.info("New rpc server");
      rpc = RPCServer.getInstance(settings.getHost(), settings.getPort());
      logger.info("Server start");
      rpc.start();

      client = initRpcClient();
      client.execute("RPCAdministration.start", Collections.EMPTY_LIST);
      Thread shutdownListener = new Thread(() -> {

        logger.info("Received stop signal");

        // Closing all index writers
        IndexHolder.closeAllWriters();
        try {
          rpc.shutdown();
        } catch (SchedulerException e) {
          logger.error("Error stopping scheduler", e);
        }

          // Set server status inactive
        ILServerStatus.setActive(false);
      });
      Runtime.getRuntime().addShutdownHook(shutdownListener);
      // Check if webserver is alive
      // otherwise stop execution
      while (true) {

        Thread.sleep(3000);
        if (!rpc.isAlive()) {
          rpc.shutdown();
          break;
        }
      }
      logger.info("WebServer shutdown. Aborting...");

    } catch (ConfigurationException e) {
      //logger.error(e);
      System.exit(1);
    } catch (InterruptedException e) {
      logger.error("VM did not allow to sleep. Aborting!");
    } catch (XmlRpcException e) {
      logger.error("Error starting server: " + e);
      System.exit(1);
    } catch (IOException e) {
      logger.error("IOException " + e.getMessage());
    } catch (SchedulerException e) {
      logger.error("Issue with scheduler", e);
    }
  }

  /**
   * Call RPC stop method, which will stop the WebServer
   * and after that stop the execution of the main thread
   */
  private void stopServer() {

    XmlRpcClient client;
    IniFileParser parser;

    try {
      parser = new IniFileParser();
      parser.parseServerSettings(arguments[0], false);

      client = initRpcClient();
      client.execute("RPCAdministration.stop", Collections.EMPTY_LIST);
    } catch (ConfigurationException e) {
      logger.error("Configuration " + e.getMessage());
    } catch (XmlRpcException e) {
      logger.error("XMLRPC " + e.getMessage());
    } catch (IOException e) {
      logger.error("IOException " + e.getMessage());
    }
  }

  /**
   * @return String usage
   */
  private String getUsage() {

    return "Usage: java -jar ilServer.jar PATH_TO_SERVER_INI start|stop|createIndex|updateIndex|search PARAMS";
  }

  /**
   * @return XmlRpcClient
   */
  private XmlRpcClient initRpcClient() throws MalformedURLException {
    XmlRpcClient client;
    XmlRpcClientConfigImpl config;
    ServerSettings settings;

    settings = ServerSettings.getInstance();
    config = new XmlRpcClientConfigImpl();
    config.setServerURL(new URL(settings.getServerUrl()));
    config.setConnectionTimeout(10000);
    config.setReplyTimeout(0);

    client = new XmlRpcClient();
    client.setTransportFactory(new XmlRpcCommonsTransportFactory(client));
    client.setConfig(config);

    return client;
  }
}

