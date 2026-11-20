const fs = require('fs');
const path = require('path');
const vm = require('vm');

const ASSETS = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'assets');

const contexto = {
  console,
  Math,
  JSON,
  isFinite,
  Object,
  Array,
  String,
  Number,
  Error
};
contexto.window = contexto;
contexto.globalThis = contexto;

const avisos = {
  ciclo: null,
  resultados: null,
  error: null
};
contexto.Android = {
  onCycleCalculated: json => { avisos.ciclo = JSON.parse(json); },
  onResultsCalculated: (sh, sc, tsb, tsh) => {
    avisos.resultados = { sh, sc, tsb, tsh };
  },
  onError: msg => { avisos.error = msg; }
};

vm.createContext(contexto);
for (const archivo of ['mollier_data.js', 'mollier.js']) {
  const codigo = fs.readFileSync(path.join(ASSETS, archivo), 'utf8');
  vm.runInContext(codigo, contexto, { filename: archivo });
}

const CHECK = contexto.MOLLIER_CHECK;
const motor = contexto.mollierEngine;

let fallos = 0;
function comparar(nombre, obtenido, esperado, tolerancia) {
  const diferencia = Math.abs(obtenido - esperado);
  const ok = diferencia <= tolerancia;
  if (!ok) fallos++;
  console.log(
    `${ok ? 'OK  ' : 'FALLA'} ${nombre}: ${obtenido} (esperado ${esperado}, dif ${diferencia.toFixed(4)})`
  );
}

console.log('--- Caso de referencia (CoolProp) ---');
avisos.error = null;
avisos.ciclo = null;
const resultado = motor.calcular(
  CHECK.refrigerante,
  CHECK.pBaja,
  CHECK.pAlta,
  CHECK.tSuccion,
  CHECK.tLiquido
);
comparar('tSatBaja', resultado.tSatBaja.toFixed(3), CHECK.tSatBaja, 0.15);
comparar('tSatAlta', resultado.tSatAlta.toFixed(3), CHECK.tSatAlta, 0.15);
comparar('sh', resultado.sh.toFixed(3), CHECK.sh, 0.15);
comparar('sc', resultado.sc.toFixed(3), CHECK.sc, 0.15);
comparar('h1', resultado.h1.toFixed(3), CHECK.h1, 1.0);
comparar('s1', resultado.s1.toFixed(5), CHECK.s1, 0.01);
comparar('h2', resultado.h2.toFixed(3), CHECK.h2, 1.5);
comparar('t2', resultado.t2.toFixed(3), CHECK.t2, 1.0);
comparar('h3', resultado.h3.toFixed(3), CHECK.h3, 1.5);
comparar('h4', resultado.h4.toFixed(3), CHECK.h4, 1.5);
comparar('x4', resultado.x4.toFixed(5), CHECK.x4, 0.01);
comparar('cop', resultado.cop.toFixed(4), CHECK.cop, 0.06);

console.log('\n--- Contrato de callbacks ---');
avisos.error = null;
avisos.ciclo = null;
avisos.resultados = null;
contexto.evaluateCycle({
  fluid: CHECK.refrigerante,
  pBaja: CHECK.pBaja,
  pAlta: CHECK.pAlta,
  tSuccion: CHECK.tSuccion,
  tLiquido: CHECK.tLiquido
});
if (!avisos.ciclo) {
  console.log('FALLA onCycleCalculated no se invoco');
  fallos++;
} else {
  console.log('OK   onCycleCalculated recibido');
}
if (!avisos.resultados) {
  console.log('FALLA onResultsCalculated no se invoco');
  fallos++;
} else {
  const r = avisos.resultados;
  const ok = Math.abs(r.sh - CHECK.sh) < 0.15 && Math.abs(r.sc - CHECK.sc) < 0.15;
  if (!ok) fallos++;
  console.log(`${ok ? 'OK  ' : 'FALLA'} onResultsCalculated: sh=${r.sh} sc=${r.sc}`);
}

console.log('\n--- Errores esperados ---');
function esperarError(nombre, entradas, codigo) {
  avisos.error = null;
  contexto.evaluateCycle(entradas);
  const ok = avisos.error === codigo;
  if (!ok) fallos++;
  console.log(`${ok ? 'OK  ' : 'FALLA'} ${nombre}: ${avisos.error} (esperado ${codigo})`);
}

esperarError('refrigerante desconocido', {
  fluid: 'R999', pBaja: 800, pAlta: 2600, tSuccion: 12, tLiquido: 40
}, 'REFRIGERANTE_INVALIDO');
esperarError('presion alta menor', {
  fluid: 'R410A', pBaja: 2600, pAlta: 800, tSuccion: 12, tLiquido: 40
}, 'P_ALTA_MENOR');
esperarError('sobrecalentamiento negativo', {
  fluid: 'R410A', pBaja: 800, pAlta: 2600, tSuccion: -20, tLiquido: 40
}, 'SH_NEGATIVO');
esperarError('subenfriamiento negativo', {
  fluid: 'R410A', pBaja: 800, pAlta: 2600, tSuccion: 12, tLiquido: 60
}, 'SC_NEGATIVO');
esperarError('presion fuera de rango', {
  fluid: 'R410A', pBaja: 800, pAlta: 9000, tSuccion: 12, tLiquido: 40
}, 'P_FUERA_DE_RANGO');

console.log('\n--- Otros refrigerantes ---');
function presionParaTSat(d, tObjetivo) {
  let lo = d.pMin;
  let hi = d.pMax;
  for (let i = 0; i < 60; i++) {
    const mid = Math.sqrt(lo * hi);
    if (motor.tSat(d, mid) < tObjetivo) lo = mid; else hi = mid;
  }
  return Math.sqrt(lo * hi);
}

for (const fluid of ['R134a', 'R32', 'R290']) {
  const d = motor.datos[fluid];
  const pBaja = presionParaTSat(d, -10);
  const pAlta = presionParaTSat(d, 40);
  const r = motor.calcular(fluid, pBaja, pAlta, motor.tSat(d, pBaja) + 10, motor.tSat(d, pAlta) - 5);
  const ok = r.cop > 1 && r.cop < 12 && isFinite(r.h2);
  if (!ok) fallos++;
  console.log(`${ok ? 'OK  ' : 'FALLA'} ${fluid}: cop=${r.cop.toFixed(2)} h2=${r.h2.toFixed(1)} t2=${r.t2.toFixed(1)}`);
}

console.log('\n--- Figura ---');
const figura = motor.construirFigura(motor.datos.R410A, resultado);
const okFigura = figura.data.length > 10 && figura.layout.yaxis.type === 'log';
if (!okFigura) fallos++;
console.log(`${okFigura ? 'OK  ' : 'FALLA'} figura con ${figura.data.length} trazas`);

console.log(fallos === 0 ? '\nTODO OK' : `\n${fallos} FALLOS`);
process.exit(fallos === 0 ? 0 : 1);
