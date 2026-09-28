# PkgLog — Roadmap

This document tracks what is **already shipped** (previously the P0
"must-have to be sellable" list) and the **high-value (P1)** and
**should-have (P2)** capabilities that remain. Each item is intentionally
scoped so it can become one atomic milestone with its own commits.

Legend: `[x]` done · `[ ]` pending

---

## Shipped — the sellable baseline (P0)

- [x] **Persistence layer** — SQLite via `org.xerial:sqlite-jdbc`, declarative
      schema, repository ports (`com.dev.db.repo`) with JDBC adapters
      (`com.dev.db.jdbc`) behind a `Repositories` facade. The store no longer
      loses data on restart.
- [x] **Seeding** — an empty database is filled from the deterministic `Seed`
      (12 zones, 14 routes, 12 centers, 6 vehicles, 60 packages) plus default
      accounts; existing data is never overwritten.
- [x] **Authentication & roles** — login gate with PBKDF2-HMAC-SHA256 password
      hashes; `ADMIN` vs `USER` enforced in the management UI.
- [x] **Audit trail** — every mutation is recorded (actor, action, entity,
      reference, timestamp) and viewable in the *Auditoría* tab.
- [x] **Full CRUD** — create/edit/delete for packages, routes, zones, centers,
      vehicles and users in the *Datos* tab.
- [x] **Capacity-aware planning** — `Routing.plan` never overloads a vehicle;
      excess packages are rebalanced or reported as unassigned.
- [x] **Correct revenue/weight metrics** — canceled shipments are excluded from
      revenue and weight aggregates.
- [x] **Deadline-aware dispatch & overdue alerts** — urgency orders by priority
      then earliest deadline; overdue counts surface in *Despacho* and *Resumen*.
- [x] **CSV bulk import/export** — packages import; every master table exports.

---

## P1 — High value (next)

- [ ] **Proof of delivery** — signature, photo, GPS coordinates and timestamp
      captured per delivery attempt; store as attachments and show in tracking.
- [ ] **Status history timeline** — per-package event history (who moved it,
      when, from/to) rather than only the current status.
- [ ] **REST API + webhooks** — expose CRUD and dispatch over HTTP so a web
      store, marketplace or ERP can integrate; emit events on status changes.
- [ ] **Label & waybill printing** — generate printable PDF labels with
      barcodes/QR for each shipment.
- [ ] **Barcode scanning workflows** — scan to advance status instead of typing
      waybills.
- [ ] **Realistic routing** — bidirectional/per-direction network edges, time
      windows, tolls and a map visualization of routes and stops.
- [ ] **Dashboards & KPIs** — trends over time, on-time delivery rate, SLA
      breach, cost per route/zone, with charts.
- [ ] **Report export** — PDF/Excel report generation for the *Reportes* tab.

## P2 — Should-have (later)

- [ ] **Driver mobile flow** — lightweight mobile view for drivers with route
      sheets, offline capture and sync.
- [ ] **Notifications & alerts** — email/SMS/push for overdue, failed attempts
      and exception states.
- [ ] **Exception management** — failed, returned, rescheduled and damaged
      flows with reasons and follow-up actions.
- [ ] **Multi-warehouse inventory** — stock levels per center, transfers
      between centers, low-stock alerts.
- [ ] **Billing, COD & insurance** — invoicing, cash-on-delivery, declared
      value and insurance surcharges, tax handling.
- [ ] **Configurable locale & currency** — replace hard-coded `R$`/Spanish
      strings with i18n and locale settings.
- [ ] **Observability & CI** — structured logging, metrics, and a build pipeline
      running the test suite on every change.
- [ ] **Data migrations** — versioned schema migrations via `PRAGMA
      user_version` so future releases upgrade existing databases safely.

---

## Guiding constraints

- Keep the **custom data structures and algorithms** (`com.dev.ds`) applied
  wherever they are the natural tool: bucket queue for dispatch, hash map for
  waybill indexing, graphs for routing, tree for the center hierarchy, sorting
  and binary search for reports, recursion and divide-and-conquer for
  cumulative metrics and fleet planning.
- Preserve the layered architecture: domain records stay immutable, modules stay
  stateless, persistence stays behind repository ports, and the UI talks only to
  the `Store` facade.
