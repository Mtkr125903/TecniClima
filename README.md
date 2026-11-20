# TecniClima

App Android de gestión para empresas de climatización y HVAC (módulos de Clientes, Equipos,
Historial de Servicios, Presupuestos, Comprobantes, Seguimiento de Trabajos, Agenda y Turnos,
**Diagrama de Mollier** y Técnicos).

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
| PDF | Generador propio (`util/PdfGenerator.kt`), sin librerías externas |
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
