package com.kavanamecania.mecania.application;

import com.kavanamecania.mecania.application.dto.AlertaRequest;
import com.kavanamecania.mecania.domain.alertas.RevisorAlertas;
import com.kavanamecania.mecania.domain.exception.AlertaNotFoundException;
import com.kavanamecania.mecania.domain.exception.VehiculoNotFoundException;
import com.kavanamecania.mecania.domain.model.Alerta;
import com.kavanamecania.mecania.domain.model.Vehiculo;
import com.kavanamecania.mecania.infrastructure.repository.AlertaRepository;
import com.kavanamecania.mecania.infrastructure.repository.VehiculoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * CRUD y revisión de vencimiento de alertas de mantenimiento.
 *
 * <p>La lógica de vencimiento/avance vive en {@link RevisorAlertas}; este
 * servicio se encarga de cargar las alertas del vehículo, ejecutar el revisor
 * y persistir las mutaciones resultantes.</p>
 */
@Service
public class AlertaService {

    private static final Logger log = LoggerFactory.getLogger(AlertaService.class);

    private final AlertaRepository alertaRepository;
    private final VehiculoRepository vehiculoRepository;

    public AlertaService(AlertaRepository alertaRepository, VehiculoRepository vehiculoRepository) {
        this.alertaRepository = alertaRepository;
        this.vehiculoRepository = vehiculoRepository;
    }

    @Transactional(readOnly = true)
    public List<Alerta> listar(Long vehiculoId) {
        return alertaRepository.findByVehiculoId(vehiculoId);
    }

    @Transactional
    public Alerta crear(Long vehiculoId, AlertaRequest req) {
        Vehiculo vehiculo = vehiculoDe(vehiculoId);
        Alerta alerta = Alerta.builder()
                .vehiculo(vehiculo)
                .tipo(req.tipo())
                .descripcion(req.descripcion())
                .fecha(req.fecha())
                .repetitividad(req.repetitividad())
                .activa(true)
                .build();
        return alertaRepository.save(alerta);
    }

    @Transactional
    public Alerta actualizar(Long vehiculoId, Long alertaId, AlertaRequest req) {
        Alerta alerta = alertaDe(vehiculoId, alertaId);
        alerta.setTipo(req.tipo());
        alerta.setDescripcion(req.descripcion());
        alerta.setFecha(req.fecha());
        alerta.setRepetitividad(req.repetitividad());
        return alertaRepository.save(alerta);
    }

    @Transactional
    public void eliminar(Long vehiculoId, Long alertaId) {
        Alerta alerta = alertaDe(vehiculoId, alertaId);
        alertaRepository.delete(alerta);
    }

    @Transactional
    public List<Alerta> vencidas(Long vehiculoId) {
        List<Alerta> resultado = new RevisorAlertas()
                .vencidas(alertaRepository.findByVehiculoId(vehiculoId), LocalDate.now());
        alertaRepository.saveAll(resultado);
        for (Alerta alerta : resultado) {
            log.info("Alerta {} vencida para el vehículo {}: {}",
                    alerta.getId(), vehiculoId, alerta.getDescripcion());
        }
        return resultado;
    }

    private Vehiculo vehiculoDe(Long vehiculoId) {
        return vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new VehiculoNotFoundException(vehiculoId));
    }

    private Alerta alertaDe(Long vehiculoId, Long alertaId) {
        return alertaRepository.findByVehiculoIdAndId(vehiculoId, alertaId)
                .orElseThrow(() -> new AlertaNotFoundException(vehiculoId, alertaId));
    }
}
