INSERT INTO perfis (nome, descricao)
SELECT 'Admin', 'Administrador da empresa'
WHERE NOT EXISTS (SELECT 1 FROM perfis WHERE LOWER(nome) = 'admin');

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS perfil_uso VARCHAR(30);
UPDATE usuarios SET perfil_uso = 'Fornecedor' WHERE perfil_uso IS NULL OR perfil_uso = '';

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS cpf VARCHAR(14);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS foto_url VARCHAR(500);

-- Contas "Cliente e fornecedor" vendem no marketplace: a empresa é AMBOS, como no cadastro
-- (CadastroService). Corrige empresas antigas que ficaram como cliente/comprador.
UPDATE empresas SET tipo = 'AMBOS'
WHERE tipo IN ('CLIENTE', 'COMPRADOR')
  AND EXISTS (SELECT 1 FROM usuarios u WHERE u.empresa_id = empresas.id AND u.perfil_uso = 'ClienteFornecedor');

-- ── Coluna de estoque (migração idempotente) ─────────────────────────────────
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS estoque NUMERIC(10, 3);
UPDATE produtos SET estoque = 0 WHERE estoque IS NULL;

-- ── Código fixo por produto ──────────────────────────────────────────────────
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS codigo VARCHAR(40);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS codigo_origem VARCHAR(40);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS criado_em TIMESTAMP(6);
UPDATE produtos SET codigo = 'LEG-' || id WHERE codigo IS NULL OR codigo = '';
CREATE UNIQUE INDEX IF NOT EXISTS uk_produtos_codigo ON produtos(codigo);

ALTER TABLE solicitacoes_compra ADD COLUMN IF NOT EXISTS estoque_comprador_creditado BOOLEAN DEFAULT FALSE;
UPDATE solicitacoes_compra SET estoque_comprador_creditado = FALSE WHERE estoque_comprador_creditado IS NULL;

-- ── Pedidos marketplace (colunas extras + endereço) ───────────────────────────
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) DEFAULT 'pdv';
UPDATE pedido SET tipo = 'pdv' WHERE tipo IS NULL;
ALTER TABLE pedido ALTER COLUMN barraca_id DROP NOT NULL;

ALTER TABLE pedido ADD COLUMN IF NOT EXISTS empresa_compradora_id BIGINT REFERENCES empresas(id);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS empresa_fornecedora_id BIGINT REFERENCES empresas(id);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS endereco_entrega_id BIGINT REFERENCES enderecos_entrega(id);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS endereco_resumo VARCHAR(300);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS cep VARCHAR(9);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS logradouro VARCHAR(150);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS numero VARCHAR(20);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS complemento VARCHAR(80);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS bairro VARCHAR(80);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS cidade VARCHAR(80);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS uf VARCHAR(2);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS metodo_pagamento VARCHAR(20);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS taxa_entrega NUMERIC(10, 2) DEFAULT 0;
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS observacao VARCHAR(500);

ALTER TABLE pagamentos ADD COLUMN IF NOT EXISTS referencia_pagamento VARCHAR(120);

-- ── Vendas do PDV (chave de idempotência, troco e cliente opcional) ─────────
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS chave_venda VARCHAR(64);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS valor_recebido NUMERIC(10, 2);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS troco NUMERIC(10, 2);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS cliente_nome VARCHAR(100);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS cliente_documento VARCHAR(14);
CREATE UNIQUE INDEX IF NOT EXISTS uk_pedido_chave_venda ON pedido(chave_venda);

ALTER TABLE solicitacoes_compra ADD COLUMN IF NOT EXISTS pedido_id BIGINT REFERENCES pedido(id);

