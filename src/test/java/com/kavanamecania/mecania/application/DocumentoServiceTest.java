package com.kavanamecania.mecania.application;

import com.kavanamecania.mecania.application.evento.DocumentoSubidoEvent;
import com.kavanamecania.mecania.domain.model.Documento;
import com.kavanamecania.mecania.domain.model.Vehiculo;
import com.kavanamecania.mecania.domain.storage.AlmacenamientoArchivos;
import com.kavanamecania.mecania.infrastructure.repository.DocumentoRepository;
import com.kavanamecania.mecania.infrastructure.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private AlmacenamientoArchivos almacenamientoArchivos;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DocumentoService documentoService;

    private Vehiculo vehiculo;
    private MultipartFile validPdf;
    private MultipartFile validTxt;
    private MultipartFile invalidExt;
    private MultipartFile tooLarge;

    @BeforeEach
    void setUp() {
        vehiculo = Vehiculo.builder()
                .id(1L)
                .usuarioId(1L)
                .marca("Toyota")
                .modelo("Corolla")
                .anio(2020)
                .build();

        validPdf = new MockMultipartFile(
                "manual.pdf",
                "manual.pdf",
                "application/pdf",
                "%PDF-1.4 test content".getBytes()
        );

        validTxt = new MockMultipartFile(
                "manual.txt",
                "manual.txt",
                "text/plain",
                "This is a text manual".getBytes()
        );

        invalidExt = new MockMultipartFile(
                "manual.jpg",
                "manual.jpg",
                "image/jpeg",
                "fake image".getBytes()
        );

        // Create a file larger than 10 MB
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        tooLarge = new MockMultipartFile(
                "large.pdf",
                "large.pdf",
                "application/pdf",
                largeContent
        );
    }

    @Test
    void subirDocumento_validoPdf_guarda_y_devuelveDocumento() throws IOException {
        // Given
        when(vehiculoRepository.findById(1L)).thenReturn(java.util.Optional.of(vehiculo));
        doReturn("vehiculos/1/documentos/manual.pdf")
                .when(almacenamientoArchivos).guardarArchivo(validPdf, "vehiculos/1/documentos");
        Documento expected = Documento.builder()
                .id(10L)
                .vehiculo(vehiculo)
                .nombre("manual.pdf")
                .tipo(Documento.TipoDocumento.PDF)
                .rutaAlmacenamiento("vehiculos/1/documentos/manual.pdf")
                .estado(Documento.EstadoProcesamiento.PROCESANDO)
                .tamanoBytes(validPdf.getSize())
                .mensajeError(null)
                
                .build();
        when(documentoRepository.save(any(Documento.class))).thenReturn(expected);

        // When
        Documento result = documentoService.subirDocumento(1L, validPdf);

        // Then
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getNombre()).isEqualTo("manual.pdf");
        assertThat(result.getTipo()).isEqualTo(Documento.TipoDocumento.PDF);
        assertThat(result.getRutaAlmacenamiento()).isEqualTo("vehiculos/1/documentos/manual.pdf");
        assertThat(result.getEstado()).isEqualTo(Documento.EstadoProcesamiento.PROCESANDO);
        assertThat(result.getTamanoBytes()).isEqualTo(validPdf.getSize());
        assertThat(result.getMensajeError()).isNull();

        verify(vehiculoRepository).findById(1L);
        verify(almacenamientoArchivos, times(1)).guardarArchivo(validPdf, "vehiculos/1/documentos");
        verify(documentoRepository).save(any(Documento.class));
        // El disparo async se hace por evento AFTER_COMMIT (no llamada directa):
        // verificar que se publica con el id del documento guardado.
        ArgumentCaptor<DocumentoSubidoEvent> captor = ArgumentCaptor.forClass(DocumentoSubidoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().documentoId()).isEqualTo(10L);
    }

    @Test
    void subirDocumento_validoTxt_guarda_y_devuelveDocumento() throws IOException {
        // Given
        when(vehiculoRepository.findById(1L)).thenReturn(java.util.Optional.of(vehiculo));
        doReturn("vehiculos/1/documentos/manual.txt")
                .when(almacenamientoArchivos).guardarArchivo(validTxt, "vehiculos/1/documentos");
        Documento expected = Documento.builder()
                .id(20L)
                .vehiculo(vehiculo)
                .nombre("manual.txt")
                .tipo(Documento.TipoDocumento.TXT)
                .rutaAlmacenamiento("vehiculos/1/documentos/manual.txt")
                .estado(Documento.EstadoProcesamiento.PROCESANDO)
                .tamanoBytes(validTxt.getSize())
                .mensajeError(null)
                
                .build();
        when(documentoRepository.save(any(Documento.class))).thenReturn(expected);

        // When
        Documento result = documentoService.subirDocumento(1L, validTxt);

        // Then
        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getNombre()).isEqualTo("manual.txt");
        assertThat(result.getTipo()).isEqualTo(Documento.TipoDocumento.TXT);
        assertThat(result.getRutaAlmacenamiento()).isEqualTo("vehiculos/1/documentos/manual.txt");
        assertThat(result.getEstado()).isEqualTo(Documento.EstadoProcesamiento.PROCESANDO);
        assertThat(result.getTamanoBytes()).isEqualTo(validTxt.getSize());
        assertThat(result.getMensajeError()).isNull();

        verify(vehiculoRepository).findById(1L);
        verify(almacenamientoArchivos, times(1)).guardarArchivo(validTxt, "vehiculos/1/documentos");
        verify(documentoRepository).save(any(Documento.class));
        ArgumentCaptor<DocumentoSubidoEvent> captor = ArgumentCaptor.forClass(DocumentoSubidoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().documentoId()).isEqualTo(20L);
    }

    @Test
    void subirDocumento_extensionInvalida_lanzaIllegalArgumentException() {
        // Given
        when(vehiculoRepository.findById(1L)).thenReturn(java.util.Optional.of(vehiculo));

        // When/Then
        assertThatThrownBy(() -> documentoService.subirDocumento(1L, invalidExt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de archivo no soportado");

        verify(vehiculoRepository).findById(1L);
        verifyNoInteractions(almacenamientoArchivos);
        verifyNoInteractions(documentoRepository);
    }

    @Test
    void subirDocumento_archivoDemasiadoGrande_lanzaIllegalArgumentException() {
        // Given
        when(vehiculoRepository.findById(1L)).thenReturn(java.util.Optional.of(vehiculo));

        // When/Then
        assertThatThrownBy(() -> documentoService.subirDocumento(1L, tooLarge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Archivo demasiado grande");

        verify(vehiculoRepository).findById(1L);
        verifyNoInteractions(almacenamientoArchivos);
        verifyNoInteractions(documentoRepository);
    }

    @Test
    void subirDocumento_vehiculoNoExistente_lanzaIllegalArgumentException() {
        // Given
        when(vehiculoRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        // When/Then
        assertThatThrownBy(() -> documentoService.subirDocumento(999L, validPdf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vehiculo no encontrado");

        verify(vehiculoRepository).findById(999L);
        verifyNoInteractions(almacenamientoArchivos);
        verifyNoInteractions(documentoRepository);
    }

    @Test
    void findByVehiculoId_devuelveLista() {
        // Given
        Documento doc1 = Documento.builder()
                .id(1L)
                .vehiculo(vehiculo)
                .nombre("doc1.pdf")
                .tipo(Documento.TipoDocumento.PDF)
                .rutaAlmacenamiento("vehiculos/1/documentos/doc1.pdf")
                .estado(Documento.EstadoProcesamiento.LISTO)
                .tamanoBytes(100L)
                .mensajeError(null)
                
                .build();
        Documento doc2 = Documento.builder()
                .id(2L)
                .vehiculo(vehiculo)
                .nombre("doc2.txt")
                .tipo(Documento.TipoDocumento.TXT)
                .rutaAlmacenamiento("vehiculos/1/documentos/doc2.txt")
                .estado(Documento.EstadoProcesamiento.ERROR)
                .tamanoBytes(200L)
                .mensajeError("Failed to process")
                
                .build();
        when(documentoRepository.findByVehiculoId(1L)).thenReturn(List.of(doc1, doc2));

        // When
        List<Documento> result = documentoService.findByVehiculoId(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Documento::getNombre)
                .containsExactlyInAnyOrder("doc1.pdf", "doc2.txt");
        verify(documentoRepository).findByVehiculoId(1L);
    }

    @Test
    void findByIdAndVehiculoId_existente_devuelveOptional() {
        // Given
        Documento doc = Documento.builder()
                .id(5L)
                .vehiculo(vehiculo)
                .nombre("test.pdf")
                .tipo(Documento.TipoDocumento.PDF)
                .rutaAlmacenamiento("vehiculos/1/documentos/test.pdf")
                .estado(Documento.EstadoProcesamiento.LISTO)
                .tamanoBytes(300L)
                .mensajeError(null)
                
                .build();
        when(documentoRepository.findByIdAndVehiculoId(5L, 1L))
                .thenReturn(java.util.Optional.of(doc));

        // When
        Optional<Documento> result = documentoService.findByIdAndVehiculoId(5L, 1L);

        // Then
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getNombre()).isEqualTo("test.pdf");
        verify(documentoRepository).findByIdAndVehiculoId(5L, 1L);
    }

    @Test
    void findByIdAndVehiculoId_noPerteneceAlVehiculo_devuelveEmpty() {
        // Given
        Vehiculo otherVehiculo = Vehiculo.builder()
                .id(2L)
                .usuarioId(2L)
                .marca("Honda")
                .modelo("Civic")
                .anio(2021)
                .build();
        Documento doc = Documento.builder()
                .id(5L)
                .vehiculo(otherVehiculo)
                .nombre("test.pdf")
                .tipo(Documento.TipoDocumento.PDF)
                .rutaAlmacenamiento("vehiculos/2/documentos/test.pdf")
                .estado(Documento.EstadoProcesamiento.LISTO)
                .tamanoBytes(300L)
                .mensajeError(null)
                
                .build();
        when(documentoRepository.findByIdAndVehiculoId(5L, 1L))
                .thenReturn(java.util.Optional.empty());

        // When
        Optional<Documento> result = documentoService.findByIdAndVehiculoId(5L, 1L);

        // Then
        assertThat(result.isEmpty()).isTrue();
        verify(documentoRepository).findByIdAndVehiculoId(5L, 1L);
    }
}