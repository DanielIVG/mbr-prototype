// FmeaView.java  (no es entidad)
package org.example.model;

import org.example.rpn.RpnResult;

/** Un análisis FMEA con su fila vigente y el resultado explicado (RNF-06). */
public record FmeaView(FmeaAnalysis analysis, RpnFactor factor, RpnResult result) {
}