-- ── Cartões de crédito/débito (dados do cadastro; CVV NÃO é armazenado) ─────
-- Os detalhes do cartão ficam aqui, NÃO em formas_pagamento_salvas.
-- formas_pagamento_salvas = tipos de pagamento (PIX, crédito, débito, dinheiro).
CREATE TABLE IF NOT EXISTS cartoes_pagamento_salvos (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    tipo VARCHAR(20) NOT NULL,
    apelido VARCHAR(80),
    bandeira VARCHAR(40) NOT NULL,
    ultimos_digitos VARCHAR(4) NOT NULL,
    numero_mascarado VARCHAR(24) NOT NULL,
    validade VARCHAR(5) NOT NULL,
    titular VARCHAR(120) NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Desativa catálogo legado (mantém histórico de pedidos) ───────────────────
UPDATE empresas SET tipo = 'INATIVO'
WHERE tipo IN ('DISTRIBUIDOR', 'PLATAFORMA')
  AND cnpj NOT IN ('30.001.001/0001-01', '30.002.002/0001-02', '30.003.003/0001-03');

UPDATE produtos SET ativo = 0
WHERE empresa_id IN (SELECT id FROM empresas WHERE tipo = 'INATIVO');

-- ── 3 Distribuidoras parceiras ───────────────────────────────────────────────
INSERT INTO empresas (nome, cnpj, telefone, tipo, descricao, logo_url, capa_url)
SELECT 'Casa dos Vinhos', '30.001.001/0001-01', '81999001001', 'DISTRIBUIDOR',
       'Vinhos, espumantes e sucos de uva sem álcool, nacionais e importados, para bares, restaurantes e eventos.',
       '/uploads/empresas/casa-dos-vinhos-logo.png',
       '/uploads/empresas/casa-dos-vinhos-capa.png'
WHERE NOT EXISTS (SELECT 1 FROM empresas WHERE cnpj = '30.001.001/0001-01');

INSERT INTO empresas (nome, cnpj, telefone, tipo, descricao, logo_url, capa_url)
SELECT 'Cervejaria Caruaru', '30.002.002/0001-02', '81999002002', 'DISTRIBUIDOR',
       'Cervejas geladas e importadas para revenda em Caruaru e região.',
       '/uploads/empresas/cervejaria-caruaru-logo.png',
       '/uploads/empresas/cervejaria-caruaru-capa.png'
WHERE NOT EXISTS (SELECT 1 FROM empresas WHERE cnpj = '30.002.002/0001-02');

INSERT INTO empresas (nome, cnpj, telefone, tipo, descricao, logo_url, capa_url)
SELECT 'Whisky Labs', '30.003.003/0001-03', '81999003003', 'DISTRIBUIDOR',
       'Especialistas na distribuição de whiskies, conhaques e destilados premium para colecionadores e revendedores.',
       '/uploads/empresas/whisky-labs-logo.png',
       '/uploads/empresas/whisky-labs-capa.png'
WHERE NOT EXISTS (SELECT 1 FROM empresas WHERE cnpj = '30.003.003/0001-03');

UPDATE empresas SET
  nome = 'Casa dos Vinhos',
  tipo = 'DISTRIBUIDOR',
  telefone = '81999001001',
  descricao = 'Vinhos, espumantes e sucos de uva sem álcool, nacionais e importados, para bares, restaurantes e eventos.',
  logo_url = '/uploads/empresas/casa-dos-vinhos-logo.png',
  capa_url = '/uploads/empresas/casa-dos-vinhos-capa.png'
WHERE cnpj = '30.001.001/0001-01';

UPDATE empresas SET
  nome = 'Cervejaria Caruaru',
  tipo = 'DISTRIBUIDOR',
  telefone = '81999002002',
  descricao = 'Cervejas geladas e importadas para revenda em Caruaru e região.',
  logo_url = '/uploads/empresas/cervejaria-caruaru-logo.png',
  capa_url = '/uploads/empresas/cervejaria-caruaru-capa.png'
WHERE cnpj = '30.002.002/0001-02';

UPDATE empresas SET
  nome = 'Whisky Labs',
  tipo = 'DISTRIBUIDOR',
  telefone = '81999003003',
  descricao = 'Especialistas na distribuição de whiskies, conhaques e destilados premium para colecionadores e revendedores.',
  logo_url = '/uploads/empresas/whisky-labs-logo.png',
  capa_url = '/uploads/empresas/whisky-labs-capa.png'
WHERE cnpj = '30.003.003/0001-03';

-- ── Casa dos Vinhos — produtos (catálogo de bebidas sem álcool: 15 produtos) ──
INSERT INTO produtos (empresa_id, codigo, nome, preco_venda, estoque, descricao, imagem_url, unidade, ativo)
SELECT e.id, v.codigo, v.nome, v.preco, v.estoque, v.descricao, v.imagem, 'UN', 1
FROM empresas e
CROSS JOIN (VALUES
    ('MKT-CDV-006', 'Suco de uva integral Serra Gaúcha 1 L', 17.90, 120, 'Linha Acessível · origem Brasil. Bebida sem álcool.', '/uploads/produtos/cdv-suco-de-uva-integral-serra-gaucha-1-l.png'),
    ('MKT-CDV-007', 'Suco de uva branca integral 1 L', 19.90, 90, 'Linha Acessível · origem Brasil. Bebida sem álcool.', '/uploads/produtos/cdv-suco-de-uva-branca-integral-1-l.png'),
    ('MKT-CDV-008', 'Néctar de uva premium 1 L', 14.90, 150, 'Linha Acessível · origem Brasil. Bebida sem álcool.', '/uploads/produtos/cdv-nectar-de-uva-premium-1-l.png'),
    ('MKT-CDV-009', 'Espumante de uva sem álcool 660 ml', 24.90, 80, 'Linha Acessível · origem Brasil. Bebida sem álcool.', '/uploads/produtos/cdv-espumante-de-uva-sem-alcool-660-ml.png'),
    ('MKT-CDV-010', 'Suco de uva rosé integral 1 L', 22.90, 70, 'Linha Acessível · origem Brasil. Bebida sem álcool.', '/uploads/produtos/cdv-suco-de-uva-rose-integral-1-l.png'),
    ('MKT-CDV-011', 'Vinho tinto desalcoolizado 750 ml', 49.90, 65, 'Linha Intermediário · origem Brasil. Bebida sem álcool.', '/uploads/produtos/cdv-vinho-tinto-desalcoolizado-750-ml.png'),
    ('MKT-CDV-012', 'Vinho branco desalcoolizado 750 ml', 54.90, 55, 'Linha Intermediário · origem Brasil. Bebida sem álcool.', '/uploads/produtos/cdv-vinho-branco-desalcoolizado-750-ml.png'),
    ('MKT-CDV-013', 'Espumante brut sem álcool 750 ml', 59.90, 60, 'Linha Intermediário · origem Brasil. Bebida sem álcool.', '/uploads/produtos/cdv-espumante-brut-sem-alcool-750-ml.png'),
    ('MKT-CDV-014', 'Suco de uva orgânico reserva 750 ml', 39.90, 75, 'Linha Intermediário · origem Brasil. Bebida sem álcool.', '/uploads/produtos/cdv-suco-de-uva-organico-reserva-750-ml.png'),
    ('MKT-CDV-015', 'Vinho rosé desalcoolizado 750 ml', 79.90, 40, 'Linha Intermediário · origem Espanha. Bebida sem álcool.', '/uploads/produtos/cdv-vinho-rose-desalcoolizado-750-ml.png'),
    ('MKT-CDV-016', 'Espumante rosé sem álcool 750 ml', 119.90, 30, 'Linha Premium · origem França. Bebida sem álcool.', '/uploads/produtos/cdv-espumante-rose-sem-alcool-750-ml.png'),
    ('MKT-CDV-017', 'Vinho tinto desalcoolizado reserva 750 ml', 129.90, 25, 'Linha Premium · origem Espanha. Bebida sem álcool.', '/uploads/produtos/cdv-vinho-tinto-desalcoolizado-reserva-750-ml.png'),
    ('MKT-CDV-018', 'Vinho branco desalcoolizado Chardonnay 750 ml', 139.90, 22, 'Linha Premium · origem Alemanha. Bebida sem álcool.', '/uploads/produtos/cdv-vinho-branco-desalcoolizado-chardonnay-750-ml.png'),
    ('MKT-CDV-019', 'Espumante sem álcool cuvée 750 ml', 149.90, 18, 'Linha Premium · origem Itália. Bebida sem álcool.', '/uploads/produtos/cdv-espumante-sem-alcool-cuvee-750-ml.png'),
    ('MKT-CDV-020', 'Vinho tinto desalcoolizado premium 750 ml', 169.90, 15, 'Linha Premium · origem França. Bebida sem álcool.', '/uploads/produtos/cdv-vinho-tinto-desalcoolizado-premium-750-ml.png')
) AS v(codigo, nome, preco, estoque, descricao, imagem)
WHERE e.cnpj = '30.001.001/0001-01'
  AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.codigo = v.codigo);

