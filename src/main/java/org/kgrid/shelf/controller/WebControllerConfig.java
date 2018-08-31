package org.kgrid.shelf.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebControllerConfig implements WebMvcConfigurer {

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    // don't match trailing version digit as file suffix, 'v0.0.1' =/= 'v0.0' w/ suffix '1'
    configurer.setUseSuffixPatternMatch(false);
  }

  @Override
  public void configureContentNegotiation(final ContentNegotiationConfigurer configurer) {
    // adds handling for ?format=zip
    configurer
        .favorParameter(true)
        .mediaType("zip", MediaType.parseMediaType("application/zip"))
    ;
  }


}