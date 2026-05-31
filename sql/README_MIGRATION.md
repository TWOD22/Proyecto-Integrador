Migration steps for `Habilitado_Asig` improvements

Overview
- Current schema restricts `Usuario.Habilitado_Asig` to values (0,1). We propose optionally supporting a 3-state model (0=pendiente,1=esperando,2=listo/en_practica).
- Codebase has been updated to centralize mapping (`StateUtils`) and to support 0/1/2 semantics.

Recommended plan
1) Backup the `Usuario` table:
   - Run: `CREATE TABLE Usuario_backup AS SELECT * FROM Usuario;`
2) Optional: run application tests in staging.
3) Run migration script 001_allow_habilitado_0_1_2_and_migrate.sql to drop the old CHECK and recreate it allowing 0,1,2 and migrate invalid values.
   - Use SQL*Plus or SQL Developer. Example:
     - `sqlplus user/pass@orclUDI @001_allow_habilitado_0_1_2_and_migrate.sql`
4) Create audit support (optional) to track changes in future. Run 002_create_habilitado_audit_and_trigger.sql.
5) Restart application and monitor logs. Re-run `tools.SyncEstados` if needed to resync users.

Notes and precautions
- Ensure you have DB backups and permissions to DROP/CREATE constraints.
- If the constraint name differs from CHK_HABILITADO, edit the script to use the actual constraint name.
- After applying migration, recompile and restart your Java app.

If you prefer NOT to change the DB schema
- Keep the code clamping to 0/1 (already implemented); this avoids DB changes but prevents representing a distinct '2' state in the DB.

If you want, I can:
- Generate the exact ALTER statement that finds and drops the constraint by name.
- Add application-level logging for all writes to `Habilitado_Asig` (in Java) to help audit before/after migration.
