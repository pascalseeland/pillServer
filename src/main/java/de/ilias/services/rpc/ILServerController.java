package de.ilias.services.rpc;

import de.ilias.services.lucene.search.RPCSearchHandler;
import de.ilias.services.rpc.xml.MethodCallType;
import de.ilias.services.rpc.xml.MethodResponseType;
import de.ilias.services.rpc.xml.ObjectFactory;
import de.ilias.services.rpc.xml.ParamType;
import de.ilias.services.rpc.xml.ValueType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.JAXBElement;

@Path("/")
public class ILServerController {

  private static Logger logger = LogManager.getLogger(ILServerController.class);

  @Inject
  RPCSearchHandler rpcSearchHandler;

  @POST
  @Path("/RPC2")
  @Consumes(value = MediaType.TEXT_XML)
  @Produces(value = MediaType.APPLICATION_XML)
  public JAXBElement<MethodResponseType> xmlRPCCall(JAXBElement<MethodCallType> wrappedRpcCall) {
    MethodCallType rpcCall = wrappedRpcCall.getValue();
    logger.debug(rpcCall.getMethodName());
    String retval = null;
    if ("RPCSearchHandler.search".equals(rpcCall.getMethodName())) {
      String param0 = null, param1 = null;
      int param2 = -1;
      int paramsSet = 0;
      for (ParamType paramType : rpcCall.getParams().getParam()) {
        for (Serializable s : paramType.getValue().getContent()) {
          if (s instanceof JAXBElement) {
            if (0 == paramsSet && "java.lang.String" == ((JAXBElement<?>) s).getDeclaredType().getName()) {
              param0 = ((String) ((JAXBElement<?>) s).getValue());
              paramsSet = 1;
            }
            if (1 == paramsSet && "java.lang.String" == ((JAXBElement<?>) s).getDeclaredType().getName()) {
              param1 = ((String) ((JAXBElement<?>) s).getValue());
              paramsSet = 2;
            }
            if (0 == paramsSet && "java.lang.Integer" == ((JAXBElement<?>) s).getDeclaredType().getName()) {
              param2 = ((Integer) ((JAXBElement<?>) s).getValue());
              paramsSet = 2;
            }
          }
        }
      }
      retval = rpcSearchHandler.search(param0, param1, param2);
    }
    ObjectFactory objectFactory = new ObjectFactory();
    MethodResponseType.Params params = objectFactory.createMethodResponseTypeParams();
    ParamType pt = objectFactory.createParamType();
    ValueType vt = objectFactory.createValueType();
    vt.getContent().add(objectFactory.createValueTypeString(retval));
    pt.setValue(vt);
    MethodResponseType methodResponseType = objectFactory.createMethodResponseType();
    methodResponseType.setParams(params);
    return objectFactory.createMethodResponse(methodResponseType);
  }

  @POST
  @Path("/RPC3")
  @Consumes(value = MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(value = MediaType.APPLICATION_XML)
  public MethodResponseType xmlRPCCall(String payloadToSend) {
    logger.debug("xml" + payloadToSend);
    return new MethodResponseType();
  }
}

