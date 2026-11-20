#!/usr/bin/env python3
"""
Genera las tablas de propiedades termodinamicas para el diagrama de Mollier.

Es una herramienta de DESARROLLO: corre una sola vez y produce
app/src/main/assets/mollier_data.js. La app no depende de CoolProp en runtime.

Uso:
    pip install CoolProp
    python3 tools/mollier/generar_datos.py

Por que CoolProp y no el Helmholtz EOS en JS:
    las ecuaciones de estado de Helmholtz son enormes y fragiles de portar a JS.
    Congelar tablas calculadas con CoolProp mantiene el motor JS simple,
    exacto (0.1 K en T_sat) y completamente offline.

Estructura de cada refrigerante:
    presion : columna de presiones [kPa], espaciado log-uniforme
    sat     : en cada columna -> t, hf, hg, sf, sg (en la saturacion)
    sup     : vapor supercalentado, por columna y por temperatura absoluta
    liq     : liquido comprimido, por columna y por temperatura absoluta
"""

import json
import math
import os
import sys

try:
    from CoolProp.CoolProp import PropsSI
except ImportError:
    print("Falta CoolProp. Instalalo con: pip install CoolProp", file=sys.stderr)
    sys.exit(1)

# Refrigerantes soportados por el modulo.
FLUIDS = [
    ("R410A", "R-410A"),
    ("R134a", "R-134a"),
    ("R32", "R-32"),
    ("R290", "R-290"),
]

# Resolucion de las tablas.
N_PRESION = 40          # columnas de presion
N_SUPERHEAT = 25        # puntos de temperatura en el vapor supercalentado
N_SUBCOOL = 9           # puntos de temperatura en el liquido comprimido
SUBCOOL_MAX_K = 30.0    # DeltaT maximo bajo la saturacion
T_MIN_C = -50.0         # limite inferior del rango de temperatura
T_MAX_C = 130.0         # limite superior del rango de temperatura (cubre T descarga)
P_MIN_KPA = 40.0        # limite inferior del rango de presion

SALIDA = os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
    "app", "src", "main", "assets", "mollier_data.js"
)


def kelvin(celsius):
    return celsius + 273.15


def interpolar_log(x, xs, ys):
    """Interpola linealmente en escala logaritmica de x."""
    if x <= xs[0]:
        return ys[0]
    if x >= xs[-1]:
        return ys[-1]
    lo, hi = 0, len(xs) - 1
    while hi - lo > 1:
        mid = (lo + hi) // 2
        if xs[mid] <= x:
            lo = mid
        else:
            hi = mid
    frac = (math.log(x) - math.log(xs[lo])) / (math.log(xs[hi]) - math.log(xs[lo]))
    return ys[lo] + frac * (ys[hi] - ys[lo])


def construir(fluid, etiqueta):
    molar = PropsSI("M", fluid)                 # kg/mol
    tc = PropsSI("Tcrit", fluid) - 273.15       # °C
    pc = PropsSI("Pcrit", fluid) / 1000.0       # kPa

    t_max = T_MAX_C

    # Presion de saturacion al limite inferior de temperatura (con margen para
    # que la primera columna tenga un tramo de liquido comprimido util).
    p_low = PropsSI("P", "T", kelvin(T_MIN_C + 2.0), "Q", 0, fluid) / 1000.0
    p_low = max(P_MIN_KPA, p_low)

    # El rango superior de presion llega hasta casi la critica.
    p_high = pc * 0.999

    presion = []
    for i in range(N_PRESION):
        frac = i / (N_PRESION - 1)
        presion.append(p_low * (p_high / p_low) ** frac)

    sat = {"t": [], "hf": [], "hg": [], "sf": [], "sg": []}
    sup_h, sup_s = [], []
    liq_h, liq_s, liq_t0 = [], [], []

    for p_kpa in presion:
        p_pa = p_kpa * 1000.0
        t_sat = PropsSI("T", "P", p_pa, "Q", 0, fluid) - 273.15

        sat["t"].append(round(t_sat, 4))
        sat["hf"].append(round(PropsSI("H", "P", p_pa, "Q", 0, fluid) / 1000.0, 3))
        sat["hg"].append(round(PropsSI("H", "P", p_pa, "Q", 1, fluid) / 1000.0, 3))
        sat["sf"].append(round(PropsSI("S", "P", p_pa, "Q", 0, fluid) / 1000.0, 5))
        sat["sg"].append(round(PropsSI("S", "P", p_pa, "Q", 1, fluid) / 1000.0, 5))

        # --- Vapor supercalentado: de T_sat hasta t_max, en temperatura absoluta ---
        columna_h, columna_s = [], []
        for j in range(N_SUPERHEAT):
            t_abs = t_sat + (t_max - t_sat) * j / (N_SUPERHEAT - 1)
            if t_abs - t_sat <= 0.01:
                h = PropsSI("H", "P", p_pa, "Q", 1, fluid)
                s = PropsSI("S", "P", p_pa, "Q", 1, fluid)
            else:
                h = PropsSI("H", "T", kelvin(t_abs), "P", p_pa, fluid)
                s = PropsSI("S", "T", kelvin(t_abs), "P", p_pa, fluid)
            columna_h.append(round(h / 1000.0, 3))
            columna_s.append(round(s / 1000.0, 5))
        sup_h.append(columna_h)
        sup_s.append(columna_s)

        # --- Liquido comprimido: de max(T_min, T_sat - 30) hasta T_sat ---
        t0 = min(t_sat, max(T_MIN_C, t_sat - SUBCOOL_MAX_K))
        columna_h, columna_s = [], []
        for j in range(N_SUBCOOL):
            t_abs = t0 + (t_sat - t0) * j / (N_SUBCOOL - 1)
            if t_sat - t_abs <= 0.01:
                h = PropsSI("H", "P", p_pa, "Q", 0, fluid)
                s = PropsSI("S", "P", p_pa, "Q", 0, fluid)
            else:
                h = PropsSI("H", "T", kelvin(t_abs), "P", p_pa, fluid)
                s = PropsSI("S", "T", kelvin(t_abs), "P", p_pa, fluid)
            columna_h.append(round(h / 1000.0, 3))
            columna_s.append(round(s / 1000.0, 5))
        liq_h.append(columna_h)
        liq_s.append(columna_s)
        liq_t0.append(round(t0, 4))

    return {
        "etiqueta": etiqueta,
        "molar": round(molar, 6),
        "tc": round(tc, 3),
        "pc": round(pc, 2),
        "tMin": T_MIN_C,
        "tMax": round(t_max, 4),
        "pMin": round(presion[0], 3),
        "pMax": round(presion[-1], 3),
        "presion": [round(p, 4) for p in presion],
        "sat": sat,
        "sup": {"h": sup_h, "s": sup_s},
        "liq": {"h": liq_h, "s": liq_s, "t0": liq_t0},
    }


