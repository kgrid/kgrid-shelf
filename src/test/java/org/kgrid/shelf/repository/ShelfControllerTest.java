package org.kgrid.shelf.repository;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource( properties = "kgrid.shelf.cdostore.url=filesystem:file://target/shelf")
public class ShelfControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .build();
    }

    // MockMVC perform doesn't seem to handle put with file upload right now so we need to add this method
    private static MockMultipartHttpServletRequestBuilder putWithFileUpload(String urlTemplate) {
        MockMultipartHttpServletRequestBuilder builder =
            MockMvcRequestBuilders.fileUpload(urlTemplate);

        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });

        return builder;
    }

    @Test
    public void addZippedKO() throws Exception {

        MockMultipartFile file = new MockMultipartFile("ko", "hello-world-jsonld.zip",
            "application/zip", new ClassPathResource("/hello-world-jsonld.zip").getInputStream());

        mockMvc.perform(putWithFileUpload("/hello/world").file(file))
            .andExpect(status().isCreated());

    }

    @Test
    public void findAllMetadata() throws Exception {
        this.mockMvc.perform(get("/hello/world"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"));
    }

    @Test
    public void findImplementationMetadata() throws Exception {
        this.mockMvc.perform(get("/hello/world/v0.0.1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"));
    }

    @Test
    public void allMetadataNotFound() throws Exception {
        this.mockMvc.perform(get("/99999/ssssss"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void implementationMetadataNotFound() throws Exception {
        this.mockMvc.perform(get("/99999/fk45m6gq9t/XXXXX"))
            .andExpect(status().isNotFound());
    }

}