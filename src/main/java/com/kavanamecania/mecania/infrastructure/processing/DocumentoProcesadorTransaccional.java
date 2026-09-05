package com.kavanamecania.mecania.infrastructure.processing;

import com.kavanamecania.mecania.domain.chunking.Chunker;
import com.kavanamecania.mecania.domain.chunking.SlidingWindowChunker;
import com.kavanamecania.mecania.domain.embedding.Embedding;
import com.kavanamecania.mecania.domain.embedding.EmbeddingException;
import com.kavanamecania.mecania.domain.embedding.EmbeddingService;
import com.kavanamecania.mecania.domain.extraction.DocumentoTipo;
import com.kavanamecania.mecania.domain.extraction.ExtractionException;
import com.kavanamecania.mecania.domain.model.Documento;
import com.kavanamecania.mecania.domain.model.Fragmento;
import com.kavanamecania.mecania.domain.storage.AlmacenamientoArchivos;
import com.kavanamecania.mecania.infrastructure.extraction.TextExtractorFactory;
import com.kavanamecania.mecania.infrastructure.repository.FragmentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Paso transaccional del procesamiento de documentos.
 *
 * <p>Vive en un bean SEPARADO de {@link DocumentoProcessor} a propósito: el
 * {@code @Transactional} aquí se aplica de verdad porque la llamada viene de
 * otro bean (vía proxy de Spring). Si estuviera en la misma clase y se
 * invocara internamente (self-invocation), la anotación no tendría efecto y
 * cada {@code save()} sería su propia transacción (fragmentos parciales ante
 * un fallo a mitad).</p>
 *
 * <p>Flujo: lee bytes → extrae texto → chunking → embedding (uno por
 * fragmento) → persiste fragmentos. Cualquier excepción revierte TODO lo
 * insertado en esta transacción.</p>
 */
@Component
public class DocumentoProcesadorTransaccional {

    private static final Logger log = LoggerFactory.getLogger(DocumentoProcesadorTransaccional.class);

    private final FragmentoRepository fragmentoRepository;
    private final AlmacenamientoArchivos almacenamiento;
    private final TextExtractorFactory extractorFactory;
    private final EmbeddingService embeddingService;
    private final Chunker chunker;
    private final DocumentoProcessorStatusUpdater statusUpdater;

    public DocumentoProcesadorTransaccional(
            FragmentoRepository fragmentoRepository,
            AlmacenamientoArchivos almacenamiento,
            TextExtractorFactory extractorFactory,
            EmbeddingService embeddingService,
            @Value("${mecania.chunking.chunk-size:512}") int chunkSize,
            @Value("${mecania.chunking.overlap:64}") int overlap,
            DocumentoProcessorStatusUpdater statusUpdater) {
        this.fragmentoRepository = fragmentoRepository;
        this.almacenamiento = almacenamiento;
        this.extractorFactory = extractorFactory;
        this.embeddingService = embeddingService;
        this.chunker = new SlidingWindowChunker(chunkSize, overlap);
        this.statusUpdater = statusUpdater;
    }

    /**
     * Procesa el documento dentro de una transacción: extracción → chunking →
     * embeddings → persistencia de fragmentos. Si algo falla, la transacción
     * revierte y el documento queda sin fragmentos (estado ERROR lo marca el
     * llamador).
     *
     * <p>rollbackFor=Exception.class es OBLIGATORIO: EmbeddingException y
     * ExtractionException son checked (no extienden RuntimeException), y el
     * default de Spring solo revierte unchecked. Sin esto, un fallo a mitad
     * commitea los fragmentos ya guardados (bug detectado por
     * DocumentoProcessorIT.procesar_con_fallo_a_mitad_en_segundo_fragmento).</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void doProcesar(Long documentoId) throws ExtractionException, EmbeddingException {
        Documento documento = statusUpdater.findDocumento(documentoId);

        // 1. Leer bytes del archivo
        byte[] contenido;
        try {
            contenido = almacenamiento.leerArchivo(documento.getRutaAlmacenamiento());
        } catch (Exception e) {
            throw new ExtractionException("No se pudo leer el archivo físico: " + e.getMessage(), e);
        }

        // 2. Extraer texto
        DocumentoTipo tipo = DocumentoTipo.valueOf(documento.getTipo().name());
        String texto = extractorFactory.extraer(tipo, contenido);

        if (texto == null || texto.isBlank()) {
            throw new ExtractionException("El documento no contiene texto extraíble");
        }

        // 3. Chunking
        List<String> chunks = chunker.chunk(texto);
        if (chunks.isEmpty()) {
            throw new ExtractionException("El documento no produjo fragmentos");
        }

        // 4. Calcular embeddings y persistir fragmentos
        int posicion = 0;
        for (String chunk : chunks) {
            Embedding emb = embeddingService.embed(chunk);
            Fragmento fragmento = Fragmento.builder()
                    .documento(documento)
                    .posicion(posicion++)
                    .texto(chunk)
                    .build();
            // El embedding se calcula pero NO se persiste en BD (decisión ADR 004).
            // Se descarta aquí; cuando llegue la fase 4 se añadirá una columna vector(N)
            // mediante migración SQL + JDBC nativo.
            if (emb != null) {
                // Reference to silence "unused variable" warning; in phase 4 this is persisted.
                log.trace("Embedding calculado para fragmento {} (dim {})", posicion, emb.dimension());
            }
            fragmentoRepository.save(fragmento);
        }

        log.info("Documento {} procesado: {} fragmentos", documentoId, chunks.size());
    }
}