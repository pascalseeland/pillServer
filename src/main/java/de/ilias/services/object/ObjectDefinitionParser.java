package de.ilias.services.object;

import de.ilias.services.lucene.index.FieldInfo;
import de.ilias.services.lucene.index.file.path.PathCreatorFactory;
import de.ilias.services.settings.ClientSettings;
import de.ilias.services.settings.ConfigurationException;
import de.ilias.services.settings.LocalSettings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Parser for  Lucene object definitions.
 *
 * @author Stefan Meyer <smeyer.ilias@gmx.de>
 */
public class ObjectDefinitionParser {

  private Logger logger = LogManager.getLogger(ObjectDefinitionParser.class);
  private Vector<File> objectPropertyFiles = new Vector<File>();
  private ClientSettings settings;
  private ObjectDefinitions definitions;

  public ObjectDefinitionParser() throws ConfigurationException {

    settings = ClientSettings.getInstance(LocalSettings.getClientKey());
    // reset definitions to avoíd duplicate entries
    definitions = ObjectDefinitions.getInstance(settings.getAbsolutePath());
    definitions.reset();
    FieldInfo.getInstance(LocalSettings.getClientKey());
  }

  public ObjectDefinitionParser(Vector<File> objectPropertyFiles) throws ConfigurationException {

    this();
    this.objectPropertyFiles = objectPropertyFiles;

  }

  public boolean parse() throws ObjectDefinitionException {

    logger.debug("Start parsing object definitions");
    for (int i = 0; i < objectPropertyFiles.size(); i++) {

      logger.debug("File nr. " + i);
      parseFile(objectPropertyFiles.get(i));
    }
    return true;
  }

  public static String xmlToString(Node node) {
    try {
      Source source = new DOMSource(node);
      StringWriter stringWriter = new StringWriter();
      Result result = new StreamResult(stringWriter);
      TransformerFactory factory = TransformerFactory.newInstance();
      Transformer transformer = factory.newTransformer();
      transformer.transform(source, result);
      return stringWriter.getBuffer().toString();
    } catch (TransformerConfigurationException e) {
      e.printStackTrace();
    } catch (TransformerException e) {
      e.printStackTrace();
    }
    return null;
  }

  private void parseFile(File file) throws ObjectDefinitionException {

    try {

      DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      builderFactory.setNamespaceAware(true);
      builderFactory.setXIncludeAware(true);
      builderFactory.setIgnoringElementContentWhitespace(true);

      DocumentBuilder builder = builderFactory.newDocumentBuilder();
      org.w3c.dom.Document document = builder.parse(file);

      //logger.info(ObjectDefinitionParser.xmlToString(document));

      // JDOM does not understand x:include but has a more comfortable API.
      org.jdom2.Document jdocument = convertToJDOM(document);

      definitions.addDefinition(parseObjectDefinition(jdocument));

      logger.debug("Start logging");
      //logger.info(definitions.toString());

    } catch (IOException e) {
      logger.error("Cannot handle file: " + file.getAbsolutePath());
      throw new ObjectDefinitionException(e);
    } catch (ParserConfigurationException e) {
      StringWriter writer = new StringWriter();
      e.printStackTrace(new PrintWriter(writer));
      logger.error(writer.toString());
    } catch (SAXException e) {
      StringWriter writer = new StringWriter();
      e.printStackTrace(new PrintWriter(writer));
      logger.error(writer.toString());
    } catch (ClassCastException e) {
      StringWriter writer = new StringWriter();
      e.printStackTrace(new PrintWriter(writer));
      logger.error(writer.toString());
    } catch (Exception e) {
      StringWriter writer = new StringWriter();
      e.printStackTrace(new PrintWriter(writer));
      logger.error(writer.toString());
    }
  }

  private org.jdom2.Document convertToJDOM(org.w3c.dom.Document document) {

    org.jdom2.input.DOMBuilder builder = new org.jdom2.input.DOMBuilder();
    return builder.build(document);
  }

  private ObjectDefinition parseObjectDefinition(org.jdom2.Document jdocument) throws ObjectDefinitionException {

    ObjectDefinition definition;

    org.jdom2.Element root = jdocument.getRootElement();

    if (!root.getName().equals("ObjectDefinition")) {
      throw new ObjectDefinitionException("Cannot find root element 'ObjectDefinition'");
    }

    if (root.getAttributeValue("indexType") != null) {
      definition = new ObjectDefinition(root.getAttributeValue("type"), root.getAttributeValue("indexType"));
    } else {
      definition = new ObjectDefinition(root.getAttributeValue("type"));
    }

    // parse documents
    for (Object element : root.getChildren()) {

      definition.addDocumentDefinition(parseDocument((Element) element));
    }

    return definition;
  }

