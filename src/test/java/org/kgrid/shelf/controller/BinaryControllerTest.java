package org.kgrid.shelf.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.ShelfResourceForbidden;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.Assert.assertThrows;
import static org.kgrid.shelf.TestHelper.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BinaryControllerTest {

  private KnowledgeObjectRepository koRepo;
  private BinaryController binaryController;
  private MockHttpServletRequest mockServletRequest;
  private final String childPath = "childPath";

  @Before
  public void setup() {
    koRepo = Mockito.mock(KnowledgeObjectRepository.class);
    binaryController = new BinaryController(koRepo, null);
    mockServletRequest = new MockHttpServletRequest();
    String requestUri = NAAN + "/" + NAME + "/" + VERSION + "/" + childPath;
    mockServletRequest.setRequestURI(requestUri);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockServletRequest));
    when(koRepo.getBinary(ARK_ID, childPath)).thenReturn("byteArray".getBytes());
  }

  @Test
  public void getBinary_CallsGetBinaryOnKoRepo() {
    binaryController.getBinary(NAAN, NAME, VERSION, mockServletRequest);
    verify(koRepo).getBinary(ARK_ID, childPath);
  }

  @Test
  public void getBinary_ThrowsErrorWhenTryingToEscapeKO() {
    String badChildpath = "../ko2/metadata.json";
    mockServletRequest = new MockHttpServletRequest();
    String requestUri = NAAN + "/" + NAME + "/" + VERSION + "/" + badChildpath;
    mockServletRequest.setRequestURI(requestUri);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockServletRequest));

    assertThrows(
        ShelfResourceForbidden.class,
        () -> binaryController.getBinary(NAAN, NAME, VERSION, mockServletRequest));
  }

  @Test
  public void getBinary_hasJsonContentTypeForJsonFileExt() {

    String jsonChildpath = "metadata.json";
    mockServletRequest = new MockHttpServletRequest();
    String requestUri = NAAN + "/" + NAME + "/" + VERSION + "/" + jsonChildpath;
    mockServletRequest.setRequestURI(requestUri);
    ResponseEntity<Object> jsonResp =
        binaryController.getBinary(NAAN, NAME, VERSION, mockServletRequest);
    Assert.assertEquals(
        "Returns a json header for a path ending in '.json'",
        jsonResp.getHeaders().get("Content-Type").get(0),
        MediaType.APPLICATION_JSON_VALUE);
  }

  @Test
  public void getBinary_hasYamlContentTypeForYamlFileExt() {

    String jsonChildpath = "deployment.yaml";
    mockServletRequest = new MockHttpServletRequest();
    String requestUri = NAAN + "/" + NAME + "/" + VERSION + "/" + jsonChildpath;
    mockServletRequest.setRequestURI(requestUri);
    ResponseEntity<Object> yamlResp =
        binaryController.getBinary(NAAN, NAME, VERSION, mockServletRequest);
    Assert.assertEquals(
        "Returns a yaml header for a path ending in '.yaml'",
        yamlResp.getHeaders().get("Content-Type").get(0),
        "application/yaml");
  }

  @Test
  public void getBinary_hasOctetContentTypeForUnknownFileExt() {
    String pdfChildpath = "file.pdf";
    mockServletRequest = new MockHttpServletRequest();
    String requestUri = NAAN + "/" + NAME + "/" + VERSION + "/" + pdfChildpath;
    mockServletRequest.setRequestURI(requestUri);
    ResponseEntity<Object> yamlResp =
        binaryController.getBinary(NAAN, NAME, VERSION, mockServletRequest);
    Assert.assertEquals(
        "Returns an octet header for an unknown filetype",
        yamlResp.getHeaders().get("Content-Type").get(0),
        MediaType.APPLICATION_OCTET_STREAM_VALUE);
  }
}
