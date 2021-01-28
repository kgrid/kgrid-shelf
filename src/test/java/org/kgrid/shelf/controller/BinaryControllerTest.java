package org.kgrid.shelf.controller;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.kgrid.shelf.ShelfResourceForbidden;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.activation.MimetypesFileTypeMap;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.kgrid.shelf.TestHelper.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BinaryControllerTest {

  private KnowledgeObjectRepository koRepo;
  private BinaryController binaryController;
  private MockHttpServletRequest mockServletRequest;

  @BeforeEach
  public void setup() {
    koRepo = Mockito.mock(KnowledgeObjectRepository.class);
    MimetypesFileTypeMap fileTypeMap = BinaryController.getFilemap();
    binaryController = new BinaryController(koRepo, fileTypeMap);
    mockServletRequest = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockServletRequest));
  }

  private static Stream<Arguments> providePathsAndTypes() {
    return Stream.of(
        Arguments.of("metadata.json", MediaType.APPLICATION_JSON_VALUE),
        Arguments.of("service.yaml", "application/yaml"),
        Arguments.of("helloWorld.js", "text/javascript"),
        Arguments.of("paper.pdf", MediaType.APPLICATION_PDF_VALUE),
        Arguments.of("tpsReports.csv", "text/csv"),
        Arguments.of("ko.zip", "application/zip"),
        Arguments.of("file.xyz", MediaType.APPLICATION_OCTET_STREAM_VALUE));
  }

  @ParameterizedTest
  @MethodSource("providePathsAndTypes")
  @DisplayName("Get binary gets binary from repo")
  public void getBinary_CallsGetBinaryOnKoRepo(String childPath, String expectedMediaType) {
    String requestUri = NAAN + "/" + NAME + "/" + VERSION + "/" + childPath;
    mockServletRequest.setRequestURI(requestUri);
    when(koRepo.getBinaryStream(ARK_ID, childPath))
        .thenReturn(IOUtils.toInputStream("inputStream", Charset.defaultCharset()));

    ResponseEntity<Object> jsonResp =
        binaryController.getBinary(NAAN, NAME, VERSION, mockServletRequest);
    assertAll(
        () -> verify(koRepo).getBinaryStream(ARK_ID, childPath),
        () ->
            assertEquals(
                expectedMediaType,
                Objects.requireNonNull(jsonResp.getHeaders().getContentType()).toString()));
  }

  @Test
  @DisplayName("Throws forbidden when trying to go outside shelf")
  public void getBinary_ThrowsErrorWhenTryingToEscapeKO() {
    String badChildPath = "../ko2/metadata.json";
    mockServletRequest = new MockHttpServletRequest();
    String requestUri = NAAN + "/" + NAME + "/" + VERSION + "/" + badChildPath;
    mockServletRequest.setRequestURI(requestUri);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockServletRequest));

    assertThrows(
        ShelfResourceForbidden.class,
        () -> binaryController.getBinary(NAAN, NAME, VERSION, mockServletRequest));
  }
}
