package com.kavanamecania.mecania.domain.busqueda;

/**
 * Resultado de una búsqueda de manuales en la web.
 *
 * @param titulo  título que muestra el buscador
 * @param url     URL real de la fuente (nunca la del intermediario de búsqueda)
 * @param snippet fragmento descriptivo devuelto por el buscador
 * @param fuente  host de la URL (para que el usuario vea de dónde sale)
 * @param pdf     true si la URL apunta a un PDF (pista, no garantía)
 */
public record CandidatoManual(String titulo, String url, String snippet, String fuente, boolean pdf) {
}
