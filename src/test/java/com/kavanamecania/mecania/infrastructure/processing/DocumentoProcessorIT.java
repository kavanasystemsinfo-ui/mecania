package com.kavanamecania.mecania.infrastructure.processing;

import com.kavanamecania.mecania.domain.embedding.Embedding;
import com.kavanamecania.mecania.domain.embedding.EmbeddingException;
import com.kavanamecania.mecania.domain.embedding.EmbeddingService;
import com.kavanamecania.mecania.domain.model.Combustible;
import com.kavanamecania.mecania.domain.model.Documento;
import com.kavanamecania.mecania.domain.model.Fragmento;
import com.kavanamecania.mecania.domain.model.Vehiculo;
import com.kavanamecania.mecania.domain.storage.AlmacenamientoArchivos;
import com.kavanamecania.mecania.infrastructure.repository.DocumentoRepository;
import com.kavanamecania.mecania.infrastructure.repository.FragmentoRepository;
import com.kavanamecania.mecania.infrastructure.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test de integración del flujo completo de procesamiento.
 * Carga el contexto de Spring (incluyendo @Async) y mockea los componentes externos
 * (embedding, almacenamiento). Persiste en BD H2 en memoria para verificar la transacción.
 */
@SpringBootTest
class DocumentoProcessorIT {

    @Autowired private DocumentoProcessor processor;
    @Autowired private DocumentoRepository documentoRepository;
    @Autowired private VehiculoRepository vehiculoRepository;
    @Autowired private FragmentoRepository fragmentoRepository;

    @MockitoBean private AlmacenamientoArchivos almacenamientoArchivos;
    @MockitoBean private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() throws EmbeddingException {
        fragmentoRepository.deleteAll();
        documentoRepository.deleteAll();
        vehiculoRepository.deleteAll();
        when(embeddingService.embed(anyString()))
                .thenReturn(new Embedding(new double[]{0.1, 0.2, 0.3, 0.4}));
        when(embeddingService.dimension()).thenReturn(4);
    }

    @Test
    void doProcesar_extrae_chunckea_y_persiste_fragmentos() throws Exception {
        // Given: vehículo y documento en BD
        final Vehiculo vehiculo = vehiculoRepository.save(Vehiculo.builder()
                .usuarioId(1L).marca("Seat").modelo("Ibiza").anio(2020)
                .kilometraje(10000L).combustible(Combustible.GASOLINA).build());

        final Documento documento = documentoRepository.save(Documento.builder()
                .vehiculo(vehiculo)
                .nombre("manual.txt")
                .tipo(Documento.TipoDocumento.TXT)
                .rutaAlmacenamiento("vehiculos/1/manual.txt")
                .estado(Documento.EstadoProcesamiento.PROCESANDO)
                .tamanoBytes(1000L)
                .build());

        String texto = "Frase uno del manual. ".repeat(50); // ~1100 chars
        when(almacenamientoArchivos.leerArchivo("vehiculos/1/manual.txt"))
                .thenReturn(texto.getBytes(StandardCharsets.UTF_8));

        // When
        processor.doProcesar(documento.getId());

        // Then: fragmentos persistidos
        List<Fragmento> fragmentos = fragmentoRepository.findAll();
        assertThat(fragmentos).isNotEmpty();
        assertThat(fragmentos).allMatch(f -> f.getTexto() != null && !f.getTexto().isBlank());
        assertThat(fragmentos).allMatch(f -> f.getDocumento().getId().equals(documento.getId()));
        assertThat(fragmentos).allMatch(f -> f.getPosicion() != null && f.getPosicion() >= 0);

        // Todos los fragmentos son <= 512 (configuración del chunker)
        for (int i = 0; i < fragmentos.size() - 1; i++) {
            assertThat(fragmentos.get(i).getTexto()).hasSizeLessThanOrEqualTo(512);
        }
    }

