# Especificación Técnica y Funcional: AirService PRO
**Software de Gestión Integral para Empresas de Climatización y HVAC**

---

## 1. Visión General del Programa

**AirService PRO** es una aplicación móvil y plataforma de gestión (FSM / ERP) diseñada específicamente para profesionalizar y digitalizar las operaciones de empresas e instaladores de aire acondicionado, calefacción y sistemas de climatización (HVAC) [1, 2].

### Propuesta de Valor y Eslóganes Principales
* **Propuesta Central:** *"Tu servicio puede ser excelente, pero si la gestión está desordenada, el cliente lo nota. Con AirService PRO tu empresa transmite más confianza."* [1]
* **Promesa de Marca:** *"Todo tu negocio en una sola App"* | *"Más orden. Mejor imagen. Más clientes conformes."* [1, 2]
* **Atributos Clave:** Simple | Rápido | Profesional [2]
* **Pilares Fundamentales:**
  1. **Más Confianza:** Imagen profesional ante el cliente final [1].
  2. **Más Orden:** Control centralizado de agencias, turnos e historial [1, 2].
  3. **Más Control:** Monitoreo en tiempo real de trabajos en curso y costos [1].
  4. **Más Profesionalismo:** Emisión de presupuestos y comprobantes digitales instantáneos [1, 2].

---

## 2. Estructura de Módulos y Funcionalidades Detalladas

La interfaz principal de **AirService PRO** está estructurada en 6 módulos operativos interconectados, accesibles desde la aplicación móvil y el panel de gestión [1, 2]:

```
+-------------------------------------------------------------------+
|                        AIRSERVICE PRO APP                         |
+-------------------------------------------------------------------+
| [1] Datos Cliente  | [2] Datos Equipo  | [3] Historial Servicios  |
| [4] Presupuestos   | [5] Comprobantes  | [6] Seguimiento Trabajo  |
+-------------------------------------------------------------------+
| [Acceso Directo] Agenda y Turnos | Clientes y Equipos | WhatsApp  |
+-------------------------------------------------------------------+
```

### Módulo 1: Datos del Cliente
* **Ficha Técnica del Cliente:** Almacena nombre completo, teléfono de contacto directo y correo electrónico [1].
* **Vinculación Multiequipo:** Asocia múltiples direcciones y equipos instalados a un mismo perfil de cliente [1, 2].
* **Acceso Rápido a Comunicación:** Botón de acción directa para llamadas y envío de mensajes vía WhatsApp [2].

### Módulo 2: Datos del Equipo (Gestión de Activos HVAC)
* **Identificación del Equipo:** Registro de tipo de unidad (ej. Aire Acondicionado Split), marca (ej. Electra), modelo (ej. ESI-12 3500 W) y número de serie único [1].
* **Fecha de Instalación:** Registro histórico de la fecha de montaje inicial (ej. 12/03/2023) para control de garantías y mantenimientos obligatorios [1].
* **Ficha de Especificaciones:** Capacidad térmica (W / BTU), tipo de gas refrigerante y ubicación del equipo en el inmueble [1].

### Módulo 3: Historial de Servicios
* **Trazabilidad Completa:** Cronograma histórico de todas las intervenciones realizadas en el equipo con fecha y tipo de servicio [1]:
  * *Mantenimiento Preventivo* (ej. revisión periódica) [1].
  * *Reparación de Fuga* (ej. detección y carga) [1].
  * *Limpieza de Filtros y Baterías* [1].
* **Indicadores de Estado:** Etiquetas visuales de estado para cada intervención (*Completado*, *En proceso*, *Cancelado*) [1].

