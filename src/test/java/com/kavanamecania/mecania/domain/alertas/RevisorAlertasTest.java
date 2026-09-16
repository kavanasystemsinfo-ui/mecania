package com.kavanamecania.mecania.domain.alertas;

import com.kavanamecania.mecania.domain.model.Alerta;
import com.kavanamecania.mecania.domain.model.Repetitividad;
import com.kavanamecania.mecania.domain.model.TipoAlerta;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RevisorAlertasTest {

    private final RevisorAlertas revisor = new RevisorAlertas();
    private final LocalDate hoy = LocalDate.of(2026, 9, 16);

    private static Alerta alerta(LocalDate fecha, Repetitividad rep, boolean activa) {
        return Alerta.builder()
                .tipo(TipoAlerta.ITV)
                .descripcion("ITV")
                .fecha(fecha)
                .repetitividad(rep)
                .activa(activa)
                .build();
    }

    @Test
    void lista_vacia_devuelve_lista_vacia() {
        assertThat(revisor.vencidas(List.of(), hoy)).isEmpty();
    }

    @Test
    void alerta_futura_no_se_devuelve() {
        Alerta futura = alerta(hoy.plusDays(1), Repetitividad.UNICA, true);
        assertThat(revisor.vencidas(List.of(futura), hoy)).isEmpty();
        assertThat(futura.isActiva()).isTrue();
    }

    @Test
    void alerta_inactiva_no_se_devuelve() {
        Alerta inactiva = alerta(hoy.minusDays(1), Repetitividad.UNICA, false);
        assertThat(revisor.vencidas(List.of(inactiva), hoy)).isEmpty();
    }

    @Test
    void unica_vencida_se_devuelve_y_se_desactiva() {
        Alerta unica = alerta(hoy, Repetitividad.UNICA, true);
        assertThat(revisor.vencidas(List.of(unica), hoy)).containsExactly(unica);
        assertThat(unica.isActiva()).isFalse();
    }

    @Test
    void mensual_vencida_se_devuelve_y_avanza_un_mes() {
        Alerta mensual = alerta(hoy.minusDays(10), Repetitividad.MENSUAL, true);
        assertThat(revisor.vencidas(List.of(mensual), hoy)).containsExactly(mensual);
        assertThat(mensual.getFecha()).isEqualTo(hoy.minusDays(10).plusMonths(1));
        assertThat(mensual.estaVencida(hoy)).isFalse();
    }

    @Test
    void anual_vencida_hace_mucho_salta_ocorrencias_hasta_el_futuro() {
        Alerta anual = alerta(hoy.minusYears(3), Repetitividad.ANUAL, true);
        assertThat(revisor.vencidas(List.of(anual), hoy)).containsExactly(anual);
        assertThat(anual.getFecha().isAfter(hoy)).isTrue();
        assertThat(anual.estaVencida(hoy)).isFalse();
    }

    @Test
    void mensual_vencida_hace_mucho_salta_hasta_quedar_en_el_futuro() {
        Alerta mensual = alerta(hoy.minusMonths(6), Repetitividad.MENSUAL, true);
        assertThat(revisor.vencidas(List.of(mensual), hoy)).containsExactly(mensual);
        assertThat(mensual.getFecha().isAfter(hoy)).isTrue();
    }
}
