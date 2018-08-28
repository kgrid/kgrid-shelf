package org.kgrid.shelf.controller;


import javax.servlet.http.HttpServletRequest;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.springframework.http.RequestEntity;

public interface KnowledgeObjectDecorator {

  void decorate(KnowledgeObject ko, HttpServletRequest request);

}
