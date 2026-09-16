package com.kavanamecania.mecania.domain.alertas;

import com.kavanamecania.mecania.domain.model.Alerta;
import com.kavanamecania.mecania.domain.model.Repetitividad;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Revisa qué alertas están vencidas y deja el estado preparado para la próxima
 * revisión. Es lógica de dominio pura (sin Spring ni base de datos): recibe la
 * lista de alertas y el día de hoy, y devuelve las que hay que avisar.
 *
 * <p>Reglas:</p>
 * <ul>
 *   <li>Una alerta {@code UNICA} vencida se devuelve y se desactiva (no se
 *       vuelve a avisar).</li>
 *   <li>Una alerta repetitiva vencida se devuelve y su fecha avanza hasta
 *       quedar en el futuro, saltando las ocurrencias ya pasadas.</li>
 *   <li>Una alerta futura o inactiva no se devuelve ni se toca.</li>
 * </ul>
 */
public class RevisorAlertas {

    /**
     * Devuelve las alertas vencidas hoy y muta las que toca (desactiva las
     * únicas, avanza las repetitivas).
     */
    public List<Alerta> vencidas(List<Alerta> alertas, LocalDate hoy) {
        List<Alerta> vencidas = new ArrayList<>();
        for (Alerta alerta : alertas) {
            if (!alerta.estaVencida(hoy)) {
                continue;
            }
            vencidas.add(alerta);
            if (alerta.getRepetitividad() == Repetitividad.UNICA) {
                alerta.setActiva(false);
            } else {
                while (alerta.estaVencida(hoy)) {
                    alerta.avanzar();
                }
            }
        }
        return vencidas;
    }
}
