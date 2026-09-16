package com.kavanamecania.mecania.domain.service;

import com.kavanamecania.mecania.application.dto.VehiculoRequest;
import com.kavanamecania.mecania.domain.exception.VehiculoDuplicadoException;
import com.kavanamecania.mecania.domain.exception.VehiculoNotFoundException;
import com.kavanamecania.mecania.domain.model.Combustible;
import com.kavanamecania.mecania.domain.model.Vehiculo;
import com.kavanamecania.mecania.infrastructure.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehiculoServiceTest {

    private static final Long USUARIO = 1L;

    @Mock VehiculoRepository repository;
    @InjectMocks VehiculoService service;

    private VehiculoRequest req;

    @BeforeEach
    void setUp() {
        req = new VehiculoRequest("Toyota", "Corolla", 2018,
                Combustible.GASOLINA, 80000L, "1234ABC");
    }

    @Test
    void findById_existente_devuelve_vehiculo() {
        Vehiculo v = Vehiculo.builder().id(10L).usuarioId(USUARIO)
                .marca("Toyota").modelo("Corolla").anio(2018)
                .combustible(Combustible.GASOLINA).build();
        when(repository.findByIdAndUsuarioId(10L, USUARIO)).thenReturn(Optional.of(v));

        Vehiculo result = service.findById(10L, USUARIO);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getMarca()).isEqualTo("Toyota");
    }

    @Test
    void findById_inexistente_lanza_VehiculoNotFoundException() {
        when(repository.findByIdAndUsuarioId(99L, USUARIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L, USUARIO))
                .isInstanceOf(VehiculoNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_duplicado_lanza_VehiculoDuplicadoException() {
        when(repository.existsByUsuarioIdAndMarcaAndModeloAndAnio(
                USUARIO, "Toyota", "Corolla", 2018)).thenReturn(true);

        assertThatThrownBy(() -> service.create(req, USUARIO))
                .isInstanceOf(VehiculoDuplicadoException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void create_valido_persiste_y_devuelve_vehiculo() {
        when(repository.existsByUsuarioIdAndMarcaAndModeloAndAnio(
                USUARIO, "Toyota", "Corolla", 2018)).thenReturn(false);
        when(repository.save(any(Vehiculo.class))).thenAnswer(inv -> {
            Vehiculo arg = inv.getArgument(0);
            arg.setId(42L);
            return arg;
        });

        Vehiculo result = service.create(req, USUARIO);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getMarca()).isEqualTo("Toyota");
        verify(repository).save(any(Vehiculo.class));
    }

    @Test
    void delete_existente_elimina() {
        when(repository.existsByIdAndUsuarioId(10L, USUARIO)).thenReturn(true);

        service.delete(10L, USUARIO);

        verify(repository).deleteById(10L);
    }

    @Test
    void delete_inexistente_lanza_excepcion() {
        when(repository.existsByIdAndUsuarioId(99L, USUARIO)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L, USUARIO))
                .isInstanceOf(VehiculoNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void findByUsuarioId_devuelve_lista() {
        when(repository.findByUsuarioId(USUARIO)).thenReturn(List.of(
                Vehiculo.builder().id(1L).usuarioId(USUARIO)
                        .marca("A").modelo("B").anio(2020)
                        .combustible(Combustible.DIESEL).build()));

        List<Vehiculo> result = service.findByUsuarioId(USUARIO);

        assertThat(result).hasSize(1);
    }
}
