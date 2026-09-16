package com.kavanamecania.mecania.application;

import com.kavanamecania.mecania.application.evento.DocumentoSubidoEvent;
import com.kavanamecania.mecania.domain.busqueda.BuscadorManuales;
import com.kavanamecania.mecania.domain.busqueda.BusquedaException;
import com.kavanamecania.mecania.domain.busqueda.CandidatoManual;
import com.kavanamecania.mecania.domain.descarga.ArchivoDescargado;
import com.kavanamecania.mecania.domain.descarga.DescargadorUrl;
import com.kavanamecania.mecania.domain.descarga.DescargaException;
import com.kavanamecania.mecania.domain.descarga.MotivoDescarga;
import com.kavanamecania.mecania.domain.model.Combustible;
import com.kavanamecania.mecania.domain.model.Documento;
import com.kavanamecania.mecania.domain.model.Vehiculo;
import com.kavanamecania.mecania.domain.storage.AlmacenamientoArchivos;
import com.kavanamecania.mecania.infrastructure.repository.DocumentoRepository;
import com.kavanamecania.mecania.infrastructure.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusquedaManualesServiceTest {

    private static final String URL_MANUAL = "https://cdn.ejemplo.es/manuales/toyota-corolla-2018.pdf";

    @Mock private BuscadorManuales buscadorManuales;
    @Mock private DescargadorUrl descargadorUrl;
    @Mock private DocumentoRepository documentoRepository;
    @Mock private VehiculoRepository vehiculoRepository;
    @Mock private AlmacenamientoArchivos almacenamientoArchivos;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private BusquedaManualesService service;

    private Vehiculo vehiculo;

    @BeforeEach
    void setUp() {
        vehiculo = Vehiculo.builder()
                .id(1L).usuarioId(1L).marca("Toyota").modelo("Corolla").anio(2018)
                .kilometraje(90000L).combustible(Combustible.GASOLINA)
                .build();
    }

    @Test
    void buscar_construye_la_consulta_con_marca_modelo_anio_y_manual() {
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(buscadorManuales.buscar(anyString())).thenReturn(List.of());

        service.buscar(1L, null);

        verify(buscadorManuales).buscar("Toyota Corolla 2018 manual");
    }

    @Test
    void buscar_añade_la_consulta_libre_del_usuario() {
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(buscadorManuales.buscar(anyString())).thenReturn(List.of());

        service.buscar(1L, "cambio de aceite");

        verify(buscadorManuales).buscar("Toyota Corolla 2018 manual cambio de aceite");
    }

    @Test
    void buscar_ignora_una_consulta_libre_en_blanco() {
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(buscadorManuales.buscar(anyString())).thenReturn(List.of());

        service.buscar(1L, "   ");

        verify(buscadorManuales).buscar("Toyota Corolla 2018 manual");
    }

    @Test
    void buscar_devuelve_los_candidatos_del_buscador() {
        CandidatoManual candidato = new CandidatoManual("Manual Corolla", URL_MANUAL, "608 páginas", "cdn.ejemplo.es", true);
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(buscadorManuales.buscar(anyString())).thenReturn(List.of(candidato));

        assertThat(service.buscar(1L, null)).containsExactly(candidato);
    }

    @Test
    void buscar_falla_si_el_vehiculo_no_existe() {
        when(vehiculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");

        verifyNoInteractions(buscadorManuales);
    }

    @Test
    void buscar_propaga_el_fallo_del_buscador_sin_ocultarlo() {
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(buscadorManuales.buscar(anyString())).thenThrow(new BusquedaException("bloqueado por anti-bot"));

        assertThatThrownBy(() -> service.buscar(1L, null))
                .isInstanceOf(BusquedaException.class)
                .hasMessageContaining("bloqueado");
    }

    @Test
    void importar_descarga_guarda_el_archivo_y_crea_el_documento() throws IOException {
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(documentoRepository.findByVehiculoIdAndOrigenUrl(1L, URL_MANUAL)).thenReturn(Optional.empty());
        when(descargadorUrl.descargar(URL_MANUAL)).thenReturn(new ArchivoDescargado(
                "manual-corolla-2018.pdf", Documento.TipoDocumento.PDF,
                "%PDF-1.4 contenido".getBytes(StandardCharsets.UTF_8), "application/pdf", URL_MANUAL));
        when(almacenamientoArchivos.guardarArchivo(any(byte[].class), eq("manual-corolla-2018.pdf"), eq("vehiculos/1/documentos")))
                .thenReturn("vehiculos/1/documentos/manual-corolla-2018.pdf");
        when(documentoRepository.save(any(Documento.class))).thenAnswer(inv -> {
            Documento d = inv.getArgument(0);
            d.setId(7L);
            return d;
        });

        ResultadoImportacion resultado = service.importar(1L, URL_MANUAL);
        Documento documento = resultado.documento();

        assertThat(resultado.yaExistia()).isFalse();
        assertThat(documento.getId()).isEqualTo(7L);
        assertThat(documento.getNombre()).isEqualTo("manual-corolla-2018.pdf");
        assertThat(documento.getTipo()).isEqualTo(Documento.TipoDocumento.PDF);
        assertThat(documento.getEstado()).isEqualTo(Documento.EstadoProcesamiento.PROCESANDO);
        assertThat(documento.getOrigenUrl()).isEqualTo(URL_MANUAL);
        assertThat(documento.getTamanoBytes()).isEqualTo("%PDF-1.4 contenido".getBytes(StandardCharsets.UTF_8).length);
        assertThat(documento.getRutaAlmacenamiento()).isEqualTo("vehiculos/1/documentos/manual-corolla-2018.pdf");
        assertThat(documento.getVehiculo()).isEqualTo(vehiculo);
    }

    @Test
    void importar_publica_el_evento_de_procesamiento_con_el_id_guardado() throws IOException {
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(documentoRepository.findByVehiculoIdAndOrigenUrl(1L, URL_MANUAL)).thenReturn(Optional.empty());
        when(descargadorUrl.descargar(URL_MANUAL)).thenReturn(new ArchivoDescargado(
                "manual.pdf", Documento.TipoDocumento.PDF, "%PDF-1.4".getBytes(StandardCharsets.UTF_8),
                "application/pdf", URL_MANUAL));
        when(almacenamientoArchivos.guardarArchivo(any(byte[].class), anyString(), anyString()))
                .thenReturn("vehiculos/1/documentos/manual.pdf");
        when(documentoRepository.save(any(Documento.class))).thenAnswer(inv -> {
            Documento d = inv.getArgument(0);
            d.setId(7L);
            return d;
        });

        service.importar(1L, URL_MANUAL);

        ArgumentCaptor<DocumentoSubidoEvent> evento = ArgumentCaptor.forClass(DocumentoSubidoEvent.class);
        verify(eventPublisher).publishEvent(evento.capture());
        assertThat(evento.getValue().documentoId()).isEqualTo(7L);
    }

    @Test
    void importar_no_recorta_la_url_guardada_ni_el_espacio_en_blanco() throws IOException {
        // La URL se normaliza (trim) pero no se altera el contenido: si el usuario
        // pega espacios, buscamos el documento ya importado por la URL limpia.
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(documentoRepository.findByVehiculoIdAndOrigenUrl(1L, URL_MANUAL)).thenReturn(Optional.empty());
        when(descargadorUrl.descargar(URL_MANUAL)).thenReturn(new ArchivoDescargado(
                "manual.pdf", Documento.TipoDocumento.PDF, "%PDF-1.4".getBytes(StandardCharsets.UTF_8),
                "application/pdf", URL_MANUAL));
        when(almacenamientoArchivos.guardarArchivo(any(byte[].class), anyString(), anyString()))
                .thenReturn("vehiculos/1/documentos/manual.pdf");
        when(documentoRepository.save(any(Documento.class))).thenAnswer(inv -> inv.getArgument(0));

        service.importar(1L, "  " + URL_MANUAL + "  ");

        verify(descargadorUrl).descargar(URL_MANUAL);
    }

    @Test
    void importar_devuelve_el_documento_existente_sin_volver_a_descargar() throws IOException {
        Documento existente = Documento.builder()
                .id(3L).vehiculo(vehiculo).nombre("manual-corolla-2018.pdf")
                .tipo(Documento.TipoDocumento.PDF).rutaAlmacenamiento("vehiculos/1/documentos/manual-corolla-2018.pdf")
                .estado(Documento.EstadoProcesamiento.LISTO).tamanoBytes(123L).origenUrl(URL_MANUAL)
                .build();
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(documentoRepository.findByVehiculoIdAndOrigenUrl(1L, URL_MANUAL)).thenReturn(Optional.of(existente));

        ResultadoImportacion resultado = service.importar(1L, URL_MANUAL);

        assertThat(resultado.documento()).isSameAs(existente);
        assertThat(resultado.yaExistia()).isTrue();
        verifyNoInteractions(descargadorUrl);
        verify(documentoRepository, never()).save(any(Documento.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void importar_no_crea_documento_ni_guarda_archivo_si_la_descarga_falla() throws IOException {
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(documentoRepository.findByVehiculoIdAndOrigenUrl(1L, URL_MANUAL)).thenReturn(Optional.empty());
        when(descargadorUrl.descargar(URL_MANUAL))
                .thenThrow(new DescargaException(MotivoDescarga.TIPO_NO_SOPORTADO, "El archivo no es un manual (text/html)"));

        assertThatThrownBy(() -> service.importar(1L, URL_MANUAL))
                .isInstanceOf(DescargaException.class)
                .hasMessageContaining("no es un manual");

        verify(documentoRepository, never()).save(any(Documento.class));
        verifyNoInteractions(almacenamientoArchivos);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void importar_falla_si_el_vehiculo_no_existe() {
        when(vehiculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.importar(99L, URL_MANUAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");

        verifyNoInteractions(descargadorUrl);
    }

    @Test
    void importar_envuelve_un_fallo_de_escritura_con_mensaje_explicito() throws IOException {
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(documentoRepository.findByVehiculoIdAndOrigenUrl(1L, URL_MANUAL)).thenReturn(Optional.empty());
        when(descargadorUrl.descargar(URL_MANUAL)).thenReturn(new ArchivoDescargado(
                "manual.pdf", Documento.TipoDocumento.PDF, "%PDF-1.4".getBytes(StandardCharsets.UTF_8),
                "application/pdf", URL_MANUAL));
        when(almacenamientoArchivos.guardarArchivo(any(byte[].class), anyString(), anyString()))
                .thenThrow(new IOException("disco lleno"));

        assertThatThrownBy(() -> service.importar(1L, URL_MANUAL))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("disco lleno");

        verify(documentoRepository, never()).save(any(Documento.class));
    }
}
