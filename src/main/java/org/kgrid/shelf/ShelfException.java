package org.kgrid.shelf;

public class ShelfException extends RuntimeException  {

  public ShelfException() {
    super();
  }

  public ShelfException(String message) {
    super(message);
  }

  public ShelfException(String message, Throwable cause) {
    super(message, cause);
  }

  public ShelfException(Throwable cause) {
    super(cause);
  }
}