-- Catálogo de vinhos anterior (MKT-CDV-001 a 005): desativado, mantém o histórico de pedidos;
-- sai também dos quiosques da própria distribuidora, como ao remover um produto.
DELETE FROM estoque_barraca WHERE produto_id IN (SELECT id FROM produtos WHERE codigo IN ('MKT-CDV-001', 'MKT-CDV-002', 'MKT-CDV-003', 'MKT-CDV-004', 'MKT-CDV-005'));
UPDATE produtos SET ativo = 0 WHERE codigo IN ('MKT-CDV-001', 'MKT-CDV-002', 'MKT-CDV-003', 'MKT-CDV-004', 'MKT-CDV-005');

-- ── Cervejaria Caruaru — produtos ────────────────────────────────────────────
INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo, codigo)
SELECT e.id, 'Cerveja Heineken Long Neck 330ml', 5.99, 'UN',
       'Cerveja Heineken long neck 330 ml.',
       '/uploads/produtos/cerveja-heineken-long-neck.png', 1, 'MKT-CC-001'
FROM empresas e WHERE e.cnpj = '30.002.002/0001-02'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Cerveja Heineken Long Neck 330ml');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo, codigo)
SELECT e.id, 'Cerveja Corona Long Neck', 6.99, 'UN',
       'Cerveja Corona Extra long neck 330 ml.',
       '/uploads/produtos/cerveja-corona-long-neck.png', 1, 'MKT-CC-002'
