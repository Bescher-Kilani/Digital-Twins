package org.DigiTwinStudio.DigiTwin_Backend.services;

import org.DigiTwinStudio.DigiTwin_Backend.adapter.AAS4jAdapter;
import org.DigiTwinStudio.DigiTwin_Backend.domain.*;
import org.DigiTwinStudio.DigiTwin_Backend.dtos.AASModelDto;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ExportException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.ForbiddenException;
import org.DigiTwinStudio.DigiTwin_Backend.exceptions.NotFoundException;
import org.DigiTwinStudio.DigiTwin_Backend.mapper.AASModelMapper;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.AASModelRepository;
import org.DigiTwinStudio.DigiTwin_Backend.repositories.MarketPlaceEntryRepository;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.aasx.InMemoryFile;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ExportServiceTest {

    // Dependencies of ExportService are mocked
    @Mock private AASModelMapper aasModelMapper;
    @Mock private AAS4jAdapter aas4jAdapter;
    @Mock private FileStorageService fileStorageService;
    @Mock private AASModelRepository aasModelRepository;
    @Mock private MarketPlaceEntryRepository marketPlaceEntryRepository;

    // InjectMocks automatically injects the above mocks into the service
    @InjectMocks
    private ExportService service;

    // ---------- helper method ----------
    // Creates a simple AASModel instance for testing
    private static AASModel model(String id, String owner) {
        return AASModel.builder()
                .id(id)
                .ownerId(owner)
                .aas(new DefaultAssetAdministrationShell.Builder().idShort("TestAAS").build())
                .build();
    }

    // ---------- exportAsJson tests ----------

    @Test
    void exportAsJson_returnsUtf8Bytes_onSuccess() throws Exception {
        // Given: mock environment and JSON serialization
        AASModel m = model("m1", "u1");
        DefaultEnvironment env = new DefaultEnvironment();
        when(aas4jAdapter.aasModelToDefaultEnvironment(m)).thenReturn(env);
        when(aas4jAdapter.serializeToJsonString(env)).thenReturn("{\"ok\":true}");

        // When
        byte[] out = service.exportAsJson(m);

        // Then: check returned bytes and verify calls
        assertArrayEquals("{\"ok\":true}".getBytes(StandardCharsets.UTF_8), out);
        verify(aas4jAdapter).aasModelToDefaultEnvironment(m);
        verify(aas4jAdapter).serializeToJsonString(env);
    }

    @Test
    void exportAsJson_wrapsSerializationException() throws Exception {
        // Given: serialization throws an exception
        AASModel m = model("m1", "u1");
        DefaultEnvironment env = new DefaultEnvironment();
        when(aas4jAdapter.aasModelToDefaultEnvironment(m)).thenReturn(env);
        when(aas4jAdapter.serializeToJsonString(env)).thenThrow(new SerializationException("boom"));

        // Expect: ExportException is thrown
        assertThrows(ExportException.class, () -> service.exportAsJson(m));
    }

    // ---------- exportAsAasx tests ----------

    @Test
    void exportAsAasx_serializesEnvironmentAndFiles() throws Exception {
        // Given: model, environment, and in-memory files
        AASModel m = model("m1", "u1");
        DefaultEnvironment env = new DefaultEnvironment();
        List<InMemoryFile> files = List.of(new InMemoryFile(new byte[]{1,2,3}, "/path/file1.bin"));

        when(aas4jAdapter.aasModelToDefaultEnvironment(m)).thenReturn(env);
        when(fileStorageService.getInMemoryFilesByModelId("m1")).thenReturn(files);

        // Stub the serializer to write test bytes into the output stream
        Mockito.doAnswer(inv -> {
            ByteArrayOutputStream baos = inv.getArgument(2);
            baos.write(new byte[]{9,9,9});
            return null; // void method
        }).when(aas4jAdapter).serializeToAASX(eq(env), eq(files), any());

        // When
        byte[] out = service.exportAsAasx(m);

        // Then
        assertArrayEquals(new byte[]{9,9,9}, out);
        verify(aas4jAdapter).aasModelToDefaultEnvironment(m);
        verify(fileStorageService).getInMemoryFilesByModelId("m1");
        verify(aas4jAdapter).serializeToAASX(eq(env), eq(files), any());
    }

    @Test
    void exportAsAasx_wrapsSerializationException() throws Exception {
        // Given: serializer throws exception
        AASModel m = model("m1", "u1");
        DefaultEnvironment env = new DefaultEnvironment();
        when(aas4jAdapter.aasModelToDefaultEnvironment(m)).thenReturn(env);
        when(fileStorageService.getInMemoryFilesByModelId("m1")).thenReturn(List.of());

        doThrow(new SerializationException("fail"))
                .when(aas4jAdapter).serializeToAASX(eq(env), any(), any());

        // Expect: ExportException
        assertThrows(ExportException.class, () -> service.exportAsAasx(m));
    }

    // ---------- exportStoredModel tests ----------

    @Test
    void exportStoredModel_throwsNotFound_whenModelMissing() {
        // Given: repository returns empty
        when(aasModelRepository.findById("missing")).thenReturn(Optional.empty());

        // Expect: NotFoundException
        assertThrows(NotFoundException.class,
                () -> service.exportStoredModel("missing", ExportFormat.JSON, "u1"));
    }

    @Test
    void exportStoredModel_throwsForbidden_whenOwnerMismatch() {
        // Given: model with different owner
        AASModel m = model("m1", "other");
        when(aasModelRepository.findById("m1")).thenReturn(Optional.of(m));

        // Expect: ForbiddenException
        assertThrows(ForbiddenException.class,
                () -> service.exportStoredModel("m1", ExportFormat.JSON, "u1"));
    }

    @Test
    void exportStoredModel_delegatesToJson() throws SerializationException {
        // Given: model belongs to the user
        AASModel m = model("m1", "u1");
        when(aasModelRepository.findById("m1")).thenReturn(Optional.of(m));
        when(aas4jAdapter.aasModelToDefaultEnvironment(m)).thenReturn(new DefaultEnvironment());
        when(aas4jAdapter.serializeToJsonString(any())).thenReturn("{ }");

        // When
        byte[] out = service.exportStoredModel("m1", ExportFormat.JSON, "u1");

        // Then
        assertNotNull(out);
        verify(aasModelRepository).findById("m1");
        verify(aas4jAdapter).serializeToJsonString(any());
    }

    @Test
    void exportStoredModel_delegatesToAasx() throws Exception {
        // Given: AASX format requested
        AASModel m = model("m1", "u1");
        when(aasModelRepository.findById("m1")).thenReturn(Optional.of(m));
        when(aas4jAdapter.aasModelToDefaultEnvironment(m)).thenReturn(new DefaultEnvironment());
        when(fileStorageService.getInMemoryFilesByModelId("m1")).thenReturn(List.of());
        doAnswer(inv -> {
            ByteArrayOutputStream baos = inv.getArgument(2);
            baos.write(1);
            return null;
        }).when(aas4jAdapter).serializeToAASX(any(), any(), any());

        // When
        byte[] out = service.exportStoredModel("m1", ExportFormat.AASX, "u1");

        // Then
        assertArrayEquals(new byte[]{1}, out);
    }

    // ---------- export() tests ----------

    @Test
    void export_buildsFilenameAndContentType_json() throws SerializationException {
        // Given: JSON export
        AASModel m = model("m1", "u1");
        when(aasModelRepository.findById("m1")).thenReturn(Optional.of(m));
        when(aas4jAdapter.aasModelToDefaultEnvironment(m)).thenReturn(new DefaultEnvironment());
        when(aas4jAdapter.serializeToJsonString(any())).thenReturn("{}");

        // When
        ExportedFile file = service.export("m1", "MyExport", ExportFormat.JSON, "u1");

        // Then: filename and content type should match format
        assertEquals("MyExport.json", file.filename());
        assertEquals("application/json", file.contentType());
        assertArrayEquals("{}".getBytes(StandardCharsets.UTF_8), file.bytes());
    }

    @Test
    void export_buildsFilenameAndContentType_aasx() throws Exception {
        // Given: AASX export
        AASModel m = model("m1", "u1");
        when(aasModelRepository.findById("m1")).thenReturn(Optional.of(m));
        when(aas4jAdapter.aasModelToDefaultEnvironment(m)).thenReturn(new DefaultEnvironment());
        when(fileStorageService.getInMemoryFilesByModelId("m1")).thenReturn(List.of());
        doAnswer(inv -> { ((ByteArrayOutputStream)inv.getArgument(2)).write(7); return null; })
                .when(aas4jAdapter).serializeToAASX(any(), any(), any());

        // When
        ExportedFile file = service.export("m1", "MyExport", ExportFormat.AASX, "u1");

        // Then
        assertEquals("MyExport.aasx", file.filename());
        assertEquals("application/asset-administration-shell-package", file.contentType());
        assertArrayEquals(new byte[]{7}, file.bytes());
    }

    // ---------- exportTransientModel tests ----------

    @Test
    void exportTransientModel_json_usesMapperAndDelegates() throws SerializationException {
        // Given: transient JSON export for guest
        AASModelDto dto = new AASModelDto();
        AASModel m = model("tmp", "GUEST");
        when(aasModelMapper.fromDto(dto, "GUEST")).thenReturn(m);
        when(aas4jAdapter.aasModelToDefaultEnvironment(m)).thenReturn(new DefaultEnvironment());
        when(aas4jAdapter.serializeToJsonString(any())).thenReturn("{\"guest\":true}");

        // When
        byte[] out = service.exportTransientModel(dto, ExportFormat.JSON);

        // Then
        assertArrayEquals("{\"guest\":true}".getBytes(StandardCharsets.UTF_8), out);
        verify(aasModelMapper).fromDto(dto, "GUEST");
    }

    @Test
    void exportTransientModel_aasx_usesMapperAndDelegates() throws Exception {
        // Given: transient AASX export for guest
        AASModelDto dto = new AASModelDto();
        AASModel m = model("tmp", "GUEST");
        when(aasModelMapper.fromDto(dto, "GUEST")).thenReturn(m);
        when(aas4jAdapter.aasModelToDefaultEnvironment(m)).thenReturn(new DefaultEnvironment());
        when(fileStorageService.getInMemoryFilesByModelId("tmp")).thenReturn(List.of());
        doAnswer(inv -> { ((ByteArrayOutputStream)inv.getArgument(2)).write(3); return null; })
                .when(aas4jAdapter).serializeToAASX(any(), any(), any());

        // When
        byte[] out = service.exportTransientModel(dto, ExportFormat.AASX);

        // Then
        assertArrayEquals(new byte[]{3}, out);
        verify(aasModelMapper).fromDto(dto, "GUEST");
    }

    // ---------- exportMarketplaceModel tests ----------

    @Test
    void exportMarketplaceModel_json_happy() throws SerializationException {
        // Given: marketplace entry references an existing model
        MarketplaceEntry entry = MarketplaceEntry.builder().id("m1").build();
        AASModel m = model("m1", "owner");
        when(marketPlaceEntryRepository.findById("e1")).thenReturn(Optional.of(entry));
        when(aasModelRepository.findById("m1")).thenReturn(Optional.of(m));
        when(aas4jAdapter.aasModelToDefaultEnvironment(m)).thenReturn(new DefaultEnvironment());
        when(aas4jAdapter.serializeToJsonString(any())).thenReturn("{}");

        // When
        byte[] out = service.exportMarketplaceModel("e1", ExportFormat.JSON);

        // Then
        assertArrayEquals("{}".getBytes(StandardCharsets.UTF_8), out);
    }

    @Test
    void exportMarketplaceModel_aasx_happy() throws Exception {
        // Given: marketplace entry references a model, AASX export
        MarketplaceEntry entry = MarketplaceEntry.builder().id("m1").build();
        AASModel m = model("m1", "owner");
        when(marketPlaceEntryRepository.findById("e1")).thenReturn(Optional.of(entry));
        when(aasModelRepository.findById("m1")).thenReturn(Optional.of(m));
        when(aas4jAdapter.aasModelToDefaultEnvironment(m)).thenReturn(new DefaultEnvironment());
        when(fileStorageService.getInMemoryFilesByModelId("m1")).thenReturn(List.of());
        doAnswer(inv -> { ((ByteArrayOutputStream)inv.getArgument(2)).write(5); return null; })
                .when(aas4jAdapter).serializeToAASX(any(), any(), any());

        // When
        byte[] out = service.exportMarketplaceModel("e1", ExportFormat.AASX);

        // Then
        assertArrayEquals(new byte[]{5}, out);
    }

    @Test
    void exportMarketplaceModel_throwsWhenEntryMissing() {
        // Given: entry not found
        when(marketPlaceEntryRepository.findById("e1")).thenReturn(Optional.empty());

        // Expect: NotFoundException
        assertThrows(NotFoundException.class,
                () -> service.exportMarketplaceModel("e1", ExportFormat.JSON));
    }

    @Test
    void exportMarketplaceModel_throwsWhenModelMissing() {
        // Given: entry found, but model not found
        MarketplaceEntry entry = MarketplaceEntry.builder().id("m1").build();
        when(marketPlaceEntryRepository.findById("e1")).thenReturn(Optional.of(entry));
        when(aasModelRepository.findById("m1")).thenReturn(Optional.empty());

        // Expect: NotFoundException
        assertThrows(NotFoundException.class,
                () -> service.exportMarketplaceModel("e1", ExportFormat.AASX));
    }
}
