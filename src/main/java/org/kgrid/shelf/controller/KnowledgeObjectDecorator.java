package org.kgrid.shelf.controller;

import org.kgrid.shelf.domain.KnowledgeObjectFields;

import javax.servlet.http.HttpServletRequest;

public interface KnowledgeObjectDecorator {

  void decorate(KnowledgeObjectFields ko, HttpServletRequest request);
}