### Módulo 4: Presupuestos y Cotizaciones en Sitio
* **Generación de Presupuestos Activos:** Creación de cotizaciones detalladas asociadas al número de comprobante (ej. Presupuesto activo #000125 - *Reparación de placa electrónica*) [1].
* **Flujo de Aprobación:** Estado visible del presupuesto (*Pendiente de aprobación*, *Aprobado*, *Rechazado*) para cierre de ventas en la misma visita [1].

### Módulo 5: Comprobantes y Facturación Digital
* **Emisión Instantánea:** Generación de comprobantes de servicio al finalizar el trabajo (ej. Comprobante #B-000987 por $35.000) [1].
* **Formatos de Exportación:** Compatibilidad nativa con documentos **PDF** [2].
* **Integración WhatsApp:** Envío de comprobantes, facturas y remitos directamente al WhatsApp del cliente con un solo clic [2].
* **Estado del Comprobante:** Registro de envío y cobro (*Enviado*, *Pagado*, *Pendiente*) [1].

### Módulo 6: Seguimiento del Trabajo y Operativa de Campo
* **Monitoreo en Tiempo Real:** Estado de los trabajos en curso (ej. *Instalación Split 3500 W*) [1].
* **Asignación de Técnico:** Identificación del operario responsable a cargo de la orden (ej. Técnico: Juan Pérez) [1].
* **Porcentaje de Avance:** Barra de progreso porcentual del trabajo (ej. *60% completado*) [1].
* **Agenda y Turnos:** Planificación de citas y organización de rutas diarias para optimizar tiempos de traslado [2].

---

## 3. Especificación del Sistema de Diseño y Paleta de Colores

El sistema visual de **AirService PRO** utiliza un tema oscuro (*Dark Mode*) de alta fidelidad tecnológica, diseñado para garantizar legibilidad en ambientes de trabajo de campo (exteriores e interiores) y transmitir solvencia técnica [1, 2].

### Paleta de Colores Primarios y de Fondo
| Elemento UI | Código Hex / Tono | Descripción y Aplicación |
| :--- | :--- | :--- |
| **Fondo Principal** | `#0B132B` / `#0D1B2A` | Azul Marino Profundo / Azul Noche. Aporta elegancia y reduce fatiga visual [1, 2]. |
| **Contenedores y Tarjetas** | `#152238` / `#1E2D4A` | Azul Oscuro Azulado con bordes suavizados y sutiles sombras [1, 2]. |
| **Texto Principal** | `#FFFFFF` | Blanco puro para títulos, encabezados principales y nombres de cliente [1, 2]. |
| **Texto Secundario / Iconos** | `#A0AEC0` / `#CBD5E1` | Gris claro neutro para etiquetas de datos y detalles de equipos [1, 2]. |
| **Acentos Neón / Brillos** | `#00D4FF` / `#38BDF8` | Azul Cian Neón / Celeste Eléctrico para botones principales y líneas de flujo [1, 2]. |

### Código de Colores Funcional (Estados y Badges)
El programa utiliza un sistema semántico de colores para identificar instantáneamente el estado de las tareas y alertas [1, 2]:

```
[ VERDE ]      ---> Completado / Servicio Finalizado (#28A745)
[ NARANJA ]    ---> Pendiente de Aprobación / En Espera (#FF9800)
[ CELESTE ]    ---> Enviado / En Curso / Avance Activo (#00A8FF)
[ ROJO ]       ---> Documentos PDF / Alertas Críticas (#E53935)
[ VERDE WA ]   ---> Integración Directa WhatsApp (#25D366)
```

1. **Verde Esmeralda (`#28A745`):** Utilizado en insignias de *"Completado"* dentro del Historial de Servicios [1].
2. **Naranja Cálido (`#FF9800`):** Utilizado para presupuestos en estado *"Pendiente de aprobación"* [1].
3. **Celeste Cian (`#00A8FF`):** Utilizado para comprobantes en estado *"Enviado"* y tareas en *"En curso"* [1].
4. **Rojo PDF (`#E53935`):** Identificador gráfico para archivos en formato PDF de presupuestos y comprobantes [2].
5. **Verde WhatsApp (`#25D366`):** Identificador oficial del canal de comunicación directa con clientes [2].

### Identidad Gráfica del Logotipo
* **Isotipo:** Unidad de aire acondicionado Split metálica plateada con pantalla digital azul en "18°" [1, 2].
* **Elemento Fuego/Frío:** Llama estilizada degradada en Naranja-Rojo (calefacción) entrelazada con una ráfaga de aire/agua azul cian en movimiento (refrigeración) [1, 2].
* **Logotipo Nombre:** Texto *"AirService"* en tipografía sans-serif limpia con contorno azul cian, acompañado de la insignia *"PRO"* sobre una cápsula metálica azul neón [1, 2].

---

## 4. Flujo Operativo del Usuario (Workflow Técnico)

1. **Recepción de Turno:** La oficina asigna la cita desde el módulo *Agenda y Turnos* [2].
2. **Llegada a Cliente:** El técnico abre la app, consulta *Datos del cliente* y el *Historial del equipo* instalado [1].
3. **Diagnóstico y Presupuesto:** Se genera una cotización en el módulo *Presupuestos* y se envía para aprobación digital [1].
4. **Ejecución y Seguimiento:** Se actualiza la orden en *Seguimiento del trabajo* marcando el avance (ej. 60%) [1].
5. **Cierre y Comprobante:** Al finalizar, el estado cambia a *Completado*, se genera el comprobante digital en *PDF* y se transmite automáticamente vía *WhatsApp* [1, 2].

---
*Documento de especificación generado para el proyecto de gestión HVAC AirService PRO.*
