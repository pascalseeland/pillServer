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

package de.ilias.services.lucene.index;

import de.ilias.services.settings.ClientSettings;
import de.ilias.services.settings.ConfigurationException;
import de.ilias.services.settings.ServerSettings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Capsulates the interaction between IndexReader and IndexWriter
 * This class is a singleton for each index path.
 *
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 * @author Pascal Seeland <pascal.seeland@tik.uni-stuttgart.de>
 */
@ApplicationScoped
public class IndexHolder implements AutoCloseable {

  private static Logger logger = LogManager.getLogger(IndexHolder.class);

  private static final int MAX_NUM_SEGMENTS = 100;

  public static int SEARCH_LIMIT = 100;

  @Inject
  ClientSettings clientSettings;

  private IndexWriter writer;

  private IndexSearcher searcher;

  /**
   * @return the searcher
   */
  public IndexSearcher getSearcher() {
    return searcher;
  }

  public void deleteIndex() throws IOException {
    writer.deleteAll();
    searcher.getIndexReader().close();
    searcher = new IndexSearcher(DirectoryReader.open(writer));
  }

  @PostConstruct
  public void init() throws IOException, ConfigurationException {
    try {
      FSDirectory directory = IndexDirectoryFactory.getDirectory(clientSettings.getIndexPath());
      IndexWriterConfig writerConfig = new IndexWriterConfig(new StandardAnalyzer());
      writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND)
          .setRAMBufferSizeMB(ServerSettings.getInstance().getRAMSize());
      writer = new IndexWriter(directory, writerConfig);
      IndexReader reader = DirectoryReader.open(writer);
      searcher = new IndexSearcher(reader);
      logger.info("Initalized the search index: {} ", searcher);
    } catch (IOException | ConfigurationException e) {
      throw e;
    }

  }

  public void addDocument(Document document) throws IOException {
    writer.addDocument(document);
  }

  public void deleteDocument(String documentId) throws IOException {
    writer.deleteDocuments(new Term("objId", documentId));
  }

  public void commitAndForceMerge() throws IOException {
    writer.commit();
    writer.forceMerge(IndexHolder.MAX_NUM_SEGMENTS);
    // Refresh index reader
    searcher.getIndexReader().close();
    searcher = new IndexSearcher(DirectoryReader.open(writer));
  }

  @PreDestroy
  public void close() {

    try {
      writer.close();
      searcher.getIndexReader().close();
      IndexDirectoryFactory.getDirectory(clientSettings.getIndexPath()).close();
    } catch (CorruptIndexException e) {
      logger.fatal("IndexController corrupted." + e);
    } catch (IOException e) {
      logger.fatal("Error closing writer." + e);
    }
  }

}
