package com.kavanamecania.mecania.infrastructure.descarga;

import com.kavanamecania.mecania.domain.descarga.DescargaException;
import com.kavanamecania.mecania.domain.descarga.MotivoDescarga;
import com.kavanamecania.mecania.domain.descarga.ValidadorDestinoDescarga;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Specs del guardián SSRF. Sin red: los hosts privados se rechazan ANTES de
 * abrir conexión (IPs literales, {@code localhost} y nombres que resuelven a
 * rangos privados).
 */
class ValidadorHostsPublicosTest {

    private final ValidadorHostsPublicos validador = new ValidadorHostsPublicos();

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1/manual.pdf",
            "http://127.0.0.1:8080/manual.pdf",
            "http://localhost/manual.pdf",
            "http://localhost:5433/manual.pdf",
            "http://[::1]/manual.pdf",
            "http://0.0.0.0/manual.pdf",
            "http://10.0.0.5/manual.pdf",
            "http://172.16.4.4/manual.pdf",
            "http://172.31.255.254/manual.pdf",
            "http://192.168.1.10/manual.pdf",
            "http://169.254.169.254/latest/meta-data/",
            "http://metadata.google.internal/manual.pdf"
    })
    void rechaza_hosts_privados_o_de_metadatos(String url) {
        assertThatThrownBy(() -> validador.validar(java.net.URI.create(url)))
                .isInstanceOf(DescargaException.class)
                .satisfies(e -> assertThat(((DescargaException) e).motivo()).isEqualTo(MotivoDescarga.HOST_NO_PERMITIDO));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.toyota.com/owners/manuals/corolla-2018.pdf",
            "http://cdn.ejemplo.es/manuales/seat-ibiza.pdf",
            "https://manua.ls/toyota/corolla-2018/manual"
    })
    void acepta_hosts_publicos(String url) {
        assertThatCode(() -> validador.validar(java.net.URI.create(url))).doesNotThrowAnyException();
    }

    @Test
    void rechaza_hosts_sin_nombre() {
        assertThatThrownBy(() -> validador.validar(java.net.URI.create("http:///manual.pdf")))
                .isInstanceOf(DescargaException.class);
    }

    @Test
    void el_validador_permisivo_acepta_loopback_para_entornos_de_desarrollo() {
        assertThatCode(() -> ValidadorDestinoDescarga.permitirTodo()
                .validar(java.net.URI.create("http://127.0.0.1:8080/manual.pdf")))
                .doesNotThrowAnyException();
    }
}
