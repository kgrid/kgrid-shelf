package edu.umich.lhs.activator.controller;


import edu.umich.lhs.activator.domain.ArkId;
import edu.umich.lhs.activator.domain.KnowledgeObject;
import edu.umich.lhs.activator.repository.KnowledgeObjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShelfController {

  @Autowired
  KnowledgeObjectRepository shelf;

  @GetMapping("/ark:/{naan}/{name}/{version}")
  public KnowledgeObject getKnowledgeObject(@PathVariable String naan, @PathVariable String name, @PathVariable String version) {
    ArkId arkId = new ArkId(naan, name);

    return shelf.getCompoundKnowledgeObject(arkId, version);
  }



}
