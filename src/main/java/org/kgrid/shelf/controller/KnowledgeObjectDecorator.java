package org.kgrid.shelf.controller;

import org.kgrid.shelf.domain.KoFields;

import javax.servlet.http.HttpServletRequest;

public interface KnowledgeObjectDecorator {

  void decorate(KoFields ko, HttpServletRequest request);
}
