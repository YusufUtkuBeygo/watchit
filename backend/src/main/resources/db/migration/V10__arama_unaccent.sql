-- Aramada Türkçe harfleri sadeleştirmek için (İ/I/ı -> i, ş -> s, ç -> c, ğ -> g, ö -> o, ü -> u).
-- PostgreSQL'in hazır eklentisi; resmi postgres imajında gelir.
CREATE EXTENSION IF NOT EXISTS unaccent;