FROM empresas e WHERE e.cnpj = '30.002.002/0001-02'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Cerveja Corona Long Neck');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo, codigo)
SELECT e.id, 'Cerveja Amstel Lata 269ml', 3.19, 'UN',
       'Cerveja Amstel lata 269 ml.',
       '/uploads/produtos/cerveja-amstel-lata.png', 1, 'MKT-CC-003'
FROM empresas e WHERE e.cnpj = '30.002.002/0001-02'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Cerveja Amstel Lata 269ml');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo, codigo)
SELECT e.id, 'Cerveja Cerpa Export Long Neck 350ml', 8.39, 'UN',
       'Cerveja Cerpa Export long neck 350 ml.',
       '/uploads/produtos/cerveja-cerpa-export-long-neck.png', 1, 'MKT-CC-004'
FROM empresas e WHERE e.cnpj = '30.002.002/0001-02'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Cerveja Cerpa Export Long Neck 350ml');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo, codigo)
SELECT e.id, 'Cerveja Paulaner Munchen Weissbier 500ml', 11.19, 'UN',
       'Cerveja Paulaner Munchen Weissbier garrafa 500 ml.',
       '/uploads/produtos/cerveja-paulaner-munchen-weissbier.png', 1, 'MKT-CC-005'
FROM empresas e WHERE e.cnpj = '30.002.002/0001-02'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Cerveja Paulaner Munchen Weissbier 500ml');

UPDATE produtos SET preco_venda = 5.99, ativo = 1,
  imagem_url = '/uploads/produtos/cerveja-heineken-long-neck.png'
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.002.002/0001-02'
  AND produtos.nome = 'Cerveja Heineken Long Neck 330ml';

UPDATE produtos SET preco_venda = 6.99, ativo = 1,
  imagem_url = '/uploads/produtos/cerveja-corona-long-neck.png'
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.002.002/0001-02'
  AND produtos.nome = 'Cerveja Corona Long Neck';

UPDATE produtos SET preco_venda = 3.19, ativo = 1,
  imagem_url = '/uploads/produtos/cerveja-amstel-lata.png'
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.002.002/0001-02'
  AND produtos.nome = 'Cerveja Amstel Lata 269ml';

UPDATE produtos SET preco_venda = 8.39, ativo = 1,
  imagem_url = '/uploads/produtos/cerveja-cerpa-export-long-neck.png'
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.002.002/0001-02'
  AND produtos.nome = 'Cerveja Cerpa Export Long Neck 350ml';

UPDATE produtos SET preco_venda = 11.19, ativo = 1,
  imagem_url = '/uploads/produtos/cerveja-paulaner-munchen-weissbier.png'
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.002.002/0001-02'
  AND produtos.nome = 'Cerveja Paulaner Munchen Weissbier 500ml';

