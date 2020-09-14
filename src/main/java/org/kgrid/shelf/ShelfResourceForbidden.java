package org.kgrid.shelf;

public class ShelfResourceForbidden extends ShelfException {

  public ShelfResourceForbidden() {
    super();
  }

  public ShelfResourceForbidden(String message) {
    super(message);
  }

  public ShelfResourceForbidden(String message, Throwable cause) {
    super(message, cause);
  }

  public ShelfResourceForbidden(Throwable cause) {
    super(cause);
  }
}