    @Test
    void procesar_sin_api_key_marca_error_y_no_persiste_fragmentos() throws Exception {
        // Given: documento pero embedding service que falla
        when(embeddingService.embed(anyString()))
                .thenThrow(new EmbeddingException("No hay API key"));

        final Vehiculo vehiculo = vehiculoRepository.save(Vehiculo.builder()
                .usuarioId(2L).marca("Ford").modelo("Focus").anio(2019)
                .kilometraje(20000L).combustible(Combustible.DIESEL).build());

        final Documento documento = documentoRepository.save(Documento.builder()
                .vehiculo(vehiculo)
                .nombre("manual.txt")
                .tipo(Documento.TipoDocumento.TXT)
                .rutaAlmacenamiento("vehiculos/2/manual.txt")
                .estado(Documento.EstadoProcesamiento.PROCESANDO)
                .tamanoBytes(500L)
                .build());

        when(almacenamientoArchivos.leerArchivo("vehiculos/2/manual.txt"))
                .thenReturn("Texto de prueba".getBytes(StandardCharsets.UTF_8));

        // When
        processor.procesar(documento.getId());

        // Then: espera determinista al hilo async (polling, no Thread.sleep)
        Optional<Documento> resultado = esperarEstado(documento.getId(), Documento.EstadoProcesamiento.ERROR, Duration.ofSeconds(10));
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getEstado()).isEqualTo(Documento.EstadoProcesamiento.ERROR);
        assertThat(resultado.get().getMensajeError()).contains("No hay API key");

        // No hay fragmentos persistidos (transacción hizo rollback)
        assertThat(fragmentoRepository.findAll()).isEmpty();
    }

    @Test
    void procesar_con_fallo_a_mitad_no_deja_fragmentos_parciales() throws Exception {
        // Given: documento cuyo embedding falla en el SEGUNDO fragmento.
        when(embeddingService.embed(anyString()))
                .thenReturn(new Embedding(new double[]{0.1, 0.2, 0.3, 0.4}))
                .thenThrow(new EmbeddingException("Fallo en el segundo embedding"));

        final Vehiculo vehiculo = vehiculoRepository.save(Vehiculo.builder()
                .usuarioId(3L).marca("Renault").modelo("Clio").anio(2018)
                .kilometraje(30000L).combustible(Combustible.GASOLINA).build());

        final Documento documento = documentoRepository.save(Documento.builder()
                .vehiculo(vehiculo)
                .nombre("manual.txt")
                .tipo(Documento.TipoDocumento.TXT)
                .rutaAlmacenamiento("vehiculos/3/manual.txt")
                .estado(Documento.EstadoProcesamiento.PROCESANDO)
                .tamanoBytes(500L)
                .build());

        // Texto que produce 2+ chunks con chunk-size 512 / overlap 64:
        // "xxxx... " repetido 100 veces ≈ 600 chars → 2 chunks
        String texto = ("Frase del manual con contenido técnico para probar el corte. ".repeat(10));
        when(almacenamientoArchivos.leerArchivo("vehiculos/3/manual.txt"))
                .thenReturn(texto.getBytes(StandardCharsets.UTF_8));

        // When: el fallo a mitad debe propagarse (el estado lo marca el orquestador async)
        assertThatThrownBy(() -> processor.doProcesar(documento.getId()))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("segundo embedding");

        // Then: el fallo a mitad revierte la transacción → CERO fragmentos.
        assertThat(fragmentoRepository.findAll()).isEmpty();
    }

    private Optional<Documento> esperarEstado(Long documentoId, Documento.EstadoProcesamiento estado, Duration timeout) throws InterruptedException {
        Instant limite = Instant.now().plus(timeout);
        Optional<Documento> actual;
        do {
            actual = documentoRepository.findById(documentoId);
            if (actual.isPresent() && actual.get().getEstado() == estado) {
                return actual;
            }
            Thread.sleep(100);
        } while (Instant.now().isBefore(limite));
        return actual;
    }
}