-- ── Whisky Labs — produtos ───────────────────────────────────────────────────
INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo, codigo)
SELECT e.id, 'Whisky Escocês Royal Salute 21 Anos The Signature Blend 700ml', 989.00, 'UN',
       'Whisky escocês Royal Salute 21 anos, garrafa 700 ml.',
       '/uploads/produtos/whisky-royal-salute-21.png', 1, 'MKT-WL-001'
FROM empresas e WHERE e.cnpj = '30.003.003/0001-03'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Whisky Escocês Royal Salute 21 Anos The Signature Blend 700ml');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo, codigo)
SELECT e.id, 'Whisky The Balvenie PortWood 21 Anos 700ml Single Malt Escocês', 2795.30, 'UN',
       'Whisky single malt The Balvenie PortWood 21 anos, garrafa 700 ml.',
       '/uploads/produtos/whisky-balvenie-portwood-21.png', 1, 'MKT-WL-002'
FROM empresas e WHERE e.cnpj = '30.003.003/0001-03'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Whisky The Balvenie PortWood 21 Anos 700ml Single Malt Escocês');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo, codigo)
SELECT e.id, 'Whisky Glenfiddich Single Malt 12 Anos 750ml Escocês', 415.75, 'UN',
       'Whisky single malt Glenfiddich 12 anos, garrafa 750 ml.',
       '/uploads/produtos/whisky-glenfiddich-12.png', 1, 'MKT-WL-003'
FROM empresas e WHERE e.cnpj = '30.003.003/0001-03'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Whisky Glenfiddich Single Malt 12 Anos 750ml Escocês');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo, codigo)
SELECT e.id, 'Conhaque Martell L''Or de Jean Martell 700ml', 18711.00, 'UN',
       'Conhaque Martell L''Or de Jean Martell, garrafa 700 ml.',
       '/uploads/produtos/conhaque-martell-lor.png', 1, 'MKT-WL-004'
FROM empresas e WHERE e.cnpj = '30.003.003/0001-03'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Conhaque Martell L''Or de Jean Martell 700ml');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo, codigo)
SELECT e.id, 'Whisky Suntory Hibiki Japanese Harmony 700ml | Blended Premium Japonês', 729.90, 'UN',
       'Whisky japonês Suntory Hibiki Japanese Harmony, garrafa 700 ml.',
       '/uploads/produtos/whisky-hibiki-japanese-harmony.png', 1, 'MKT-WL-005'
FROM empresas e WHERE e.cnpj = '30.003.003/0001-03'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Whisky Suntory Hibiki Japanese Harmony 700ml | Blended Premium Japonês');

UPDATE produtos SET preco_venda = 989.00, ativo = 1,
  imagem_url = '/uploads/produtos/whisky-royal-salute-21.png'
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.003.003/0001-03'
  AND produtos.nome = 'Whisky Escocês Royal Salute 21 Anos The Signature Blend 700ml';

UPDATE produtos SET preco_venda = 2795.30, ativo = 1,
  imagem_url = '/uploads/produtos/whisky-balvenie-portwood-21.png'
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.003.003/0001-03'
  AND produtos.nome = 'Whisky The Balvenie PortWood 21 Anos 700ml Single Malt Escocês';

UPDATE produtos SET preco_venda = 415.75, ativo = 1,
  imagem_url = '/uploads/produtos/whisky-glenfiddich-12.png'
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.003.003/0001-03'
  AND produtos.nome = 'Whisky Glenfiddich Single Malt 12 Anos 750ml Escocês';

UPDATE produtos SET preco_venda = 18711.00, ativo = 1,
  imagem_url = '/uploads/produtos/conhaque-martell-lor.png'
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.003.003/0001-03'
  AND produtos.nome = 'Conhaque Martell L''Or de Jean Martell 700ml';

UPDATE produtos SET preco_venda = 729.90, ativo = 1,
  imagem_url = '/uploads/produtos/whisky-hibiki-japanese-harmony.png'
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.003.003/0001-03'
  AND produtos.nome = 'Whisky Suntory Hibiki Japanese Harmony 700ml | Blended Premium Japonês';

