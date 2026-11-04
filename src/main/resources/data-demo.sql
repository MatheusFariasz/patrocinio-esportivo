INSERT INTO clube (id, nome) VALUES (10, 'Clube de demonstração') ON CONFLICT(id) DO NOTHING;
INSERT INTO patrocinador (id, nome) VALUES (20, 'Patrocinador de demonstração') ON CONFLICT(id) DO NOTHING;
