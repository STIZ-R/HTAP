DO $$
BEGIN
   IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'htap') THEN
      ALTER ROLE htap PASSWORD 'htap';
      ALTER ROLE htap SET password_encryption = 'md5';
END IF;
END$$;

GRANT ALL PRIVILEGES ON DATABASE htapdb TO htap;
