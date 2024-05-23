package de.ilias.services.object;

import de.ilias.services.lucene.index.file.path.PathCreator;

public abstract class FSDataSource extends DataSource {
  private PathCreator pathCreator = null;

  public FSDataSource(TYPE type) {
    super(type);
  }

  /**
   * @param pathCreator the pathCreator to set
   */
  public void setPathCreator(PathCreator pathCreator) {
    this.pathCreator = pathCreator;
  }

  public PathCreator getPathCreator() {
    return pathCreator;
  }
}
