-- Fixture data for VoteControllerE2ETest
-- Resets before each test method via @Sql (default BEFORE_TEST_METHOD phase)
-- Delete order respects FK constraints (leaf tables first)

DELETE FROM auditoria_eventos WHERE eleccion_id = 'c0773f6c-ec1b-594a-8254-45d2fc83593f';
DELETE FROM votos WHERE eleccion_id = 'c0773f6c-ec1b-594a-8254-45d2fc83593f';
DELETE FROM participacion_electoral WHERE eleccion_id = 'c0773f6c-ec1b-594a-8254-45d2fc83593f';
DELETE FROM tokens_votacion WHERE eleccion_id = 'c0773f6c-ec1b-594a-8254-45d2fc83593f';
DELETE FROM candidatos WHERE eleccion_id = 'c0773f6c-ec1b-594a-8254-45d2fc83593f';
DELETE FROM elecciones WHERE id = 'c0773f6c-ec1b-594a-8254-45d2fc83593f';

-- Seed: an ACTIVE election
INSERT INTO elecciones (id, codigo, nombre, estado, fecha_inicio, fecha_fin)
VALUES ('c0773f6c-ec1b-594a-8254-45d2fc83593f', 'TEST-E2E-2026', 'E2E Test Election', 'ACTIVA',
        '2026-01-01 00:00:00+00', '2026-12-31 23:59:59+00');

-- Seed: a candidate for that election
INSERT INTO candidatos (id, eleccion_id, nombre, es_voto_en_blanco, numero_orden)
VALUES ('55d9a2a3-7e05-51fe-836d-4680dc900b58', 'c0773f6c-ec1b-594a-8254-45d2fc83593f',
        'E2E Test Candidate', false, 1);

-- Seed: an ISSUED token for funcionario_id=1 (EMP001 from initial seed)
-- SHA-256("e2e-test-raw-token-001") = 8e4fa56159f9b40b9b5edf7e15641b8015d0a5a26ab9cd4d7d8b2b812fdf9937
INSERT INTO tokens_votacion (id, eleccion_id, funcionario_id, token_hash, status, issued_at)
VALUES ('4843a300-6bb8-52f0-9471-a692de52c3c4', 'c0773f6c-ec1b-594a-8254-45d2fc83593f', 1,
        '8e4fa56159f9b40b9b5edf7e15641b8015d0a5a26ab9cd4d7d8b2b812fdf9937', 'ISSUED',
        '2026-06-01 00:00:00+00');
