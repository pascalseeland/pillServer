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

package de.ilias.services.object;

import de.ilias.services.db.DBFactory;
import de.ilias.services.lucene.index.CommandQueueElement;
import de.ilias.services.lucene.index.DocumentHandler;
import de.ilias.services.lucene.index.DocumentHandlerException;
import de.ilias.services.lucene.index.DocumentHolder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public class JDBCDataSource extends DataSource {

  private static Logger logger = LogManager.getLogger(JDBCDataSource.class);

  DBFactory dbFactory;

  String query;
  Vector<ParameterDefinition> parameters = new Vector<>();

  public JDBCDataSource(int type, DBFactory dbFactory) {
    super(type);
    this.dbFactory = dbFactory;
  }

  /**
   * @return the query
   */
  public String getQuery() {
    return query;
  }

  /**
   * @param query the query to set
   */
  public void setQuery(String query) {
    this.query = query;
  }

  public PreparedStatement getStatement() throws SQLException {

    return this.dbFactory.getPreparedStatement(getQuery());
  }

  /**
   * @return the parameters
   */
  public Vector<ParameterDefinition> getParameters() {
    return parameters;
  }

  /**
   * @param parameters the parameters to set
   */
  public void setParameters(Vector<ParameterDefinition> parameters) {
    this.parameters = parameters;
  }

  public void addParameter(ParameterDefinition parameter) {
    this.parameters.add(parameter);
  }

  @Override
  public String toString() {

    StringBuilder out = new StringBuilder();

    out.append("New JDBC Data Source");
    out.append("\n");
    out.append("Query: ").append(getQuery());
    out.append("\n");

    for (Object param : getParameters()) {

      out.append(param.toString());
    }
    out.append(super.toString());

    return out.toString();
  }

  /**
   * @see de.ilias.services.lucene.index.DocumentHandler#createDocument(de.ilias.services.lucene.index.CommandQueueElement)
   */
  public List<DocumentHolder>  createDocument(CommandQueueElement el) throws DocumentHandlerException {

    return createDocument(el, null);
  }

  /**
   * TODO remove the synchronized (cannot use more than one result set for one prepared statement)
   * @see de.ilias.services.lucene.index.DocumentHandler#createDocument(de.ilias.services.lucene.index.CommandQueueElement, java.sql.ResultSet)
   */
  public List<DocumentHolder> createDocument(CommandQueueElement el, ResultSet parentResult) throws DocumentHandlerException {

    logger.trace("Handling data source: " + getType());
    List<DocumentHolder> documentHolders = new LinkedList<>();
    try {
      // Create Statement for JDBC data source
      DocumentHolder doc = new DocumentHolder();

      int paramNumber = 1;
      for (ParameterDefinition param : getParameters()) {

        param.writeParameter(getStatement(), paramNumber++, el, parentResult);
      }

      logger.trace(getStatement());
      ResultSet res = getStatement().executeQuery();

      while (res.next()) {

        logger.trace("Found new result");
        for (FieldDefinition field : getFields()) {
          field.writeDocument(doc, res);
        }

        // Add subitems from additional data sources
        for (DocumentHandler ds : getDataSources()) {
          documentHolders.addAll(ds.createDocument(el, res));
        }

        // Finally addDocument to index
        documentHolders.add(doc);
      }
      try {
        res.close();
      } catch (SQLException e) {
        logger.warn("Cannot close result set", e);
      }
    } catch (SQLException e) {
      logger.error("Cannot parse data source.", e);
      throw new DocumentHandlerException(e);
    }
    return documentHolders;
  }
}
