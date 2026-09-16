package com.kavanamecania.mecania.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class BusquedaManualesControllerIT {

    private static final String URL_MANUAL = "https://cdn.ejemplo.es/manuales/toyota-corolla-2018.pdf";

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper om;
    @Autowired private VehiculoRepository vehiculoRepository;
    @Autowired private DocumentoRepository documentoRepository;

    @MockitoBean private BuscadorManuales buscadorManuales;
    @MockitoBean private DescargadorUrl descargadorUrl;
    @MockitoBean private AlmacenamientoArchivos almacenamientoArchivos;

    private Vehiculo vehiculo;

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @BeforeEach
    void setUp() {
        documentoRepository.deleteAll();
        vehiculoRepository.deleteAll();
        vehiculo = vehiculoRepository.save(Vehiculo.builder()
                .usuarioId(1L).marca("Toyota").modelo("Corolla").anio(2018)
                .kilometraje(90000L).combustible(Combustible.GASOLINA).matricula("1234ABC")
                .build());
    }

    @Test
    void get_candidatos_devuelve_los_candidatos_y_no_descarga_nada() throws Exception {
        when(buscadorManuales.buscar(anyString())).thenReturn(List.of(
                new CandidatoManual("Manual Toyota Corolla 2018", URL_MANUAL, "608 páginas", "cdn.ejemplo.es", true)));

        mvc().perform(get("/api/vehiculos/" + vehiculo.getId() + "/manuales/candidatos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Manual Toyota Corolla 2018"))
                .andExpect(jsonPath("$[0].url").value(URL_MANUAL))
                .andExpect(jsonPath("$[0].fuente").value("cdn.ejemplo.es"))
                .andExpect(jsonPath("$[0].pdf").value(true));

        assertThat(documentoRepository.findAll()).isEmpty();
    }

    @Test
    void get_candidatos_pasa_la_consulta_libre_al_buscador() throws Exception {
        when(buscadorManuales.buscar(anyString())).thenReturn(List.of());

        mvc().perform(get("/api/vehiculos/" + vehiculo.getId() + "/manuales/candidatos")
                        .param("q", "cambio de aceite"))
                .andExpect(status().isOk());

        verify(buscadorManuales).buscar("Toyota Corolla 2018 manual cambio de aceite");
    }

    @Test
    void get_candidatos_devuelve_503_cuando_el_buscador_bloquea_la_peticion() throws Exception {
        when(buscadorManuales.buscar(anyString()))
                .thenThrow(new BusquedaException("El buscador externo nos ha bloqueado (anti-bot)"));

        mvc().perform(get("/api/vehiculos/" + vehiculo.getId() + "/manuales/candidatos"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("buscador_no_disponible"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("bloqueado")));
    }

    @Test
    void get_candidatos_devuelve_404_si_el_vehiculo_no_existe() throws Exception {
        mvc().perform(get("/api/vehiculos/999999/manuales/candidatos"))
                .andExpect(status().isNotFound());
    }

    @Test
    void post_importar_crea_el_documento_y_devuelve_201() throws Exception {
        prepararDescargaOk();

        mvc().perform(post("/api/vehiculos/" + vehiculo.getId() + "/manuales/importar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("url", URL_MANUAL))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.yaExistia").value(false))
                .andExpect(jsonPath("$.documento.nombre").value("manual-corolla-2018.pdf"))
                .andExpect(jsonPath("$.documento.tipo").value("PDF"))
                .andExpect(jsonPath("$.documento.estado").value("PROCESANDO"))
                .andExpect(jsonPath("$.documento.origenUrl").value(URL_MANUAL));

        List<Documento> documentos = documentoRepository.findAll();
        assertThat(documentos).hasSize(1);
        assertThat(documentos.get(0).getOrigenUrl()).isEqualTo(URL_MANUAL);
        assertThat(documentos.get(0).getVehiculo().getId()).isEqualTo(vehiculo.getId());
    }

    @Test
    void post_importar_devuelve_400_si_la_url_esta_vacia() throws Exception {
        mvc().perform(post("/api/vehiculos/" + vehiculo.getId() + "/manuales/importar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("url", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validacion"));

        assertThat(documentoRepository.findAll()).isEmpty();
    }

    @Test
    void post_importar_devuelve_400_si_la_url_no_es_http() throws Exception {
        when(descargadorUrl.descargar(anyString())).thenThrow(
                new DescargaException(MotivoDescarga.URL_INVALIDA, "La URL debe empezar por http:// o https://"));

        mvc().perform(post("/api/vehiculos/" + vehiculo.getId() + "/manuales/importar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("url", "file:///etc/passwd"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("url_no_valida"));

        assertThat(documentoRepository.findAll()).isEmpty();
    }

    @Test
    void post_importar_devuelve_415_si_el_archivo_no_es_un_manual() throws Exception {
        when(descargadorUrl.descargar(anyString())).thenThrow(
                new DescargaException(MotivoDescarga.TIPO_NO_SOPORTADO, "Tipo de archivo no soportado: text/html"));

        mvc().perform(post("/api/vehiculos/" + vehiculo.getId() + "/manuales/importar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("url", "https://ejemplo.es/pagina"))))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("tipo_no_soportado"));
    }

    @Test
    void post_importar_devuelve_413_si_el_archivo_supera_el_limite() throws Exception {
        when(descargadorUrl.descargar(anyString())).thenThrow(
                new DescargaException(MotivoDescarga.DEMASIADO_GRANDE, "El archivo supera el máximo"));

        mvc().perform(post("/api/vehiculos/" + vehiculo.getId() + "/manuales/importar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("url", URL_MANUAL))))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error").value("demasiado_grande"));
    }

    @Test
    void post_importar_devuelve_200_con_yaExistia_si_el_manual_ya_estaba_importado() throws Exception {
        prepararDescargaOk();
        Map<String, String> body = Map.of("url", URL_MANUAL);

        mvc().perform(post("/api/vehiculos/" + vehiculo.getId() + "/manuales/importar")
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mvc().perform(post("/api/vehiculos/" + vehiculo.getId() + "/manuales/importar")
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yaExistia").value(true))
                .andExpect(jsonPath("$.documento.origenUrl").value(URL_MANUAL));

        assertThat(documentoRepository.findAll()).hasSize(1);
    }

    @Test
    void post_importar_devuelve_404_si_el_vehiculo_no_existe() throws Exception {
        mvc().perform(post("/api/vehiculos/999999/manuales/importar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("url", URL_MANUAL))))
                .andExpect(status().isNotFound());
    }

    private void prepararDescargaOk() throws Exception {
        when(descargadorUrl.descargar(URL_MANUAL)).thenReturn(new ArchivoDescargado(
                "manual-corolla-2018.pdf", Documento.TipoDocumento.PDF,
                "%PDF-1.4 contenido".getBytes(StandardCharsets.UTF_8), "application/pdf", URL_MANUAL));
        when(almacenamientoArchivos.guardarArchivo(any(byte[].class), anyString(), anyString()))
                .thenReturn("vehiculos/" + vehiculo.getId() + "/documentos/manual-corolla-2018.pdf");
    }
}
