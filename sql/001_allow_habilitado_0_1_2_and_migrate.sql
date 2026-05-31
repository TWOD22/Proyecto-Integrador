-- Migration: allow Habilitado_Asig to accept 0,1,2 and migrate values
-- WARNING: Run with DBA or table-owner privileges. BACKUP your data first.
-- Steps:
-- 1) Backup table: create table Usuario_backup as select * from Usuario;
-- 2) Run this migration.

-- Drop existing CHECK constraint (assumes name CHK_HABILITADO; change if different)
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE Usuario DROP CONSTRAINT CHK_HABILITADO';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE = -2443 THEN NULL; -- constraint does not exist
    ELSE RAISE; END IF;
END;
/

-- Recreate constraint to allow 0,1,2
ALTER TABLE Usuario ADD CONSTRAINT CHK_HABILITADO CHECK (Habilitado_Asig IN (0,1,2));

-- Migrate existing invalid values to nearest valid (set to 1 by default)
UPDATE Usuario SET Habilitado_Asig = CASE WHEN Habilitado_Asig IN (0,1,2) THEN Habilitado_Asig ELSE 1 END WHERE Habilitado_Asig NOT IN (0,1,2);
COMMIT;

-- Optional: verify rows
SELECT Cedula_Usuario, Habilitado_Asig FROM Usuario WHERE Habilitado_Asig NOT IN (0,1,2);

-- End of migration
