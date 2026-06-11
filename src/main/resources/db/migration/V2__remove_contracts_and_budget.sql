DO $$
DECLARE
    fk_record RECORD;
BEGIN
    FOR fk_record IN
        SELECT conrelid::regclass AS table_name, conname
        FROM pg_constraint
        WHERE contype = 'f'
          AND confrelid IN (
              to_regclass('public.presupuesto_areas'),
              to_regclass('public.contrato_info'),
              to_regclass('public.contratos'),
              to_regclass('public.contrato')
          )
    LOOP
        EXECUTE format(
            'ALTER TABLE %s DROP CONSTRAINT IF EXISTS %I',
            fk_record.table_name,
            fk_record.conname
        );
    END LOOP;
END $$;

DROP TABLE IF EXISTS hw_costos CASCADE;
DROP TABLE IF EXISTS hardware_costos CASCADE;
DROP TABLE IF EXISTS presupuesto_areas CASCADE;

DROP TABLE IF EXISTS contrato_activos CASCADE;
DROP TABLE IF EXISTS contratos_activos CASCADE;
DROP TABLE IF EXISTS activo_contratos CASCADE;
DROP TABLE IF EXISTS contratos_proveedores CASCADE;
DROP TABLE IF EXISTS contrato_info CASCADE;
DROP TABLE IF EXISTS contratos CASCADE;
DROP TABLE IF EXISTS contrato CASCADE;
