package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kgrid.shelf.ShelfException;

import java.util.List;

public interface CompoundDigitalObjectStore {

  List<String> getChildren(String... relativeLocationParts);

  String getAbsoluteLocation(String... relativeLocationParts);

  ObjectNode getMetadata(String... relativeLocationParts);

  byte[] getBinary(String... relativeLocationParts);

  void createContainer(String... relativeLocationParts);

  void saveMetadata(JsonNode metadata, String... relativeLocationParts);

  void saveBinary(byte[] data, String... relativeLocationParts);

  void delete(String... relativeLocationParts) throws ShelfException;

  String createTransaction();

  void commitTransaction(String transactionID);

  void rollbackTransaction(String transactionID);
}
