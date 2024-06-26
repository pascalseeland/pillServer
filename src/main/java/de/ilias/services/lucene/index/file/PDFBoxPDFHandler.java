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
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public class PDFBoxPDFHandler implements FileHandler {

  private final Logger logger = LogManager.getLogger(PDFBoxPDFHandler.class);

  /**
   * @see de.ilias.services.lucene.index.file.FileHandler#getContent(java.io.InputStream)
   */
  public String getContent(InputStream is) throws FileHandlerException {

    PDDocument pddo = null;
    PDFTextStripper stripper;
    String str = "";

    try {

      pddo = Loader.loadPDF(new RandomAccessReadBuffer(is));

      if (pddo.isEncrypted()) {
        logger.warn("PDF Document is encrypted. Trying empty password...");
        return "";
      }
      stripper = new PDFTextStripper();
      str = stripper.getText(pddo);
    } catch (NumberFormatException e) {
      logger.warn("Invalid PDF version number given. Aborting");
    } catch (IOException e) {
      logger.warn(e.getMessage());
      throw new FileHandlerException(e);
    } catch (Exception e) {
      logger.error(e.getMessage());
      throw new FileHandlerException(e);
    } finally {
      try {
        if (pddo != null) {
          pddo.close();
        }
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
      }
    }
    return str;
  }

}
