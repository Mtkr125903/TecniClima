# AGENTS.md — TecniClima

App Android (Kotlin) de gestión para empresas de climatización/HVAC. Módulo único `:app`.
UI en XML con ViewBinding (sin Compose), Room para persistencia, Navigation Component para la
navegación por pestañas, tema oscuro Material.

> Nota de naming: la app visible se llama **TecniClima**, pero el package sigue siendo
> `com.airservice.pro`, la DB `airservice.db` y el FileProvider `${applicationId}.fileprovider`.
> No los renombres sin avisar: rompen instalaciones existentes.

## Comandos

```bash
./gradlew assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew test                   # unit tests JVM (app/src/test/.../util/)
./gradlew lintDebug              # reporte: app/build/reports/lint-results-debug.html
./gradlew connectedAndroidTest   # instrumented (app/src/androidTest/.../data/) — requiere emulador
./gradlew assembleDebug test lintDebug   # verificación completa (lo que se usa antes de dar por cerrado)
```

- APK debug: `~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk`
- Emulador disponible: AVD `Pixel_7` (`~/Android/Sdk/emulator/emulator -avd Pixel_7 ...`). Si lo lanzás
  desde una sesión que termina, el proceso muere: usá `setsid nohup ... > /tmp/emu.log 2>&1 & disown`.
- Base de datos del emulador: `adb exec-out run-as com.airservice.pro cat databases/airservice.db`
  (DB Room `airservice.db`, tablas en minúscula: `client`, `equipment`, `service`, `budget`,
  `receipt`, `technician`, `work`, `turn`, `counter`, `photo`).

## Restricciones del entorno (no romperlas)

- **Red:** `repo.maven.apache.org` NO es accesible. `settings.gradle` agrega
  `maven { url 'https://repo1.maven.org/maven2' }` antes de `mavenCentral()` (en `pluginManagement`
  y en `dependencyResolutionManagement`). No lo borres: sin él no resuelven dependencias.
  `dl.google.com` y `repo1.maven.org` sí funcionan.
- **Navigation Component debe quedar en 2.7.7.** Las 2.8.x vienen compiladas con Kotlin 2.0 y
  rompen el build con Kotlin 1.9.23 / AGP 8.2.2 / KSP 1.9.23-1.0.19.
- Room **no** usa `fallbackToDestructiveMigration`: todo cambio de esquema exige subir `version` en
  `data/AppDb.kt` **y** escribir una `Migration` nueva y registrarla en `AppDatabase.getInstance`
  (`addMigrations`). Sin migración, la app crashea en instalaciones existentes.
- `exportSchema = false`: no hay carpeta de schemas que actualizar.

## Arquitectura que no se deduce de los nombres

- **Una pestaña = un subgrafo anidado** en `res/navigation/nav_graph.xml` (`clientes_nav`,
  `equipos_nav`, `historial_nav`, `presupuestos_nav`, `comprobantes_nav`, `seguimiento_nav`,
  `agenda_nav`, `mollier_nav`). `ui/MainActivity.kt` es el dueño del `NavController`, del toolbar
  (`setupActionBarWithNavController`) y del `TabLayout`.
- Al cambiar de pestaña se usa `setPopUpTo(startDestination, false, true)` (saveState) +
  `setRestoreState(true)` para que cada pestaña conserve su pila (abrir una ficha, ir a otra
  pestaña y volver restaura la ficha).
- `MainActivity.tabsData` guarda por pestaña `grafo` + `raiz` (id del fragmento raíz). **Siempre
  hacer `popBackStack` al id de `raiz`, nunca al id del grafo**: hacer pop al grafo deja la pantalla
  vacía y asoma el fragmento que estaba debajo (bug ya corregido).
- Destino de detalle → se navega con `findNavController().navigate(actionId, bundleOf(...))`; volver
  con `findNavController().navigateUp()`.
- **Datos de demo:** `AirServiceApp.kt` → `DataSeeder.sembrar()` solo siembra si
  `clientDao().count() == 0`. Para regenerar el seed en el dispositivo hay que limpiar la app:
  `adb shell pm clear com.airservice.pro`.
