package org.kgrid.shelf.repository;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class CompoundDigitalObjectStoreFactory {

  private static ApplicationContext applicationContext;

  @Autowired
  public CompoundDigitalObjectStoreFactory(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  public static CompoundDigitalObjectStore create(String cdoStoreURI) {
    String shelfClass = cdoStoreURI.substring(0, cdoStoreURI.indexOf(':'));
    CompoundDigitalObjectStore cdoStore;
    try {
      cdoStore = BeanFactoryAnnotationUtils
          .qualifiedBeanOfType(applicationContext.getAutowireCapableBeanFactory(),
              CompoundDigitalObjectStore.class, shelfClass);
    } catch (NoSuchBeanDefinitionException nsbdEx) {
      throw new IllegalStateException(
          "Cannot find specified CDO store implementation " + shelfClass + " " + nsbdEx);
    }
    return cdoStore;
  }
}
