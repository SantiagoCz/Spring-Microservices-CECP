package com.santiagocz.employees_service.domain.enums;

import lombok.Getter;

@Getter
public enum Delegation {

    APOSTOLES                ("Apóstoles",                  Locality.APOSTOLES),
    ELDORADO                 ("Eldorado",                   Locality.ELDORADO),
    JARDIN_AMERICA           ("Jardín América",             Locality.JARDIN_AMERICA),
    ALEM                     ("Leandro N. Alem",            Locality.ALEM),
    POSADAS_POLICONSULTORIOS ("Posadas - Policonsultorios", Locality.POSADAS),
    POSADAS_ECSALUD          ("Posadas - Ecsalud",          Locality.POSADAS);

    private final String displayName;
    private final Locality locality;

    Delegation(String displayName, Locality locality) {
        this.displayName = displayName;
        this.locality = locality;
    }
}