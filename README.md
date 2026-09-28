# PkgLog — Sistema de Gestión de Logística y Envíos

Aplicación de escritorio para gestionar operaciones logísticas: despacho por
urgencia y fecha límite, rastreo de paquetes, red de rutas, planificación de
repartos por capacidad, jerarquía de centros de distribución, CRUD completo,
autenticación con roles, auditoría y reportes, con **persistencia en SQLite**.

Las estructuras de datos y los algoritmos de negocio están **implementados desde
cero** (cola de prioridad, cola por buckets, tabla hash, grafo ponderado, árbol,
ordenamientos y búsqueda) y se siguen usando para resolver los problemas del
dominio.

---

## Cómo ejecutar

Requisitos: **Java 21** y **Maven**.

```bash
make run        # compila y ejecuta desde el código fuente
make test       # ejecuta la suite de pruebas
make package    # genera el jar ejecutable
make run-jar    # compila el jar y lo ejecuta
make help       # lista todos los objetivos
```

La aplicación abre un diálogo de **inicio de sesión** y luego una ventana Swing
con las pestañas **Resumen, Despacho, Rastreo, Red, Repartos, Centros, Reportes,
Datos** y **Auditoría**.

### Cuentas por defecto

| Usuario    | Contraseña    | Rol   | Puede |
| :--------- | :------------ | :---- | :---- |
| `admin`    | `admin123`    | ADMIN | Todo, incluyendo CRUD de datos maestros, usuarios y borrados |
| `operator` | `operator123` | USER  | Despacho, cambios de estado, planificación y alta/edición de paquetes |

Las contraseñas se guardan como hash PBKDF2-HMAC-SHA256 con sal; nunca en claro.

---

## Persistencia

- Los datos viven en `pkglog.db` (SQLite) junto al directorio de trabajo. El
  archivo se crea solo y se llena con datos de ejemplo deterministas la primera
  vez que se abre.
- El esquema se declara en `com.dev.db.Schema`; cada tabla del dominio tiene su
  puerto de repositorio (`com.dev.db.repo`) y su adaptador JDBC
  (`com.dev.db.jdbc`) detrás de la fachada `Repositories`.
- `Store` es la única fachada que ve la UI: mantiene una copia en memoria para
  los algoritmos y escribe cada mutación en la base y en el registro de
  auditoría.
- Para reiniciar de cero, borre `pkglog.db` y vuelva a arrancar.

---

## Funcionalidades

| Pestaña | Qué hace |
| :--- | :--- |
| **Resumen** | Métricas generales: totales, entregados, cancelados, vencidos, ingresos (sin cancelados), rutas, distancia y costos. |
| **Despacho** | Extrae el envío más urgente (prioridad y luego fecha límite) y lo pasa a `EN_TRANSITO`; resalta vencidos en rojo. |
| **Rastreo** | Consulta por guía en tiempo constante, filtros y transiciones de estado. |
| **Red** | Grafo de ciudades/rutas y camino más corto por tiempo, costo o distancia. |
| **Repartos** | Planificación por capacidad: divide y vencerás + best-fit; lista los paquetes que no caben en la flota. |
| **Centros** | Jerarquía Nacional → Regional → Local y flota ordenada por capacidad. |
| **Reportes** | Listados ordenados (MergeSort/QuickSort), búsqueda binaria y resumen analítico. |
| **Datos** | CRUD completo de paquetes, rutas, zonas, centros, vehículos y usuarios. |
| **Auditoría** | Bitácora de solo lectura con cada mutación (actor, acción, entidad, referencia). |

Además, el menú **Archivo** permite **exportar** paquetes, rutas, zonas,
vehículos y centros a CSV e **importar** paquetes desde CSV (omite guías ya
existentes).

---

## Módulos de negocio (`com.dev.modules`)

Funciones estáticas sin estado: reciben datos y devuelven listas o mapas nuevos,
sin mutar la entrada.

- **`Deliveries`** — despacho por urgencia y fecha límite (`dispatchNext`),
  `urgent`, índice guía→paquete `O(1)` (`indexByWaybill` / `findByWaybill`),
  `updateStatus`, filtros, ordenamientos y agregados, más `overdue` /
  `overdueCount` para vencidos.
- **`Routing`** — red y camino más corto sin/peso (BFS y Dijkstra), ordenamiento
  de rutas, acumulados recursivos, partición geográfica (divide y vencerás) y
  planificación de flota consciente de la capacidad (`plan` / `partitionDeliveries`).
- **`Centers`** — jerarquía de centros (`hierarchy`, `nationalToLocal`, `byLevel`).
- **`Analytics`** — métricas agregadas: gasto, distancia, ingresos (excluyendo
  cancelados), ingresos entregados, peso, paquetes más pesado y por ruta/estado.

---

## Estructuras y algoritmos propios (`com.dev.ds`)

| Clase | Uso |
| :--- | :--- |
| `PriorityQueue` / `BucketQueue` | Despacho por urgencia; el bucket preserva el orden por fecha límite en cada nivel. |
| `HashMap` | Índice guía → paquete y otras indexaciones en memoria. |
| `WeightedGraph` | Red de rutas; Dijkstra para el camino de menor peso. |
| `Graph` | Red no ponderada; camino con menos paradas (BFS). |
| `Tree` / `BinaryTree` | Jerarquía de centros y organización de la flotilla. |
| `Sorting` | MergeSort (estable) y QuickSort. |
| `Search` | Búsqueda binaria sobre arreglos ordenados. |
| `Queue` / `Stack` | Recorridos auxiliares. |

Los cálculos acumulados de **tiempo, costo y distancia** usan **recursividad**;
la partición de entregas y el plan de flota usan **divide y vencerás**.

---

## Arquitectura

```
src/main/java/com/dev/
├── pkglog/     App: punto de entrada (Swing + login)
├── ui/         Paneles, ventana principal, Store y formularios CRUD
├── db/         Database/Schema, fachada Repositories
│   ├── repo/   Puertos de persistencia (interfaces)
│   └── jdbc/   Adaptadores SQLite
├── io/         Importación/exportación CSV
├── security/   Hash de contraseñas (PBKDF2)
├── modules/    Lógica de negocio: Deliveries, Routing, Centers, Analytics
├── ds/         Estructuras de datos y algoritmos propios
├── domain/     Modelos inmutables (Package, Route, Zone, Vehicle, Center, User, AuditEvent…)
└── data/       Seed: datos deterministas de ejemplo
```

- **`Store`** mantiene los datos en memoria como listas inmutables y persiste
  cada cambio a través de `Repositories`.
- Los **módulos** son funciones estáticas sin estado que devuelven datos nuevos;
  nunca mutan la entrada.
- **`Seed`** genera datos reproducibles (semilla fija 42).
- **`Seeder`** llena una base vacía sin sobrescribir datos existentes.

---

## Pruebas

Las pruebas unitarias (JUnit 5) cubren `ds`, `modules`, `data`, `db`, `io`, `ui`
y `pkglog`. Ejecútalas con:

```bash
make test
```

---

## Roadmap

Las capacidades de alto valor (P1) y deseables (P2) que aún no están
implementadas se documentan en [`ROADMAP.md`](ROADMAP.md).
