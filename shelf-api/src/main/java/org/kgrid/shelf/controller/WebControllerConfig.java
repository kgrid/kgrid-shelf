package org.kgrid.shelf.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
public class WebControllerConfig extends WebMvcConfigurationSupport {

  @Override
  @Bean
  public RequestMappingHandlerMapping requestMappingHandlerMapping() {
    RequestMappingHandlerMapping mapping = super.requestMappingHandlerMapping();
    mapping.setUseSuffixPatternMatch(false);
    return mapping;
  }

}
