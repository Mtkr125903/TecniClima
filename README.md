# TecniClima

App Android de gestión para empresas de climatización y HVAC (módulos de Clientes, Equipos,
Historial de Servicios, Presupuestos, Comprobantes, Seguimiento de Trabajos, Agenda y Turnos,
**Diagrama de Mollier**, **Datos de la Empresa** y Técnicos).

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-7F52FF.svg) ![Android](https://img.shields.io/badge/Android-24%2B-3DDC84.svg) ![Room](https://img.shields.io/badge/Room-2.6.1-FF6D00.svg)
[![Android CI](https://github.com/Mtkr125903/TecniClima/actions/workflows/android.yml/badge.svg)](https://github.com/Mtkr125903/TecniClima/actions/workflows/android.yml)

## Stack

| Pieza | Versión / detalle |
| :--- | :--- |
| Kotlin | 1.9.23 · AGP 8.2.2 · Gradle 8.7 · JDK 17 |
| UI | XML + ViewBinding (sin Compose), Material 3 tema oscuro |
| Persistencia | Room 2.6.1 (`airservice.db`, migraciones manuales) |
| Navegación | Navigation Component 2.7.7 (un subgrafo por pestaña) |
| Imágenes | Coil 2.7.0 · cámara y galería sin permisos |
| PDF | Generador propio (`util/PdfGenerator.kt`), sin librerías externas · desglose de renglones y recargos |
| Mollier (P-h) | WebView local con Plotly.js empaquetado + motor termodinámico JS (tablas congeladas con CoolProp) |

## Requisitos

- JDK 17
- Android SDK con `compileSdk 34` (minSdk 24)
- `local.properties` con `sdk.dir=/ruta/al/Android/Sdk` (no se versiona)

## Clonar y compilar

```bash
git clone https://github.com/Mtkr125903/TecniClima.git
cd TecniClima
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Verificación

```bash
./gradlew assembleDebug test lintDebug   # build + unit tests + lint
./gradlew connectedAndroidTest           # tests instrumentados (requiere emulador)
node tools/mollier/probar_motor.js       # motor JS vs CoolProp
```

Reportes: `app/build/reports/lint-results-debug.html`

## Integración continua

GitHub Actions corre en cada push y pull request a `main`
(`.github/workflows/android.yml`): JDK 17, Android SDK 34, `assembleDebug test lintDebug` y
publicación del APK de debug como artefacto (`app-debug`, 14 días).

## Estructura

```
app/src/main/java/com/airservice/pro/
├── AirServiceApp.kt        # Application + DataSeeder (datos de demo)
├── data/                   # Entities.kt, Daos.kt, AppDb.kt (Room + migraciones)
├── ui/                     # un paquete por módulo + MainActivity (tabs/nav)
│   ├── client/  equipment/  history/  budget/
│   ├── receipt/ track/     agenda/    technician/   mollier/
│   ├── profile/            # datos de la empresa (perfil, logo)
│   └── common/UiHelpers.kt # estados vacíos, badges y colores
└── util/                   # Fecha, Numero, PDF, fotos, compartir, WhatsApp, MollierValidacion
```

- `res/navigation/nav_graph.xml`: cada pestaña es un subgrafo anidado, así conserva su propia pila.
  Las 8 pestañas son Clientes, Equipos, Historial, Presupuestos, Comprobantes, Seguimiento,
  Agenda y **Mollier** (al final, para no romper `irAPestana(6)`).
- `res/values/strings.xml`: todo el texto visible de la app.
- `tools/mollier/`: generador de tablas termodinámicas (`generar_datos.py`) y banco del motor JS
  (`probar_motor.js`).
- `especificaciones.md`: especificación funcional y de diseño del proyecto.
- `AGENTS.md`: guía de trabajo para agentes (comandos, restricciones de red y migraciones).

## Módulos

### Presupuestos

El editor (`ui/budget/BudgetEditFragment.kt`) permite cargar renglones desde el catálogo, aplicar
plantillas y agregar recargos; el total se calcula solo a partir de los renglones.

- **Renglones y recargos**: se editan y se borran **por índice**, nunca por `equals`. Con
  `data class` dos renglones idénticos (`items.remove(item)` / `items.indexOf(existente)`)
  editaban o borraban el equivocado.
- **Guardado transaccional**: presupuesto, renglones y recargos se escriben dentro de
  `db.withTransaction { }`, así no queda un presupuesto a medio guardar si falla un renglón.
- **Rotación**: cliente y equipo elegidos se guardan en `onSaveInstanceState` y se restauran al
  recrear el formulario.
- **Recargos fuera del catálogo**: si el recargo no está en `charge_catalog` no se puede deducir
  si era de temporada, así se respeta el importe ya guardado en lugar de recalcularlo.

### Datos de la empresa

Pantalla `ui/profile/CompanyProfileFragment.kt` (fila única `company_profile`, id = 1) con
nombre, matrícula, CUIT, teléfono, WhatsApp, alias de pago, dirección, validez por defecto de los
presupuestos y logo.

- Se accede desde el botón **Empresa** del header de `MainActivity`.
- El logo se elige de la galería (`PickVisualMedia`, sin permisos) y se copia al almacenamiento
  propio de la app con `PhotoStorage.copiarDesde`.
- La primera vez se avisa (una sola vez, con un flag en `SharedPreferences`) que faltan los datos
  de la empresa para que los documentos salgan con cabecera.

### PDF

`util/PdfGenerator.kt` es un generador PDF 1.4 propio (sin librerías externas).

- `PdfGenerator.Documento` acepta `renglones`, `recargos` y `empresa`; todos son opcionales, así
  que los comprobantes y los presupuestos antiguos se imprimen igual.
- Con desglose: tabla de renglones (concepto, cantidad, precio unitario, subtotal), sección de
  recargos y bloque `Subtotal` / `Recargos` antes de la banda `TOTAL`.
- Los importes se alinean a la derecha con las métricas reales de Helvetica; los conceptos largos
  se envuelven en varias líneas y la cabecera de tabla se repite al cortar página.
- Los datos de la empresa salen en el encabezado (solo los campos con contenido).

## Notas de implementación

- **Room sin `fallbackToDestructiveMigration`**: todo cambio de esquema exige subir `version` en
  `data/AppDb.kt` **y** registrar una `Migration` nueva en `AppDatabase.getInstance`.
- **Red**: en algunos entornos `repo.maven.apache.org` no es accesible; `settings.gradle` agrega
  `https://repo1.maven.org/maven2` antes de `mavenCentral()` por ese motivo.
- **Navigation 2.7.7**: las versiones 2.8.x vienen compiladas con Kotlin 2.0 y no compilan con
  Kotlin 1.9.23.
- **Fotos**: la cámara usa `TakePicture` y la galería `PickVisualMedia` (sin permisos); la imagen
  elegida se copia al almacenamiento propio de la app porque el acceso a la galería es temporal.
- **Compartir PDFs**: requiere `ClipData.newRawUri` + `FLAG_GRANT_READ_URI_PERMISSION`, o el
  chooser falla con *Permission Denial*.
- **Estados de presupuesto**: viven en `strings.xml` (`presupuesto_estados` y los `estado_*`).
  El texto es parte del dato guardado, así que debe coincidir exactamente con lo que espera
  `UiHelpers.colorEstado()` para pintar el color del estado.
- **Diagrama de Mollier (P-h)**: los assets `mollier.html`, `mollier.js`, `mollier_data.js` y
  `plotly-basic.min.js` se cargan desde `file:///android_asset/`. `mollier_data.js` es
  **generado** con `tools/mollier/generar_datos.py` (necesita `pip install CoolProp`) y se valida
  con `node tools/mollier/probar_motor.js`. La app NO usa CoolProp en runtime: el motor JS
  interpola las tablas congeladas. El puente JS→Kotlin es `window.evaluateCycle(json)` (objeto
  JSON) y notifica con `Android.onCycleCalculated(json)`, `Android.onResultsCalculated(sh, sc,
  tSatBaja, tSatAlta)` y `Android.onError(codigo)`. El parseo de entradas es estricto
  (`util/MollierValidacion.kt`): `NumeroUtil.aNumero` convierte basura en 0 y no sirve.

## Licencia

**PolyForm Noncommercial 1.0.0** — el código es público y se puede clonar, estudiar y usar con
fines **no comerciales** (personal, educativo, charities, organismos públicos).

**Usar TecniClima con fines comerciales requiere una licencia de pago.** Para consultarla:
[LICENCIA-COMERCIAL.md](LICENCIA-COMERCIAL.md) o escribile al usuario
[`Mtkr125903`](https://github.com/Mtkr125903). El texto completo de la licencia está en
[LICENSE](LICENSE).

Si preferís una licencia abierta clásica (MIT, Apache-2.0, GPL-3.0) o Business Source License
1.1, avisame y la cambio en un commit.
