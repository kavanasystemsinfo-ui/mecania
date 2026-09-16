package com.kavanamecania.mecania.application;

import com.kavanamecania.mecania.application.dto.AlertaRequest;
import com.kavanamecania.mecania.domain.exception.AlertaNotFoundException;
import com.kavanamecania.mecania.domain.exception.VehiculoNotFoundException;
import com.kavanamecania.mecania.domain.model.Alerta;
import com.kavanamecania.mecania.domain.model.Repetitividad;
import com.kavanamecania.mecania.domain.model.TipoAlerta;
import com.kavanamecania.mecania.domain.model.Vehiculo;
import com.kavanamecania.mecania.infrastructure.repository.AlertaRepository;
import com.kavanamecania.mecania.infrastructure.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    @Mock
    private AlertaRepository alertaRepository;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @InjectMocks
    private AlertaService alertaService;

    private Vehiculo vehiculo;

    @BeforeEach
    void setUp() {
        vehiculo = Vehiculo.builder()
                .id(1L)
                .usuarioId(1L)
                .marca("Toyota")
                .modelo("Corolla")
                .anio(2020)
                .build();
    }

    private AlertaRequest request() {
        return new AlertaRequest(
                TipoAlerta.ITV, "Revisar ITV", LocalDate.now(), Repetitividad.UNICA);
    }

    @Test
    void crear_vehiculoExistente_guardaAlertaActiva() {
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        Alerta guardada = Alerta.builder()
                .id(10L)
                .vehiculo(vehiculo)
                .tipo(TipoAlerta.ITV)
                .descripcion("Revisar ITV")
                .fecha(LocalDate.now())
                .repetitividad(Repetitividad.UNICA)
                .activa(true)
                .build();
        when(alertaRepository.save(any(Alerta.class))).thenReturn(guardada);

        Alerta result = alertaService.crear(1L, request());

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.isActiva()).isTrue();
        assertThat(result.getVehiculo()).isEqualTo(vehiculo);
        assertThat(result.getTipo()).isEqualTo(TipoAlerta.ITV);
    }

    @Test
    void crear_vehiculoInexistente_lanzaVehiculoNotFoundException() {
        when(vehiculoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertaService.crear(999L, request()))
                .isInstanceOf(VehiculoNotFoundException.class);
    }

    @Test
    void listar_devuelveListaDeAlertas() {
        Alerta a1 = Alerta.builder().id(1L).vehiculo(vehiculo).build();
        Alerta a2 = Alerta.builder().id(2L).vehiculo(vehiculo).build();
        when(alertaRepository.findByVehiculoId(1L)).thenReturn(List.of(a1, a2));

        List<Alerta> result = alertaService.listar(1L);

        assertThat(result).hasSize(2);
        verify(alertaRepository).findByVehiculoId(1L);
    }

    @Test
    void actualizar_existente_seteaCampos() {
        Alerta existente = Alerta.builder()
                .id(5L)
                .vehiculo(vehiculo)
                .tipo(TipoAlerta.ACEITE)
                .descripcion("Cambio aceite")
                .fecha(LocalDate.now())
                .repetitividad(Repetitividad.MENSUAL)
                .activa(true)
                .build();
        when(alertaRepository.findByVehiculoIdAndId(1L, 5L)).thenReturn(Optional.of(existente));
        when(alertaRepository.save(any(Alerta.class))).thenReturn(existente);

        Alerta result = alertaService.actualizar(1L, 5L,
                new AlertaRequest(TipoAlerta.FRENOS, "Frenos", LocalDate.now(), Repetitividad.ANUAL));

        assertThat(result.getTipo()).isEqualTo(TipoAlerta.FRENOS);
        assertThat(result.getDescripcion()).isEqualTo("Frenos");
        assertThat(result.getRepetitividad()).isEqualTo(Repetitividad.ANUAL);
    }

    @Test
    void actualizar_noEncontrada_lanzaAlertaNotFoundException() {
        when(alertaRepository.findByVehiculoIdAndId(1L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertaService.actualizar(1L, 5L, request()))
                .isInstanceOf(AlertaNotFoundException.class);
    }

    @Test
    void eliminar_existente_borra() {
        Alerta existente = Alerta.builder().id(5L).vehiculo(vehiculo).build();
        when(alertaRepository.findByVehiculoIdAndId(1L, 5L)).thenReturn(Optional.of(existente));

        alertaService.eliminar(1L, 5L);

        verify(alertaRepository).delete(existente);
    }

    @Test
    void eliminar_noEncontrada_lanzaAlertaNotFoundException() {
        when(alertaRepository.findByVehiculoIdAndId(1L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertaService.eliminar(1L, 5L))
                .isInstanceOf(AlertaNotFoundException.class);
    }

    @Test
    void vencidas_unicaVencida_seDesactiva() {
        Alerta unica = Alerta.builder()
                .id(1L)
                .vehiculo(vehiculo)
                .tipo(TipoAlerta.ITV)
                .descripcion("ITV")
                .fecha(LocalDate.now().minusDays(1))
                .repetitividad(Repetitividad.UNICA)
                .activa(true)
                .build();
        when(alertaRepository.findByVehiculoId(1L)).thenReturn(List.of(unica));

        List<Alerta> result = alertaService.vencidas(1L);

        assertThat(result).containsExactly(unica);
        assertThat(unica.isActiva()).isFalse();
        verify(alertaRepository).saveAll(result);
    }

    @Test
    void vencidas_mensualVencida_avanzaFecha() {
        LocalDate vencida = LocalDate.now().minusDays(10);
        Alerta mensual = Alerta.builder()
                .id(2L)
                .vehiculo(vehiculo)
                .tipo(TipoAlerta.ACEITE)
                .descripcion("Aceite")
                .fecha(vencida)
                .repetitividad(Repetitividad.MENSUAL)
                .activa(true)
                .build();
        when(alertaRepository.findByVehiculoId(1L)).thenReturn(List.of(mensual));

        List<Alerta> result = alertaService.vencidas(1L);

        assertThat(result).containsExactly(mensual);
        assertThat(mensual.getFecha()).isEqualTo(vencida.plusMonths(1));
        assertThat(mensual.isActiva()).isTrue();
    }

    @Test
    void vencidas_sinVencidas_devuelveVacio() {
        when(alertaRepository.findByVehiculoId(1L)).thenReturn(List.of());

        List<Alerta> result = alertaService.vencidas(1L);

        assertThat(result).isEmpty();
    }
}