- **Fotos:** archivos en `external-files/fotos`, compartidos como `content://` vía FileProvider
  (`${applicationId}.fileprovider`, paths en `res/xml/file_paths.xml`). La galería usa
  `PickVisualMedia` (sin permisos) pero la imagen debe **copiarse** al almacenamiento propio
  (`PhotoStorage.copiarDesde`), porque el acceso a la galería es temporal. La cámara usa
  `TakePicture` **sin** permiso CAMERA. La app no pide permisos de cámara ni de galería.
  Al borrar una foto hay que borrar fila **y** archivo; al borrar un equipo, también sus fotos.
- **PDF:** generador propio `util/PdfGenerator.kt` (sin librería externa). Para compartir PDFs,
  `util/ShareHelper.kt` necesita `ClipData.newRawUri` + `FLAG_GRANT_READ_URI_PERMISSION` (con solo
  el flag en el intent, el chooser falla con Permission Denial).
- **Mollier (P-h):** todo vive en `assets/` (`mollier.html`, `mollier.js`, `mollier_data.js`,
  `plotly-basic.min.js`) y se carga en un WebView con `file:///android_asset/mollier.html`.
  - `mollier_data.js` es **generado**: `tools/mollier/generar_datos.py` (necesita `pip install
    CoolProp`) congela las tablas de R410A/R134a/R32/R290. **No editar el asset a mano**: se
    regenera y se valida contra CoolProp con `node tools/mollier/probar_motor.js`.
  - La app NO usa CoolProp en runtime. El motor JS interpola las tablas congeladas.
  - El puente es `window.evaluateCycle(json)` (recibe un **objeto** JSON, no un string) y
    notifica a Kotlin con `Android.onCycleCalculated(json)`, `Android.onResultsCalculated(sh, sc,
    tSatBaja, tSatAlta)` y `Android.onError(codigo)`. Los códigos de error son strings
    compartidos con `MollierFragment`.
  - El parseo numérico nativo es estricto (`util/MollierValidacion.kt`): `NumeroUtil.aNumero`
    convierte basura en 0 y no sirve para validar.

## Convenciones del código

- Strings, comentarios y textos de UI en **español**; todo texto visible va en
  `res/values/strings.xml` (lint marca `HardcodedText`/`SetTextI18n`).
- ViewBinding obligatorio (`*Binding` generado), sin `findViewById`.
- Los DAO exponen `Flow` para observar; los fragmentos recogen con
  `repeatOnLifecycle(Lifecycle.State.STARTED)` y usan `UiHelpers.emptyState(...)` para el estado vacío.
- Estados de documentos/colores: usar `UiHelpers.colorEstado(...)` / `colorAvance(...)` en vez de
  hardcodear colores.
- Los importes de XML de fotos usan `coil.load(uri)`; para Coil 2.x el import es `coil.load`.
- Formularios en diálogo (`MaterialAlertDialogBuilder`) construidos como objetos estáticos
  (`XForm.show(fragment, ...)`) en `ui/<módulo>/`.

## Publicación

- Repo público: `https://github.com/Mtkr125903/TecniClima` (remoto SSH `git@github.com:Mtkr125903/TecniClima.git`,
  clave en `~/.ssh/id_tecniclima_ed25519`). El PAT de las tools MCP es **solo lectura**: no puede
  crear repos, issues, PRs ni editar metadata; para eso se usa `git push` por SSH.
- **Licencia:** PolyForm Noncommercial 1.0.0 (`LICENSE` + plantilla `LICENCIA-COMERCIAL.md`).
  El uso comercial requiere licencia de pago: no cambiar a MIT/Apache/GPL sin avisar al usuario.
- **CI:** `.github/workflows/android.yml` corre en cada push/PR a `main`:
  `assembleDebug test lintDebug` (JDK 17, Android SDK 34) + artefacto `app-debug`.
  No usar `android-actions/setup-android@v3`: en la imagen `ubuntu-26.04` falla al instalar el
  paquete legacy `tools`; se usa el `sdkmanager` preinstalado en
  `/usr/local/lib/android/sdk/cmdline-tools/latest/bin/sdkmanager`.
