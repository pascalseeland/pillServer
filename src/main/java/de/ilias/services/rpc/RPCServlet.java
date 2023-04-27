package de.ilias.services.rpc;

import de.ilias.services.lucene.index.RPCIndexHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.XmlRpcServlet;

public class RPCServlet extends XmlRpcServlet {

  private static Logger logger = LogManager.getLogger(RPCServlet.class);

  protected XmlRpcHandlerMapping newXmlRpcHandlerMapping() throws XmlRpcException {

    PropertyHandlerMapping mapping;

    mapping = new PropertyHandlerMapping();
    mapping.addHandler("RPCAdministration", de.ilias.services.rpc.RPCAdministration.class);
    mapping.addHandler("RPCIndexHandler", RPCIndexHandler.class);
    mapping.addHandler("RPCSearchHandler", de.ilias.services.lucene.search.RPCSearchHandler.class);
    mapping.addHandler("RPCTransformationHandler", de.ilias.services.transformation.RPCTransformationHandler.class);

    logger.error("Added RPC-Handlers");
    String[] methods = mapping.getListMethods();
    for (String method : methods) {
      logger.error(method);
    }

    return mapping;
  }
}
