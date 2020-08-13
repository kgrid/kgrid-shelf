package org.kgrid.shelf.repository;

public class ImportExportException extends RuntimeException {

  public ImportExportException(String s, Exception e) {
    super(s,e);
  }
}