-- Desativa produtos extras dos 3 parceiros (catálogo oficial de cada loja)
UPDATE produtos SET ativo = 0
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.001.001/0001-01'
  AND produtos.nome NOT IN (
    'Suco de uva integral Serra Gaúcha 1 L',
    'Suco de uva branca integral 1 L',
    'Néctar de uva premium 1 L',
    'Espumante de uva sem álcool 660 ml',
    'Suco de uva rosé integral 1 L',
    'Vinho tinto desalcoolizado 750 ml',
    'Vinho branco desalcoolizado 750 ml',
    'Espumante brut sem álcool 750 ml',
    'Suco de uva orgânico reserva 750 ml',
    'Vinho rosé desalcoolizado 750 ml',
    'Espumante rosé sem álcool 750 ml',
    'Vinho tinto desalcoolizado reserva 750 ml',
    'Vinho branco desalcoolizado Chardonnay 750 ml',
    'Espumante sem álcool cuvée 750 ml',
    'Vinho tinto desalcoolizado premium 750 ml'
  );

UPDATE produtos SET ativo = 0
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.002.002/0001-02'
  AND produtos.nome NOT IN (
    'Cerveja Heineken Long Neck 330ml',
    'Cerveja Corona Long Neck',
    'Cerveja Amstel Lata 269ml',
    'Cerveja Cerpa Export Long Neck 350ml',
    'Cerveja Paulaner Munchen Weissbier 500ml'
  );

UPDATE produtos SET ativo = 0
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.003.003/0001-03'
  AND produtos.nome NOT IN (
    'Whisky Escocês Royal Salute 21 Anos The Signature Blend 700ml',
    'Whisky The Balvenie PortWood 21 Anos 700ml Single Malt Escocês',
    'Whisky Glenfiddich Single Malt 12 Anos 750ml Escocês',
    'Conhaque Martell L''Or de Jean Martell 700ml',
    'Whisky Suntory Hibiki Japanese Harmony 700ml | Blended Premium Japonês'
  );

-- ── Estoque inicial dos parceiros (somente quando ainda não movimentado) ─────
UPDATE produtos SET estoque = 300
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.002.002/0001-02'
  AND produtos.nome = 'Cerveja Amstel Lata 269ml'
  AND (produtos.estoque IS NULL OR (produtos.estoque = 0 AND NOT EXISTS (
    SELECT 1 FROM itens_solicitacao_compra i WHERE i.produto_id = produtos.id)));

UPDATE produtos SET estoque = 200
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.002.002/0001-02'
  AND produtos.nome = 'Cerveja Heineken Long Neck 330ml'
  AND (produtos.estoque IS NULL OR (produtos.estoque = 0 AND NOT EXISTS (
    SELECT 1 FROM itens_solicitacao_compra i WHERE i.produto_id = produtos.id)));

UPDATE produtos SET estoque = 150
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.002.002/0001-02'
  AND produtos.nome = 'Cerveja Corona Long Neck'
  AND (produtos.estoque IS NULL OR (produtos.estoque = 0 AND NOT EXISTS (
    SELECT 1 FROM itens_solicitacao_compra i WHERE i.produto_id = produtos.id)));

UPDATE produtos SET estoque = 100
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.002.002/0001-02'
  AND produtos.nome = 'Cerveja Cerpa Export Long Neck 350ml'
  AND (produtos.estoque IS NULL OR (produtos.estoque = 0 AND NOT EXISTS (
    SELECT 1 FROM itens_solicitacao_compra i WHERE i.produto_id = produtos.id)));

UPDATE produtos SET estoque = 80
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.002.002/0001-02'
  AND produtos.nome = 'Cerveja Paulaner Munchen Weissbier 500ml'
  AND (produtos.estoque IS NULL OR (produtos.estoque = 0 AND NOT EXISTS (
    SELECT 1 FROM itens_solicitacao_compra i WHERE i.produto_id = produtos.id)));

UPDATE produtos SET estoque = 300
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.003.003/0001-03'
  AND produtos.nome = 'Whisky Glenfiddich Single Malt 12 Anos 750ml Escocês'
  AND (produtos.estoque IS NULL OR (produtos.estoque = 0 AND NOT EXISTS (
    SELECT 1 FROM itens_solicitacao_compra i WHERE i.produto_id = produtos.id)));

