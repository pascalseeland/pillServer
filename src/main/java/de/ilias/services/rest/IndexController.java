package de.ilias.services.rest;

import de.ilias.services.lucene.index.RPCIndexHandler;
import de.ilias.services.lucene.search.RPCSearchHandler;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/index")
public class IndexController {

  @Inject
  RPCIndexHandler rpcIndexHandler;
  @Inject
  RPCSearchHandler rpcSearchHandler;

  @ConfigProperty(name = "pillServer.ClientId")
  String clientID;

  @GET
  public String list() {
    return "/update is available as post";
  }

  @GET
  @Path("search")
  public String search(@QueryParam("query") String query) {
    return rpcSearchHandler.search(clientID, query,1);
  }

  @POST
  @Path("/update")
  public String update(String data) {
    rpcIndexHandler.index(false);
    return "success";
  }
}
