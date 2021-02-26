package org.kgrid.shelf.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.activation.MimetypesFileTypeMap;

@Component
public class FileTypeMap {

    @Bean
    public static MimetypesFileTypeMap getFilemap() {
        MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();
        fileTypeMap.addMimeTypes(
                "application/yaml yaml YAML\n"
                        + "application/json json JSON\n"
                        + "text/javascript js JS\n"
                        + "application/pdf pdf PDF\n"
                        + "text/csv csv CSV\n"
                        + "application/zip zip ZIP");
        return fileTypeMap;
    }
}