UPDATE produtos SET estoque = 200
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.003.003/0001-03'
  AND produtos.nome = 'Whisky Suntory Hibiki Japanese Harmony 700ml | Blended Premium Japonês'
  AND (produtos.estoque IS NULL OR (produtos.estoque = 0 AND NOT EXISTS (
    SELECT 1 FROM itens_solicitacao_compra i WHERE i.produto_id = produtos.id)));

UPDATE produtos SET estoque = 150
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.003.003/0001-03'
  AND produtos.nome = 'Whisky Escocês Royal Salute 21 Anos The Signature Blend 700ml'
  AND (produtos.estoque IS NULL OR (produtos.estoque = 0 AND NOT EXISTS (
    SELECT 1 FROM itens_solicitacao_compra i WHERE i.produto_id = produtos.id)));

UPDATE produtos SET estoque = 100
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.003.003/0001-03'
  AND produtos.nome = 'Whisky The Balvenie PortWood 21 Anos 700ml Single Malt Escocês'
  AND (produtos.estoque IS NULL OR (produtos.estoque = 0 AND NOT EXISTS (
    SELECT 1 FROM itens_solicitacao_compra i WHERE i.produto_id = produtos.id)));

UPDATE produtos SET estoque = 80
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.003.003/0001-03'
  AND produtos.nome = 'Conhaque Martell L''Or de Jean Martell 700ml'
  AND (produtos.estoque IS NULL OR (produtos.estoque = 0 AND NOT EXISTS (
    SELECT 1 FROM itens_solicitacao_compra i WHERE i.produto_id = produtos.id)));

-- ── SaborViva — águas, sucos e refrigerantes (catálogo oficial: 15 produtos) ──
INSERT INTO empresas (nome, cnpj, telefone, tipo, descricao, logo_url, capa_url)
SELECT 'SaborViva', '30.004.004/0001-04', '81999004004', 'DISTRIBUIDOR',
       'Águas, sucos e refrigerantes para bares, mercados e eventos. Bebidas para hoje, negócios para sempre.',
       '/uploads/empresas/saborviva-logo.png',
       '/uploads/empresas/saborviva-capa.png'
WHERE NOT EXISTS (SELECT 1 FROM empresas WHERE cnpj = '30.004.004/0001-04');

INSERT INTO produtos (empresa_id, codigo, nome, preco_venda, estoque, descricao, imagem_url, unidade, ativo)
SELECT e.id, v.codigo, v.nome, v.preco, v.estoque, v.descricao, v.imagem, 'UN', 1
FROM empresas e
CROSS JOIN (VALUES
    ('MKT-SV-001', 'Água mineral sem gás 500 ml', 2.50, 500, 'Água mineral natural sem gás, garrafa 500 ml.', '/uploads/produtos/saborviva-agua-sem-gas-500ml.png'),
    ('MKT-SV-002', 'Água mineral com gás 500 ml', 3.50, 350, 'Água mineral com gás, garrafa 500 ml.', '/uploads/produtos/saborviva-agua-com-gas-500ml.png'),
    ('MKT-SV-003', 'Água mineral sem gás 1,5 L', 5.00, 250, 'Água mineral natural sem gás, garrafa 1,5 L.', '/uploads/produtos/saborviva-agua-sem-gas-1-5l.png'),
    ('MKT-SV-004', 'Água mineral galão 5 L', 12.00, 100, 'Água mineral natural sem gás, galão 5 L.', '/uploads/produtos/saborviva-agua-galao-5l.png'),
    ('MKT-SV-005', 'Água de coco 330 ml', 6.50, 180, 'Água de coco natural, caixinha 330 ml.', '/uploads/produtos/saborviva-agua-de-coco-330ml.png'),
    ('MKT-SV-006', 'Suco de laranja 1 L', 9.90, 200, 'Suco de laranja 100% fruta, caixa 1 L.', '/uploads/produtos/saborviva-suco-laranja-1l.png'),
    ('MKT-SV-007', 'Suco de uva integral 1 L', 14.90, 150, 'Suco de uva integral 100% fruta, caixa 1 L.', '/uploads/produtos/saborviva-suco-uva-integral-1l.png'),
    ('MKT-SV-008', 'Suco de maçã 1 L', 10.50, 180, 'Suco de maçã 100% fruta, caixa 1 L.', '/uploads/produtos/saborviva-suco-maca-1l.png'),
    ('MKT-SV-009', 'Suco de manga 1 L', 8.90, 220, 'Suco de manga 100% fruta, caixa 1 L.', '/uploads/produtos/saborviva-suco-manga-1l.png'),
    ('MKT-SV-010', 'Suco de maracujá 1 L', 9.50, 180, 'Suco de maracujá 100% fruta, caixa 1 L.', '/uploads/produtos/saborviva-suco-maracuja-1l.png'),
    ('MKT-SV-011', 'Coca-Cola Original 2 L', 11.90, 450, 'Refrigerante Coca-Cola sabor original, garrafa 2 L.', '/uploads/produtos/saborviva-coca-cola-original-2l.png'),
    ('MKT-SV-012', 'Coca-Cola Zero 2 L', 11.90, 300, 'Refrigerante Coca-Cola zero açúcar, garrafa 2 L.', '/uploads/produtos/saborviva-coca-cola-zero-2l.png'),
    ('MKT-SV-013', 'Guaraná Antarctica 2 L', 9.50, 500, 'Refrigerante Guaraná Antarctica, garrafa 2 L.', '/uploads/produtos/saborviva-guarana-antarctica-2l.png'),
    ('MKT-SV-014', 'Sprite 2 L', 9.90, 400, 'Refrigerante Sprite sabor limão, garrafa 2 L.', '/uploads/produtos/saborviva-sprite-2l.png'),
    ('MKT-SV-015', 'Fanta Laranja 2 L', 9.50, 400, 'Refrigerante Fanta sabor laranja, garrafa 2 L.', '/uploads/produtos/saborviva-fanta-laranja-2l.png')
) AS v(codigo, nome, preco, estoque, descricao, imagem)
WHERE e.cnpj = '30.004.004/0001-04'
  AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.codigo = v.codigo);

