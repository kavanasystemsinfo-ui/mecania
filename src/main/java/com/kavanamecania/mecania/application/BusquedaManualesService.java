package com.kavanamecania.mecania.application;

import com.kavanamecania.mecania.application.evento.DocumentoSubidoEvent;
import com.kavanamecania.mecania.domain.busqueda.BuscadorManuales;
import com.kavanamecania.mecania.domain.busqueda.CandidatoManual;
import com.kavanamecania.mecania.domain.descarga.ArchivoDescargado;
import com.kavanamecania.mecania.domain.descarga.DescargadorUrl;
import com.kavanamecania.mecania.domain.model.Documento;
import com.kavanamecania.mecania.domain.model.Vehiculo;
import com.kavanamecania.mecania.domain.storage.AlmacenamientoArchivos;
import com.kavanamecania.mecania.infrastructure.repository.DocumentoRepository;
import com.kavanamecania.mecania.infrastructure.repository.VehiculoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Búsqueda asistida de manuales: propone candidatos y descarga solo lo que el
 * usuario acepta explícitamente.
 *
 * <p>Nada se guarda por buscar: la descarga y el alta del documento ocurren
 * únicamente en {@link #importar}, y el procesamiento (extracción, chunking y
 * embeddings) se dispara con el mismo evento que usa la subida manual, para que
 * ambos caminos compartan un solo pipeline.</p>
 */
@Service
public class BusquedaManualesService {

    private final BuscadorManuales buscadorManuales;
    private final DescargadorUrl descargadorUrl;
    private final DocumentoRepository documentoRepository;
    private final VehiculoRepository vehiculoRepository;
    private final AlmacenamientoArchivos almacenamientoArchivos;
    private final ApplicationEventPublisher eventPublisher;

    public BusquedaManualesService(BuscadorManuales buscadorManuales,
                                   DescargadorUrl descargadorUrl,
                                   DocumentoRepository documentoRepository,
                                   VehiculoRepository vehiculoRepository,
                                   AlmacenamientoArchivos almacenamientoArchivos,
                                   ApplicationEventPublisher eventPublisher) {
        this.buscadorManuales = buscadorManuales;
        this.descargadorUrl = descargadorUrl;
        this.documentoRepository = documentoRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.almacenamientoArchivos = almacenamientoArchivos;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Propone candidatos para el vehículo. No descarga ni persiste nada.
     *
     * @param consultaLibre texto extra del usuario (puede ser null o vacío)
     */
    @Transactional(readOnly = true)
    public List<CandidatoManual> buscar(Long vehiculoId, String consultaLibre) {
        Vehiculo vehiculo = vehiculoDe(vehiculoId);
        return buscadorManuales.buscar(construirConsulta(vehiculo, consultaLibre));
    }

    /**
     * Descarga una URL aceptada por el usuario y la registra como documento del
     * vehículo. Si esa URL ya se importó para este vehículo, devuelve el
     * documento existente sin volver a descargar.
     */
    @Transactional
    public ResultadoImportacion importar(Long vehiculoId, String url) {
        Vehiculo vehiculo = vehiculoDe(vehiculoId);
        String urlLimpia = url == null ? "" : url.trim();

        Optional<Documento> yaImportado = documentoRepository.findByVehiculoIdAndOrigenUrl(vehiculoId, urlLimpia);
        if (yaImportado.isPresent()) {
            return new ResultadoImportacion(yaImportado.get(), true);
        }

        ArchivoDescargado archivo = descargadorUrl.descargar(urlLimpia);

        String rutaRelativa;
        try {
            rutaRelativa = almacenamientoArchivos.guardarArchivo(
                    archivo.contenido(), archivo.nombreArchivo(), "vehiculos/" + vehiculoId + "/documentos");
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el manual descargado: " + e.getMessage(), e);
        }

        Documento documento = Documento.builder()
                .vehiculo(vehiculo)
                .nombre(archivo.nombreArchivo())
                .tipo(archivo.tipo())
                .rutaAlmacenamiento(rutaRelativa)
                .estado(Documento.EstadoProcesamiento.PROCESANDO)
                .tamanoBytes((long) archivo.contenido().length)
                .origenUrl(archivo.urlFinal())
                .mensajeError(null)
                .build();

        Documento guardado = documentoRepository.save(documento);

        // Mismo evento que la subida manual: el listener AFTER_COMMIT lanza el
        // procesamiento cuando la transacción ya ha commiteado (ver ADR 004).
        eventPublisher.publishEvent(new DocumentoSubidoEvent(guardado.getId()));

        return new ResultadoImportacion(guardado, false);
    }

    private Vehiculo vehiculoDe(Long vehiculoId) {
        return vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new IllegalArgumentException("Vehiculo no encontrado: " + vehiculoId));
    }

    private static String construirConsulta(Vehiculo vehiculo, String consultaLibre) {
        StringBuilder consulta = new StringBuilder()
                .append(vehiculo.getMarca()).append(' ')
                .append(vehiculo.getModelo()).append(' ')
                .append(vehiculo.getAnio()).append(' ')
                .append("manual");
        if (consultaLibre != null && !consultaLibre.isBlank()) {
            consulta.append(' ').append(consultaLibre.trim());
        }
        return consulta.toString();
    }
}