  private DocumentDefinition parseDocument(Element element) throws ObjectDefinitionException {

    if (!element.getName().equals("Document")) {
      throw new ObjectDefinitionException("Cannot find element 'Document'");
    }

    DocumentDefinition definition = new DocumentDefinition(element.getAttributeValue("type"));

    // Workaround xi:include of metaData)
    for (Object dataSources : element.getChildren("DataSources")) {

      logger.info("Adding DataSources...");
      for (Object source : ((Element) dataSources).getChildren("DataSource")) {

        definition.addDataSource(parseDataSource((Element) source));
      }

    }

    for (Object source : element.getChildren("DataSource")) {

      definition.addDataSource(parseDataSource((Element) source));
    }
    return definition;
  }

  private DataSource parseDataSource(Element source) throws ObjectDefinitionException {

    DataSource ds;

    if (!source.getName().equals("DataSource")) {
      throw new ObjectDefinitionException("Cannot find element 'DataSource'");
    }

    if (source.getAttributeValue("type").equalsIgnoreCase("JDBC")) {

      ds = new JDBCDataSource(DataSource.TYPE_JDBC);
      ds.setAction(source.getAttributeValue("action"));
      ((JDBCDataSource) ds).setQuery(source.getChildText("Query").trim());

      // Set parameters
      for (Object param : source.getChildren("Param")) {

        ParameterDefinition parameter = new ParameterDefinition(((Element) param).getAttributeValue("format"),
            ((Element) param).getAttributeValue("type"), ((Element) param).getAttributeValue("value"));
        ((JDBCDataSource) ds).addParameter(parameter);
      }

    } else if (source.getAttributeValue("type").equalsIgnoreCase("File")) {

      ds = new FileDataSource(DataSource.TYPE_FILE);
      ds.setAction(source.getAttributeValue("action"));

      Element pathCreator = source.getChild("PathCreator");
      if (pathCreator != null) {
        ((FileDataSource) ds).setPathCreator(PathCreatorFactory.factory(pathCreator.getAttributeValue("name")));
      }
    } else if (source.getAttributeValue("type").equalsIgnoreCase("Directory")) {

      ds = new DirectoryDataSource(DataSource.TYPE_DIRECTORY);
      ds.setAction(source.getAttributeValue("action"));

      Element pathCreator = source.getChild("PathCreator");
      if (pathCreator != null) {
        ((DirectoryDataSource) ds).setPathCreator(PathCreatorFactory.factory(pathCreator.getAttributeValue("name")));
      }
    } else {
      throw new ObjectDefinitionException(
          "Invalid type for element 'DataSource' type=" + source.getAttributeValue("type"));
    }

    // Now add nested data source element
    for (Object nestedDS : source.getChildren("DataSource")) {

      // Recursion
      ds.addDataSource(parseDataSource((Element) nestedDS));
    }

    // workaround for nested xi:include (e.g meta data)
    for (Object dataSources : source.getChildren("DataSources")) {

      logger.info("Adding nested dataSources...");
      for (Object xiDS : ((Element) dataSources).getChildren("DataSource")) {

        ds.addDataSource(parseDataSource((Element) xiDS));
      }

    }

    // Add fields
    for (Object field : source.getChildren("Field")) {

      FieldDefinition fieldDef = new FieldDefinition(((Element) field).getAttributeValue("store"),
          ((Element) field).getAttributeValue("index"), ((Element) field).getAttributeValue("name"),
          ((Element) field).getAttributeValue("column"), ((Element) field).getAttributeValue("type"),
          ((Element) field).getAttributeValue("global"), ((Element) field).getAttributeValue("dynamicName"));
      /*
       * Currentliy diabled.
      if(fieldDef.getIndex() != Field.Index.NO) {
        fieldInfo.addField(fieldDef.getName());
      }
      */

      // Add transformers to field definitions
      for (Object transformer : ((Element) field).getChildren("Transformer")) {

        TransformerDefinition transDef = new TransformerDefinition(((Element) transformer).getAttributeValue("name"));

        fieldDef.addTransformer(transDef);
      }
      ds.addField(fieldDef);
    }
    logger.debug(ds);
    return ds;
  }
}
