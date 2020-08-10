package org.kgrid.shelf.repository;

import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.net.URI;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ImportServiceTest {

    @Spy
    ApplicationContext applicationContext;
    @Mock
    CompoundDigitalObjectStore cdoStore;

    @InjectMocks
    ImportService importService;

    //    @Test
    public void importZip_GetsUriFromAppContext() throws IOException {
        applicationContext = new ClassPathXmlApplicationContext();
        URI resourceUri = URI.create("classpath:/fixtures/import-export/kozip.zip");
        String shelfLocation = "shelfLocation";
        when(cdoStore.getAbsoluteLocation()).thenReturn(shelfLocation);

        importService.importZip(resourceUri);

        verify(applicationContext).getResource(resourceUri.toString());
    }
}