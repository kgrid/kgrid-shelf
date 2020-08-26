package org.kgrid.shelf.service;

public class ImportExportException extends RuntimeException {

  public ImportExportException(String s, Exception e) {
    super(s,e);
  }
}
