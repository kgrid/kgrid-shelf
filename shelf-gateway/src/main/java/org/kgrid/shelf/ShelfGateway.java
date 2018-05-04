package org.kgrid.shelf;

import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootApplication
public class ShelfGateway implements ApplicationContextAware {

  ApplicationContext ctx;

  public static void main(String[] args) {
    SpringApplication.run(ShelfGateway.class, args);

  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.ctx = applicationContext;
  }

  // Set the URL suffix pattern matching to not cut off final periods and anything after them
  @Configuration
  public class AppConfig extends WebMvcConfigurationSupport {

    @Override
    @Bean
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
      RequestMappingHandlerMapping mapping = super.requestMappingHandlerMapping();
      mapping.setUseSuffixPatternMatch(false);
      return mapping;
    }

  }
}
