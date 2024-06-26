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

package de.ilias.services.lucene.index.file;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Handle plain text files
 *
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @version $Id$
 */
public class PlainTextHandler implements FileHandler {

  private final Logger logger = LogManager.getLogger(PlainTextHandler.class);

  /**
   *
   */
  public PlainTextHandler() {
  }

  /**
   * @see de.ilias.services.lucene.index.file.FileHandler#getContent(java.io.InputStream)
   */
  public String getContent(InputStream is) throws FileHandlerException {

    StringBuilder content = new StringBuilder();
    BufferedReader br = new BufferedReader(new InputStreamReader(is));

    try {
      String line;
      while ((line = br.readLine()) != null) {
        content.append(' ');
        content.append(line);
      }
      return content.toString();
    } catch (IOException e) {
      throw new FileHandlerException("Cannot read plain text file: " + e);
    } finally {
      try {
        br.close();
      } catch (IOException e) {
        logger.error("Error closing BufferedReader", e);
      }
    }
  }

}
