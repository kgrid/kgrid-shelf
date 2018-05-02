package kgrid.org.shelf.repository;

import javax.annotation.Resource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class CompoundDigitalObjectStoreFactory {

  @Resource
  private CompoundDigitalObjectStore cdoStore;

  private ApplicationContext applicationContext;

  @Value("${shelf.type:filesystemCDOStore}")
  private String shelfClass;

  @Autowired
  public CompoundDigitalObjectStoreFactory(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  public CompoundDigitalObjectStore create() {
    try {
      cdoStore = (CompoundDigitalObjectStore)applicationContext.getBean(shelfClass);
      System.out.println(cdoStore);
    } catch (NoSuchBeanDefinitionException nsbdEx) {
      throw new IllegalStateException("Cannot find specified CDO store implementation " + shelfClass + " " + nsbdEx);
    }
    return cdoStore;
  }

  void setShelfClass(String shelfClass) {
    this.shelfClass = shelfClass;
  }
}
