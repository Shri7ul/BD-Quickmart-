# BD QuickMart — Infrastructure

Local infrastructure assets for the platform.

---

## Database

```
infrastructure/database/init/
├── 00_create_database.sql    Creates the bd_quickmart database
└── 01_verify_connection.sql  Confirms the database is reachable
```

Both are run **manually by the developer** through the phpMyAdmin SQL tab or
the MySQL command line. Nothing in this repository creates, drops or modifies
a database automatically.

See [../../docs/database/schema.md](../../docs/database/schema.md) for the
schema baseline and the module-to-table plan.

---

## XAMPP

XAMPP is installed and managed by the developer. This repository does not
install, start or configure it, and does not launch phpMyAdmin.

To start MySQL: open the XAMPP Control Panel and click **Start** next to
**MySQL**.

---

## Planned contents

| Path | Purpose | Goal |
|---|---|---|
| `docker/` | Container definitions for a portable local stack | Goal 18 |
| `monitoring/` | Metrics and alerting configuration | Goal 18 |
| `ci/` | GitHub Actions workflows | Goal 18 |
| `backup/` | Database backup and restore procedures | Goal 18 |

These are intentionally absent for now. Goal 01 introduces no infrastructure
beyond the MySQL instance the developer already runs, because nothing in the
current scope needs it.