def caso_de_prueba(datos):
    """Ejemplo precargado de la app: R-410A, 800/2600 kPa, 12/40 C."""
    fluid = "R410A"
    molar = PropsSI("M", fluid)
    p_low, p_high = 800.0, 2600.0
    t_suction, t_liquid = 12.0, 40.0

    t_sat_low = interpolar_log(p_low, datos[fluid]["presion"], datos[fluid]["sat"]["t"])
    t_sat_high = interpolar_log(p_high, datos[fluid]["presion"], datos[fluid]["sat"]["t"])

    h1 = PropsSI("H", "T", kelvin(t_suction), "P", p_low * 1000, fluid) / 1000.0
    s1 = PropsSI("S", "T", kelvin(t_suction), "P", p_low * 1000, fluid) / 1000.0
    h2 = PropsSI("H", "P", p_high * 1000, "S", s1 * 1000.0, fluid) / 1000.0
    t2 = PropsSI("T", "P", p_high * 1000, "S", s1 * 1000.0, fluid) - 273.15
    h3 = PropsSI("H", "T", kelvin(t_liquid), "P", p_high * 1000, fluid) / 1000.0
    h4 = h3

    hf_low = PropsSI("H", "P", p_low * 1000, "Q", 0, fluid) / 1000.0
    hg_low = PropsSI("H", "P", p_low * 1000, "Q", 1, fluid) / 1000.0
    x4 = (h4 - hf_low) / (hg_low - hf_low)
    cop = (h1 - h4) / (h2 - h1)

    return {
        "refrigerante": "R410A",
        "pBaja": p_low,
        "pAlta": p_high,
        "tSuccion": t_suction,
        "tLiquido": t_liquid,
        "tSatBaja": round(t_sat_low, 3),
        "tSatAlta": round(t_sat_high, 3),
        "sh": round(t_suction - t_sat_low, 3),
        "sc": round(t_sat_high - t_liquid, 3),
        "h1": round(h1, 3),
        "s1": round(s1, 5),
        "h2": round(h2, 3),
        "t2": round(t2, 3),
        "h3": round(h3, 3),
        "h4": round(h4, 3),
        "x4": round(x4, 5),
        "cop": round(cop, 4),
    }


def main():
    datos = {}
    for fluid, etiqueta in FLUIDS:
        print(f"Generando {etiqueta} ({fluid})...")
        datos[fluid] = construir(fluid, etiqueta)

    caso = caso_de_prueba(datos)

    cuerpo = json.dumps(datos, separators=(",", ":"), ensure_ascii=False)
    with open(SALIDA, "w", encoding="utf-8") as f:
        f.write("// Generado por tools/mollier/generar_datos.py - NO EDITAR A MANO.\n")
        f.write("// Fuente: CoolProp 8.0.0 (EOS de Span y Wagner / Lemmon para cada fluido).\n")
        f.write("// Unidades: presion kPa, temperatura C, entalpia kJ/kg, entropia kJ/kg.K.\n")
        f.write("window.MOLLIER_DATA = " + cuerpo + ";\n")
        f.write("window.MOLLIER_CHECK = " + json.dumps(caso, separators=(",", ":")) + ";\n")

    tam = os.path.getsize(SALIDA)
    print(f"\nEscrito {SALIDA} ({tam / 1024:.1f} KB)")
    print("\nCaso de prueba (R-410A, 800/2600 kPa, 12/40 C):")
    for clave, valor in caso.items():
        print(f"  {clave}: {valor}")


if __name__ == "__main__":
    main()
