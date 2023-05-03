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

package de.ilias.services.lucene.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Vector;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public class QueryRewriter {

  public static final int MODE_SEARCH = 1;
  public static final int MODE_HIGHLIGHT = 2;
  public static final int MODE_MAIL_HIGHLIGHT = 3;

  public static final int MODE_USER_HIGHLIGHT = 4;

  private static Logger logger = LogManager.getLogger(QueryRewriter.class);

  private String query;
  private StringBuffer rewritten;
  private int mode;
  private Vector<Integer> objIds = new Vector<>();

  public QueryRewriter(int mode, String query) {

    this.query = query;
    this.mode = mode;
    rewritten = new StringBuffer();

  }

  public String rewrite() {

    switch (mode) {
      case MODE_SEARCH:
        return rewriteSearch();
      case MODE_HIGHLIGHT:
        return rewriteHighlight();
      case MODE_USER_HIGHLIGHT:
        return rewriteUserHighlight();
    }

    return query;
  }

  public String rewrite(int userId, int folderId) {

    if (mode == MODE_MAIL_HIGHLIGHT) {
      return rewriteMailHighlight(userId, folderId);
    }
    return query;
  }

  public String rewrite(Vector<Integer> objIds) {

    setObjIds(objIds);
    return rewrite();
  }

  private String rewriteHighlight() {

    rewritten.append("( ");
    rewritten.append(query);
    rewritten.append(" ) AND ((");
    for (Integer objId : objIds) {
      rewritten.append("objId:");
      rewritten.append(objId);
      rewritten.append(' ');
    }
    rewritten.append(" ) AND docType:separated)");

    logger.debug("Searching for: " + rewritten);
    return rewritten.toString();
  }

  /**
   * rewrite mail search
   */
  private String rewriteMailHighlight(int userId, int folderId) {

    rewritten.append("( ");
    rewritten.append(query);
    rewritten.append(") AND ((");
    rewritten.append("objId:");
    rewritten.append(userId);
    if (folderId > 0) {
      rewritten.append(" AND ");
      rewritten.append("mfolder_id:");
      rewritten.append(folderId);
    }
    rewritten.append(") AND docType:separated) ");

    logger.debug("Searching for: " + rewritten);
    return rewritten.toString();
  }

  private String rewriteSearch() {

    rewritten.append("(");
    rewritten.append(query);
    rewritten.append(")");
    rewritten.append(" AND +docType:combined");

    logger.debug("Searching for: " + rewritten);
    return rewritten.toString();
  }

  /**
   * Rewrite user search
   */
  private String rewriteUserHighlight() {

    rewritten.append("(");
    rewritten.append(query);
    rewritten.append(")");
    rewritten.append(" AND type:usr");

    logger.info("Searching for:" + rewritten);
    return rewritten.toString();
  }

  /**
   * @param objIds the objIds to set
   */
  public void setObjIds(Vector<Integer> objIds) {
    this.objIds = objIds;
  }

}
