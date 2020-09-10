package org.kgrid.shelf;

public class ShelfResourceNotFound extends ShelfException {

  public ShelfResourceNotFound() {
    super();
  }

  public ShelfResourceNotFound(String message) {
    super(message);
  }

  public ShelfResourceNotFound(String message, Throwable cause) {
    super(message, cause);
  }

  public ShelfResourceNotFound(Throwable cause) {
    super(cause);
  }
}
