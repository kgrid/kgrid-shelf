package kgrid.org.shelf.controller;

import kgrid.org.shelf.repository.KnowledgeObjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ShelfHealthIndicator implements HealthIndicator {

    ShelfHealthIndicator(KnowledgeObjectRepository shelf) {
        this.shelf = shelf;
    }

    final KnowledgeObjectRepository shelf;

    @Override
    public Health health() {
        try {
            shelf.findAll();
            return Health.up().withDetail("shelf.connection", shelf.getConnection()).build();
        } catch (Exception ex) {
            return Health.down().withException(ex).build();
        }
    }
}
