package kgrid.org.shelf.controller;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Category(IntegrationTest.class)

public class ShelfControllerTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void addZippedKO()  {

        LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
        parameters.add("ko", new org.springframework.core.io.ClassPathResource("/99999-fk45m6gq9t.zip"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<LinkedMultiValueMap<String, Object>> entity =
                    new HttpEntity<LinkedMultiValueMap<String, Object>>(parameters, headers);

        ResponseEntity<String> response = testRestTemplate.exchange("/ark:/99999/fk45m6gq9t",
                HttpMethod.PUT, entity, String.class);

        // Expect Ok
        assertThat(response.getStatusCode(), is(HttpStatus.CREATED));

    }

    @Test
    public void findAllMetadata() {
        ResponseEntity<String> response  = this.testRestTemplate.getForEntity("/ark:/99999/fk45m6gq9t", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        jsonPath("$..metadata", hasSize(2));
    }

    @Test
    public void findVersionMetadata() {
        ResponseEntity<String> response  = this.testRestTemplate.getForEntity("/ark:/99999/fk45m6gq9t/default", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        jsonPath("$..metadata", hasSize(1));
    }

    @Test
    public void allMetadataNotFound() {
        ResponseEntity<String> response  = this.testRestTemplate.getForEntity("/ark:/99999/ssssss", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void versionMetadataNotFound() {
        ResponseEntity<String> response  = this.testRestTemplate.getForEntity("/ark:/99999/fk45m6gq9t/XXXXX", String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

}