package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kgrid.shelf.ShelfException;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

public interface CompoundDigitalObjectStore {

  List<URI> getChildren();

  URI getAbsoluteLocation(URI relativeLocation);

  ObjectNode getMetadata(URI relativeLocation);

  byte[] getBinary(URI relativeLocation);

  InputStream getBinaryStream(URI relativeLocation);

  void createContainer(URI relativeLocation);

  void saveMetadata(JsonNode metadata, URI relativeLocation);

  @Deprecated
  void saveBinary(byte[] data, URI relativeLocation);

  void saveBinary(InputStream stream, URI relativeLocation);

  void delete(URI relativeLocation) throws ShelfException;

  String createTransaction();

  void commitTransaction(String transactionID);

  void rollbackTransaction(String transactionID);

  long getBinarySize(URI relativeLocation);
}
