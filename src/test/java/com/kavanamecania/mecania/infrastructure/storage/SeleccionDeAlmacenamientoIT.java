package com.kavanamecania.mecania.infrastructure.storage;

import com.kavanamecania.mecania.domain.storage.AlmacenamientoArchivos;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Qué implementación de {@link AlmacenamientoArchivos} se registra según
 * {@code mecania.storage.tipo}.
 *
 * <p>Existe por un fallo real: los dos almacenes estaban anotados sin condición y
 * arrancar con {@code storage.tipo=s3} reventaba con "required a single bean, but
 * 2 were found". El contexto no se levanta si vuelve a pasar, así que este test
 * es la red que faltaba.</p>
 */
class SeleccionDeAlmacenamientoIT {

    @SpringBootTest
    @ActiveProfiles("test")
    @Nested
    class PorDefecto {

        @Autowired private AlmacenamientoArchivos almacenamiento;

        @Test
        void sin_configuracion_usa_el_disco_local() {
            assertThat(almacenamiento).isInstanceOf(AlmacenamientoDiscoLocal.class);
        }
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "mecania.storage.tipo=s3",
            "mecania.storage.s3.bucket=mecania-manuales-test",
            "mecania.storage.s3.endpoint=http://127.0.0.1:9000",
            "mecania.storage.s3.region=us-east-1",
            "mecania.storage.s3.access-key=prueba",
            "mecania.storage.s3.secret-key=prueba",
            "mecania.storage.s3.path-style=true"
    })
    @Nested
    class ModoObjetos {

        @Autowired private AlmacenamientoArchivos almacenamiento;

        @Test
        void con_tipo_s3_usa_el_almacen_de_objetos_y_no_el_disco() {
            assertThat(almacenamiento).isInstanceOf(AlmacenamientoS3.class);
        }
    }
}
