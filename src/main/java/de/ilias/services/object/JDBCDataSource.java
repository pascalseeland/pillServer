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
import de.ilias.services.lucene.index.DocumentExtractor;
import de.ilias.services.lucene.index.DocumentHandlerException;
import de.ilias.services.lucene.index.DocumentHolder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public class JDBCDataSource extends DataSource {

  private static final Logger logger = LogManager.getLogger(JDBCDataSource.class);

  String query;
  private final Vector<ParameterDefinition> parameters = new Vector<>();

  public JDBCDataSource() {
    super(TYPE.JDBC);
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

    return DBFactory.getPreparedStatement(getQuery());
  }

  /**
   * @return the parameters
   */
  public Vector<ParameterDefinition> getParameters() {
    return parameters;
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
   * @see DocumentExtractor#extractDocument(CommandQueueElement, DocumentHolder)
   */
  public void extractDocument(CommandQueueElement el, DocumentHolder dh) throws DocumentHandlerException {
    extractDocument(el, dh, null);
  }

  /**
   * TODO remove the synchronized (cannot use more than one result set for one prepared statement)
   * @see DocumentExtractor#extractDocument(CommandQueueElement, DocumentHolder, ResultSet)
   */
  public void extractDocument(CommandQueueElement el, DocumentHolder dh, ResultSet parentResult) throws DocumentHandlerException {

    logger.trace("Handling data source: " + getType());

    try {
      int paramNumber = 1;
      for (ParameterDefinition param : getParameters()) {

        param.writeParameter(getStatement(), paramNumber++, el, parentResult);
      }

      logger.trace(getStatement());
      ResultSet res = getStatement().executeQuery();

      while (res.next()) {

        logger.trace("Found new result");
        for (FieldDefinition field : getFields()) {
          field.writeDocument(res, dh);
        }

        // Add subitems from additional data sources
        for (DocumentExtractor ds : getDataSources()) {
          ds.extractDocument(el, dh, res);
        }
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

  }
}
