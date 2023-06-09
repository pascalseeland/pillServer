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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @version $Id$
 */
public class ILServer {

  private String version = "4.4.0.1";

  private String[] arguments;
  private String command;

  private static final Logger logger = LogManager.getLogger(ILServer.class);

  public ILServer(String[] args) {

    arguments = args;
  }

  public static void main(String[] args) {

    ILServer server = null;

    server = new ILServer(args);
    server.handleRequest();
  }

  /**
   * @return success status
   */
  private boolean handleRequest() {

    if (arguments.length < 1) {
      logger.error(getUsage());
      return false;
    }

    if (arguments.length == 1) {
      command = arguments[0];
      if (command.compareTo("version") == 0) {
        logger.info("ILIAS java server version \"" + version + "\"");
        return true;
      }
    }
    command = arguments[1];
    if (command.compareTo("start") == 0) {
      if (arguments.length != 2) {
        logger.error("Usage: java -jar ilServer.jar PATH_TO_SERVER_INI start");
        return false;
      }
      return startServer();
    } else if (command.compareTo("stop") == 0) {
      if (arguments.length != 2) {
        logger.error("Usage: java -jar ilServer.jar PATH_TO_SERVER_INI stop");
        return false;
      }
      return stopServer();
    } else if (command.compareTo("createIndex") == 0) {
      if (arguments.length != 3) {
        logger.error("Usage java -jar ilServer.jar PATH_TO_SERVER_INI createIndex CLIENT_KEY");
        return false;
      }
      return createIndexer();
    } else if (command.compareTo("updateIndex") == 0) {
      if (arguments.length < 3) {
        logger.error("Usage java -jar ilServer.jar PATH_TO_SERVER_INI updateIndex CLIENT_KEY");
        return false;
      }
      return updateIndexer();
    } else if (command.compareTo("search") == 0) {
      if (arguments.length != 4) {
        logger.error("Usage java -jar ilServer.jar PATH_TO_SERVER_INI CLIENT_KEY search QUERY_STRING");
        return false;
      }
      return startSearch();

    } else if (command.compareTo("status") == 0) {
      if (arguments.length != 2) {
        logger.error("Usage java -jar ilServer.jar PATH_TO_SERVER_INI status");
        return false;
      }
      return getStatus();
    } else {
      logger.error(getUsage());
      return false;
    }
  }

  private boolean createIndexer() {

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
      params[1] = false;
      client.execute("RPCIndexHandler.index", params);
      return true;
    } catch (Exception e) {
      logger.error(e);
      logger.fatal(e.getMessage());
      System.exit(1);
    }
    return false;
  }

  private boolean updateIndexer() {

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
      params[1] = true;
      client.execute("RPCIndexHandler.index", params);
      return true;
    } catch (Exception e) {
      logger.error(e);
      logger.fatal(e.getMessage());
      System.exit(1);
    }
    return false;
  }

  private boolean startSearch() {

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
      return true;
    } catch (Exception e) {
      logger.error(e);
      logger.fatal(e.getMessage());
      System.exit(1);
    }
    return false;
  }

  /**
   * Start RPC services
   */
  private boolean startServer() {

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
      Thread shutdownListener = new Thread() {
        public void run() {

          logger.info("Received stop signal");

          // Closing all index writers
          IndexHolder.closeAllWriters();
          rpc.shutdown();

          // Set server status inactive
          ILServerStatus.setActive(false);
        }
      };
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
      return true;

    } catch (ConfigurationException e) {
      //logger.error(e);
      System.exit(1);
      return false;
    } catch (InterruptedException e) {
      logger.error("VM did not allow to sleep. Aborting!");
    } catch (XmlRpcException e) {
      logger.error("Error starting server: " + e);
      System.exit(1);
    } catch (IOException e) {
      logger.error("IOException " + e.getMessage());
    }
    return false;
  }

  /**
   * Call RPC stop method, which will stop the WebServer
   * and after that stop the execution of the main thread
   */
  private boolean stopServer() {

    XmlRpcClient client;
    IniFileParser parser;

    try {
      parser = new IniFileParser();
      parser.parseServerSettings(arguments[0], false);

      client = initRpcClient();
      client.execute("RPCAdministration.stop", Collections.EMPTY_LIST);
      return true;
    } catch (ConfigurationException e) {
      logger.error("Configuration " + e.getMessage());
    } catch (XmlRpcException e) {
      logger.error("XMLRPC " + e.getMessage());
    } catch (IOException e) {
      logger.error("IOException " + e.getMessage());
    }
    return false;
  }

  private boolean getStatus() {

    XmlRpcClient client;
    IniFileParser parser;

    String status;

    try {
      parser = new IniFileParser();
      parser.parseServerSettings(arguments[0], false);

      client = initRpcClient();
      status = (String) client.execute("RPCAdministration.status", Collections.EMPTY_LIST);
      logger.info(status);
      return true;
    } catch (ConfigurationException e) {
      logger.error("Configuration " + e.getMessage());
    } catch (XmlRpcException e) {
      logger.error(ILServerStatus.STOPPED);
      System.exit(1);
    } catch (IOException e) {
      logger.error(ILServerStatus.STOPPED);
      System.exit(1);
    }
    return false;
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
  private XmlRpcClient initRpcClient() throws ConfigurationException, MalformedURLException {
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

