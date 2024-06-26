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

package de.ilias.services.rpc;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

import de.ilias.services.lucene.index.UpdateIndexJob;
import de.ilias.services.settings.ConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import java.net.InetAddress;



/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @version $Id$
 */
public class RPCServer {

  private static RPCServer instance = null;
  private static final Logger logger = LogManager.getLogger(RPCServer.class);

  private Scheduler scheduler;
  private WebServer server;
  private InetAddress host = null;
  private int port = 0;
  private boolean alive = false;

  private RPCServer(InetAddress host, int port) throws XmlRpcException, SchedulerException {

    logger.info("New RPCServer construct.");
    setHost(host);
    setPort(port);
    initServer();
    initScheduler();
  }

  public static RPCServer getInstance(InetAddress host, int port) throws XmlRpcException, SchedulerException {

    if (RPCServer.instance == null) {
      return RPCServer.instance = new RPCServer(host, port);
    }
    return instance;
  }

  public static RPCServer getInstance() {

    return RPCServer.instance;
  }

  /**
   * @return the host
   */
  public InetAddress getHost() {
    return host;
  }

  /**
   * @param host the host to set
   */
  public void setHost(InetAddress host) {
    this.host = host;
  }

  /**
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   * @param port the port to set
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * @return the alive
   */
  public boolean isAlive() {
    return alive;
  }

  /**
   * @param alive the alive to set
   */
  public void setAlive(boolean alive) {
    this.alive = alive;
  }

  /**
   * Init WebServer
   */
  private void initServer() throws XmlRpcException {

    XmlRpcServer rpcServer;
    XmlRpcServerConfigImpl config;

    logger.info("Init Webserver...");

    server = new WebServer(getPort(), getHost());
    rpcServer = server.getXmlRpcServer();
    rpcServer.setHandlerMapping(addHandlers());

    config = (XmlRpcServerConfigImpl) rpcServer.getConfig();
    config.setKeepAliveEnabled(true);
    config.setEncoding("UTF8");
  }

  public void initScheduler() throws SchedulerException {
    scheduler = StdSchedulerFactory.getDefaultScheduler();
    JobDetail job = JobBuilder.newJob(UpdateIndexJob.class)
            .withIdentity("updateJob", "group1")
            .usingJobData("jobSays", "Hello World!")
            .usingJobData("myFloatValue", 3.141f)
            .build();
    Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity("myTrigger", "group1")
            .startNow()
            .withSchedule(simpleSchedule()
                    .withIntervalInSeconds(60)
                    .repeatForever())
            .build();
    scheduler.scheduleJob(job, trigger);
  }

  /**
   * Start Webserver
   */
  public void start() throws ConfigurationException {

    try {
      logger.info("Starting ILIAS RPC-Server...");
      scheduler.start();
      logger.info("Started quartz scheduler");
      server.start();
      logger.debug("Using host :" + getHost().toString());
      logger.debug("Using port :" + getPort());
      logger.info("Waiting for connections...");
      alive = true;
    } catch (Exception e) {
      logger.error("Cannot bind to host: " + getHost() + ", port: " + port + " " + e);
      throw new ConfigurationException(e.getMessage());
    } catch (Throwable e) {
      logger.error(e);
      throw new ConfigurationException(e.getMessage());
    }
  }

  /**
   * Stop Webserver
   */
  public void shutdown() throws SchedulerException {

    logger.info("Stopping webserver...");
    server.shutdown();
    logger.info("Stopping scheduler");
    scheduler.shutdown();
    alive = false;
  }

  private PropertyHandlerMapping addHandlers() throws XmlRpcException {

    PropertyHandlerMapping mapping;

    mapping = new PropertyHandlerMapping();
    mapping.addHandler("RPCDebug", de.ilias.services.rpc.RPCDebug.class);
    mapping.addHandler("RPCAdministration", de.ilias.services.rpc.RPCAdministration.class);
    mapping.addHandler("RPCSearchHandler", de.ilias.services.lucene.search.RPCSearchHandler.class);
    mapping.addHandler("RPCTransformationHandler", de.ilias.services.transformation.RPCTransformationHandler.class);

    logger.info("Added RPC-Handlers");
    String[] methods = mapping.getListMethods();
    for (String method : methods) {
      logger.info(method);
    }

    return mapping;
  }
}