-- ── Status do pedido: 4 etapas manuais (29/10/2026) ──────────────────────────
-- Fluxo: recebido -> preparando -> em_rota -> entregue (ou cancelada).
-- O fornecedor avança cada etapa na mão; nada muda sozinho por tempo.
-- Antes existia 'aguardando_liberacao' como etapa inicial: vira 'recebido'.
UPDATE solicitacoes_compra SET status = 'recebido' WHERE status = 'aguardando_liberacao';
UPDATE pedido               SET status = 'recebido' WHERE status = 'aguardando_liberacao';

-- Trava de integridade: o banco passa a recusar status fora da lista.
-- Antes a coluna era texto livre e aceitava qualquer valor, inclusive erro de digitação.
ALTER TABLE solicitacoes_compra DROP CONSTRAINT IF EXISTS ck_solicitacoes_status;
ALTER TABLE solicitacoes_compra ADD  CONSTRAINT ck_solicitacoes_status
    CHECK (status IN ('recebido', 'preparando', 'em_rota', 'entregue', 'cancelada'));

-- 'pedido' atende os dois fluxos: marketplace (acima) e PDV (aberto/concluido/cancelado).
ALTER TABLE pedido DROP CONSTRAINT IF EXISTS ck_pedido_status;
ALTER TABLE pedido ADD  CONSTRAINT ck_pedido_status
    CHECK (status IN ('recebido', 'preparando', 'em_rota', 'entregue', 'cancelada',
                      'aberto', 'concluido', 'cancelado'));

-- Índices nas chaves estrangeiras mais consultadas (o PostgreSQL não cria sozinho).
CREATE INDEX IF NOT EXISTS idx_solic_comprador   ON solicitacoes_compra(empresa_compradora_id);
CREATE INDEX IF NOT EXISTS idx_solic_fornecedor  ON solicitacoes_compra(empresa_fornecedora_id);
CREATE INDEX IF NOT EXISTS idx_solic_status      ON solicitacoes_compra(status);
CREATE INDEX IF NOT EXISTS idx_itens_solic_solic ON itens_solicitacao_compra(solicitacao_id);
CREATE INDEX IF NOT EXISTS idx_itens_pedido_ped  ON itens_pedido(pedido_id);
CREATE INDEX IF NOT EXISTS idx_pagamentos_pedido ON pagamentos(pedido_id);
