// RiskChain.java  (no es entidad: agrupa los cuatro eslabones de una cadena)
package org.example.model;

public record RiskChain(Hazard hazard, SequenceOfEvents sequence,
                        HazardousSituation situation, Harm harm) {
}