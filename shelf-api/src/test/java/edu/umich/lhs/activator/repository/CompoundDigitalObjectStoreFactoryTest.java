package edu.umich.lhs.activator.repository;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

public class CompoundDigitalObjectStoreFactoryTest {

  CompoundDigitalObjectStoreFactory factory;

  @Test
  public void checkObjectStoreCreation() {
//    assertTrue(factory.create("ark:/99999/test").getClass().isInstance(FilesystemCDOStore.class));
  }

}