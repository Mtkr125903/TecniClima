(function () {
  'use strict';

  var DATOS = window.MOLLIER_DATA || {};
  var TEMA = {
    fondo: '#F1F5FB',
    texto: '#0B132B',
    secundario: '#55637A',
    grilla: '#DDE5EF',
    domo: '#0077B6',
    isobara: '#9AA9BD',
    isoterma: '#B36B00',
    ciclo: '#C62828',
    punto: '#1E7E34'
  };
  var FIGURA = null;
  var FLUID_ACTUAL = null;
  var RES_ACTUAL = null;

  function falla(codigo) {
    var e = new Error(codigo);
    e.codigo = codigo;
    return e;
  }

  function datosDe(fluid) {
    var d = DATOS[fluid];
    if (!d) throw falla('REFRIGERANTE_INVALIDO');
    return d;
  }

  function limite(v, min, max) {
    if (v < min) return min;
    if (v > max) return max;
    return v;
  }

  function columnas(d, p) {
    var ps = d.presion;
    var n = ps.length;
    if (p <= ps[0]) return { i: 0, f: 0 };
    if (p >= ps[n - 1]) return { i: n - 2, f: 1 };
    var lo = 0;
    var hi = n - 1;
    while (hi - lo > 1) {
      var mid = (lo + hi) >> 1;
      if (ps[mid] <= p) lo = mid; else hi = mid;
    }
    var f = (Math.log(p) - Math.log(ps[lo])) / (Math.log(ps[hi]) - Math.log(ps[lo]));
    return { i: lo, f: f };
  }

  function interpolar(d, serie, p) {
    var c = columnas(d, p);
    return serie[c.i] + c.f * (serie[c.i + 1] - serie[c.i]);
  }

  function tSat(d, p) {
    return interpolar(d, d.sat.t, p);
  }

  function hf(d, p) {
    return interpolar(d, d.sat.hf, p);
  }

  function hg(d, p) {
    return interpolar(d, d.sat.hg, p);
  }

  function sf(d, p) {
    return interpolar(d, d.sat.sf, p);
  }

  function sg(d, p) {
    return interpolar(d, d.sat.sg, p);
  }

  function interpolarSerie(serie, posicion) {
    var n = serie.length;
    if (n <= 1) return serie[0];
    var u = limite(posicion, 0, n - 1);
    var j = Math.floor(u);
    if (j >= n - 1) return serie[n - 1];
    return serie[j] + (u - j) * (serie[j + 1] - serie[j]);
  }

  function superheat(d, p, t) {
    var ts = tSat(d, p);
    if (t <= ts) {
      return { h: hg(d, p), s: sg(d, p), t: ts, saturado: true };
    }
    var c = columnas(d, p);
    var tMax = d.tMax;
    var p1 = interpolarSerie(d.sup.h[c.i], (t - d.sat.t[c.i]) * (d.sup.h[c.i].length - 1) / (tMax - d.sat.t[c.i]));
    var p2 = interpolarSerie(d.sup.h[c.i + 1], (t - d.sat.t[c.i + 1]) * (d.sup.h[c.i + 1].length - 1) / (tMax - d.sat.t[c.i + 1]));
    var s1 = interpolarSerie(d.sup.s[c.i], (t - d.sat.t[c.i]) * (d.sup.s[c.i].length - 1) / (tMax - d.sat.t[c.i]));
    var s2 = interpolarSerie(d.sup.s[c.i + 1], (t - d.sat.t[c.i + 1]) * (d.sup.s[c.i + 1].length - 1) / (tMax - d.sat.t[c.i + 1]));
    return {
      h: p1 + c.f * (p2 - p1),
      s: s1 + c.f * (s2 - s1),
      t: t,
      saturado: false
    };
  }

  function liquid(d, p, t) {
    var ts = tSat(d, p);
    if (t >= ts) {
      return { h: hf(d, p), s: sf(d, p), t: ts };
    }
    var c = columnas(d, p);
    var liq1 = d.liq.h[c.i];
    var liq2 = d.liq.h[c.i + 1];
    var pos1 = (t - d.liq.t0[c.i]) * (liq1.length - 1) / (d.sat.t[c.i] - d.liq.t0[c.i]);
    var pos2 = (t - d.liq.t0[c.i + 1]) * (liq2.length - 1) / (d.sat.t[c.i + 1] - d.liq.t0[c.i + 1]);
    var h1 = interpolarSerie(liq1, pos1);
    var h2 = interpolarSerie(liq2, pos2);
    var s1 = interpolarSerie(d.liq.s[c.i], pos1);
    var s2 = interpolarSerie(d.liq.s[c.i + 1], pos2);
    return {
      h: h1 + c.f * (h2 - h1),
      s: s1 + c.f * (s2 - s1),
      t: t
    };
  }

  function hDesdeS(d, p, sObjetivo) {
    var ts = tSat(d, p);
    var sMin = sg(d, p);
    if (sObjetivo <= sMin) {
      return { h: hg(d, p), t: ts };
    }
    var estadoMax = superheat(d, p, d.tMax);
    if (sObjetivo >= estadoMax.s) {
      throw falla('T2_FUERA_DE_RANGO');
    }
    var lo = ts;
    var hi = d.tMax;
    for (var k = 0; k < 60; k++) {
      var mid = (lo + hi) / 2;
      if (superheat(d, p, mid).s < sObjetivo) lo = mid; else hi = mid;
    }
    var t = (lo + hi) / 2;
    var estado = superheat(d, p, t);
    return { h: estado.h, t: t };
  }

  function calidad(d, p, h) {
    var a = hf(d, p);
    var b = hg(d, p);
    if (b - a <= 0) return 0;
    return limite((h - a) / (b - a), 0, 1);
  }

  function calcular(fluid, pBaja, pAlta, tSuccion, tLiquido) {
    var d = datosDe(fluid);

    if (!esNumero(pBaja) || pBaja <= 0) throw falla('P_BAJA_INVALIDA');
    if (!esNumero(pAlta) || pAlta <= 0) throw falla('P_ALTA_INVALIDA');
    if (!esNumero(tSuccion)) throw falla('T_SUCION_INVALIDA');
    if (!esNumero(tLiquido)) throw falla('T_LIQUIDO_INVALIDA');
    if (pBaja >= pAlta) throw falla('P_ALTA_MENOR');
    if (pBaja < d.pMin || pAlta > d.pc) throw falla('P_FUERA_DE_RANGO');
    if (tSuccion < d.tMin || tSuccion > d.tMax) throw falla('T_FUERA_DE_RANGO');
    if (tLiquido < d.tMin || tLiquido > d.tMax) throw falla('T_FUERA_DE_RANGO');

    var tSatBaja = tSat(d, pBaja);
    var tSatAlta = tSat(d, pAlta);
    var sh = tSuccion - tSatBaja;
    var sc = tSatAlta - tLiquido;
    if (sh < 0) throw falla('SH_NEGATIVO');
    if (sc < 0) throw falla('SC_NEGATIVO');

    var estado1 = superheat(d, pBaja, tSuccion);
    var estado2 = hDesdeS(d, pAlta, estado1.s);
    var estado3 = liquid(d, pAlta, tLiquido);
    var h4 = estado3.h;
    var x4 = calidad(d, pBaja, h4);
    var q1 = estado1.h - h4;
    var q2 = estado2.h - estado3.h;
    var trabajo = estado2.h - estado1.h;
    var cop = trabajo > 0 ? q1 / trabajo : 0;

    return {
      refrigerante: fluid,
      etiqueta: d.etiqueta,
      pBaja: pBaja,
      pAlta: pAlta,
      tSatBaja: tSatBaja,
      tSatAlta: tSatAlta,
      sh: sh,
      sc: sc,
      h1: estado1.h,
      s1: estado1.s,
      t1: estado1.t,
      h2: estado2.h,
      s2: estado1.s,
      t2: estado2.t,
      h3: estado3.h,
      s3: estado3.s,
      t3: estado3.t,
      h4: h4,
      x4: x4,
      q1: q1,
      q2: q2,
      cop: cop,
      razonCompresion: pAlta / pBaja
    };
  }

  function esNumero(v) {
    return typeof v === 'number' && isFinite(v);
  }

  function presionesIsobara(d) {
    var candidatas = [50, 75, 100, 150, 200, 300, 400, 500, 700, 1000, 1200,
      1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000];
    var salida = [];
    for (var i = 0; i < candidatas.length; i++) {
      var p = candidatas[i];
      if (p >= d.pMin && p <= d.pMax) salida.push(p);
    }
    return salida;
  }

  function puntosIsobara(d, p) {
    var puntos = [];
    var ts = tSat(d, p);
    var t0 = Math.max(d.tMin, ts - 30);
    var nLiq = 9;
    for (var j = 0; j < nLiq; j++) {
      var t = t0 + (ts - t0) * j / (nLiq - 1);
      puntos.push([liquid(d, p, t).h, p]);
    }
    var nVap = 21;
    for (var k = 0; k < nVap; k++) {
      var tv = ts + (d.tMax - ts) * k / (nVap - 1);
      puntos.push([superheat(d, p, tv).h, p]);
    }
    return puntos;
  }

  function puntosIsoterma(d, t) {
    var puntos = [];
    for (var i = 0; i < d.presion.length; i++) {
      var p = d.presion[i];
      if (t < d.sat.t[i]) {
        if (t >= d.liq.t0[i] - 0.01) puntos.push([liquid(d, p, t).h, p]);
        continue;
      }
      if (t > d.tMax) break;
      puntos.push([superheat(d, p, t).h, p]);
    }
    return puntos;
  }

  function ticksPresion(d) {
    var candidatas = [50, 100, 150, 200, 300, 400, 500, 700, 1000, 1500, 2000, 3000, 4000, 5000];
    var valores = [];
    var textos = [];
    for (var i = 0; i < candidatas.length; i++) {
      var p = candidatas[i];
      if (p < d.pMin || p > d.pMax) continue;
      valores.push(p);
      textos.push(p >= 1000 ? (p / 1000) + 'k' : String(p));
    }
    return { valores: valores, textos: textos };
  }

  function construirFigura(d, res) {
    var trazas = [];

    trazas.push({
      x: d.sat.hf,
      y: d.presion,
      type: 'scatter',
      mode: 'lines',
      line: { color: TEMA.domo, width: 2 },
      hoverinfo: 'skip',
      showlegend: false,
      name: 'Saturacion'
    });
    trazas.push({
      x: d.sat.hg,
      y: d.presion,
      type: 'scatter',
      mode: 'lines',
      line: { color: TEMA.domo, width: 2 },
      hoverinfo: 'skip',
      showlegend: false,
      name: 'Saturacion'
    });

    var isobaras = presionesIsobara(d);
    for (var i = 0; i < isobaras.length; i++) {
      var puntos = puntosIsobara(d, isobaras[i]);
      var xs = [];
      var ys = [];
      for (var j = 0; j < puntos.length; j++) {
        xs.push(puntos[j][0]);
        ys.push(puntos[j][1]);
      }
      trazas.push({
        x: xs,
        y: ys,
        type: 'scatter',
        mode: 'lines',
        line: { color: TEMA.isobara, width: 1 },
        opacity: 0.55,
        hoverinfo: 'skip',
        showlegend: false,
        name: 'p = ' + isobaras[i] + ' kPa'
      });
    }

    var isotermas = [];
    for (var t = -40; t <= 80; t += 10) isotermas.push(t);
    for (var k = 0; k < isotermas.length; k++) {
      var puntosT = puntosIsoterma(d, isotermas[k]);
      if (puntosT.length < 2) continue;
      var xt = [];
      var yt = [];
      for (var m = 0; m < puntosT.length; m++) {
        xt.push(puntosT[m][0]);
        yt.push(puntosT[m][1]);
      }
      trazas.push({
        x: xt,
        y: yt,
        type: 'scatter',
        mode: 'lines',
        line: { color: TEMA.isoterma, width: 1, dash: 'dot' },
        opacity: 0.6,
        hoverinfo: 'skip',
        showlegend: false,
        name: isotermas[k] + ' C'
      });
    }

    trazas.push({
      x: [res.h1, res.h2, res.h3, res.h4, res.h1],
      y: [res.pBaja, res.pAlta, res.pAlta, res.pBaja, res.pBaja],
      type: 'scatter',
      mode: 'lines+markers',
      line: { color: TEMA.ciclo, width: 3 },
      marker: { color: TEMA.punto, size: 10, line: { color: TEMA.ciclo, width: 2 } },
      text: ['1 - Salida evaporador', '2 - Descarga compresor', '3 - Salida condensador', '4 - SalidaExpansion'],
      hovertemplate: '%{text}<br>h = %{x:.1f} kJ/kg<br>p = %{y:.0f} kPa<extra></extra>',
      showlegend: false,
      name: 'Ciclo'
    });

    var hMin = Math.min.apply(null, d.sat.hf) - 25;
    var hMax = Math.max.apply(null, d.sat.hg) + 25;
    var ticks = ticksPresion(d);

    return {
      data: trazas,
      layout: {
        margin: { l: 66, r: 12, t: 12, b: 46 },
        paper_bgcolor: TEMA.fondo,
        plot_bgcolor: TEMA.fondo,
        font: { color: TEMA.texto, size: 11, family: 'sans-serif' },
        showlegend: false,
        hovermode: 'closest',
        dragmode: 'pan',
        xaxis: {
          title: { text: 'h (kJ/kg)', font: { size: 11 } },
          gridcolor: TEMA.grilla,
          zeroline: false,
          range: [hMin, hMax],
          automargin: true,
          fixedrange: false
        },
        yaxis: {
          title: { text: 'p (kPa)', font: { size: 11 } },
          type: 'log',
          gridcolor: TEMA.grilla,
          zeroline: false,
          range: [Math.log(d.pMin) / Math.LN10, Math.log(d.pMax) / Math.LN10],
          tickmode: 'array',
          tickvals: ticks.valores,
          ticktext: ticks.textos,
          automargin: true,
          fixedrange: false
        }
      },
      config: {
        displayModeBar: false,
        responsive: true,
        scrollZoom: true,
        doubleClick: 'reset'
      }
    };
  }

  function pintar(d, res) {
    if (typeof Plotly === 'undefined') return;
    var figura = construirFigura(d, res);
    FIGURA = figura;
    FLUID_ACTUAL = d;
    RES_ACTUAL = res;
    Plotly.react('grafico', figura.data, figura.layout, figura.config);
  }

  function notificar(res) {
    if (typeof Android === 'undefined' || !Android) return;
    if (Android.onCycleCalculated) Android.onCycleCalculated(JSON.stringify(res));
    if (Android.onResultsCalculated) {
      Android.onResultsCalculated(res.sh, res.sc, res.tSatBaja, res.tSatAlta);
    }
  }

  window.mollierEngine = {
    datos: DATOS,
    tSat: tSat,
    hf: hf,
    hg: hg,
    superheat: superheat,
    liquid: liquid,
    hDesdeS: hDesdeS,
    calidad: calidad,
    calcular: calcular,
    construirFigura: construirFigura,
    pintar: pintar
  };

  window.aplicarTema = function (tema) {
    if (!tema) return;
    for (var clave in tema) {
      if (Object.prototype.hasOwnProperty.call(tema, clave)) TEMA[clave] = tema[clave];
    }
    if (FIGURA && FLUID_ACTUAL && RES_ACTUAL) {
      var nueva = construirFigura(FLUID_ACTUAL, RES_ACTUAL);
      FIGURA = nueva;
      if (typeof Plotly !== 'undefined') {
        Plotly.react('grafico', nueva.data, nueva.layout, nueva.config);
      }
    }
    if (typeof document !== 'undefined' && document.body) {
      document.body.style.background = TEMA.fondo;
    }
  };

  window.evaluateCycle = function (params) {
    try {
      var d = datosDe(params.fluid);
      var res = calcular(params.fluid, params.pBaja, params.pAlta, params.tSuccion, params.tLiquido);
      var figura = construirFigura(d, res);
      FIGURA = figura;
      FLUID_ACTUAL = d;
      RES_ACTUAL = res;
      if (typeof Plotly !== 'undefined') {
        Plotly.react('grafico', figura.data, figura.layout, figura.config);
      }
      notificar(res);
      return res;
    } catch (e) {
      if (typeof Android !== 'undefined' && Android && Android.onError) {
        Android.onError(e && e.codigo ? e.codigo : 'GENERICO');
      }
      return null;
    }
  };
})();
