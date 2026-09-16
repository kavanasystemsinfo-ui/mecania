package com.kavanamecania.mecania.infrastructure.descarga;

import com.kavanamecania.mecania.domain.descarga.DescargaException;
import com.kavanamecania.mecania.domain.descarga.MotivoDescarga;
import com.kavanamecania.mecania.domain.descarga.ValidadorDestinoDescarga;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Rechaza destinos que no sean públicos para impedir SSRF: la URL la elige el
 * usuario, así que sin esta comprobación podría apuntar a
 * {@code 169.254.169.254} (metadatos de la nube), a la base de datos local o a
 * la propia API.
 *
 * <p>Comprueba el host por nombre y las direcciones a las que resuelve: si el
 * DNS no se puede verificar, se falla en cerrado (se rechaza).</p>
 */
public class ValidadorHostsPublicos implements ValidadorDestinoDescarga {

    private static final Set<String> NOMBRES_BLOQUEADOS = Set.of(
            "localhost",
            "metadata",
            "metadata.google.internal",
            "instance-data");

    @Override
    public void validar(URI destino) {
        String host = destino.getHost();
        if (host == null || host.isBlank()) {
            throw new DescargaException(MotivoDescarga.HOST_NO_PERMITIDO,
                    "La URL no indica un host válido: " + destino);
        }

        String normalizado = host.toLowerCase();
        if (normalizado.endsWith(".localhost") || NOMBRES_BLOQUEADOS.contains(normalizado)) {
            throw new DescargaException(MotivoDescarga.HOST_NO_PERMITIDO,
                    "Host no permitido (local o de metadatos): " + host);
        }

        InetAddress[] direcciones;
        try {
            direcciones = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new DescargaException(MotivoDescarga.HOST_NO_PERMITIDO,
                    "No se pudo verificar que el host sea público (DNS): " + host, e);
        }

        List<InetAddress> privadas = Arrays.stream(direcciones)
                .filter(ValidadorHostsPublicos::esDireccionNoPublica)
                .toList();
        if (!privadas.isEmpty()) {
            throw new DescargaException(MotivoDescarga.HOST_NO_PERMITIDO,
                    "Host no permitido (dirección interna, privada o de metadatos): " + host
                            + " → " + privadas.get(0).getHostAddress());
        }
    }

    private static boolean esDireccionNoPublica(InetAddress direccion) {
        if (direccion.isLoopbackAddress() || direccion.isAnyLocalAddress()
                || direccion.isLinkLocalAddress() || direccion.isSiteLocalAddress()
                || direccion.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = direccion.getAddress();
        if (bytes.length == 4) {
            int primer = bytes[0] & 0xFF;
            int segundo = bytes[1] & 0xFF;
            // 100.64.0.0/10 (CGNAT) y 192.0.0.0/24 (IETF protocol assignments).
            return (primer == 100 && segundo >= 64 && segundo <= 127)
                    || (primer == 192 && segundo == 0 && (bytes[2] & 0xFF) == 0);
        }
        // IPv6: fc00::/7 (unique local).
        return (bytes[0] & 0xFE) == 0xFC;
    }
}
