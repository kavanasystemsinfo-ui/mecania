package com.kavanamecania.mecania.domain.embedding;

import java.util.Arrays;

/**
 * Vector de embedding de un texto.
 * Es un value object: equals por contenido, inmutable.
 */
public record Embedding(double[] valores) {

    public Embedding {
        if (valores == null || valores.length == 0) {
            throw new IllegalArgumentException("Embedding no puede ser null ni vacío");
        }
        // Clonamos para inmutabilidad defensiva
        valores = valores.clone();
    }

    public int dimension() {
        return valores.length;
    }

    /**
     * Representación textual en formato pgvector: "[0.1,0.2,...]"
     */
    public String toPgVectorString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(valores[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    // equals/hashCode manuales: el auto-generado del record compara double[] por referencia.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Embedding other)) return false;
        return Arrays.equals(this.valores, other.valores);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(valores);
    }
}