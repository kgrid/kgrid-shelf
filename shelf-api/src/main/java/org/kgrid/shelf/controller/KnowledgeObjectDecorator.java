package org.kgrid.shelf.controller;


import org.kgrid.shelf.domain.KnowledgeObject;
import org.springframework.http.RequestEntity;

public interface KnowledgeObjectDecorator {

  public void decorate(KnowledgeObject ko, RequestEntity request);

}
