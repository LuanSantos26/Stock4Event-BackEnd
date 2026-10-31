INSERT INTO perfis (nome, descricao)
SELECT 'Admin', 'Administrador da empresa'
WHERE NOT EXISTS (SELECT 1 FROM perfis WHERE LOWER(nome) = 'admin');

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS perfil_uso VARCHAR(30);
UPDATE usuarios SET perfil_uso = 'Fornecedor' WHERE perfil_uso IS NULL OR perfil_uso = '';

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS cpf VARCHAR(14);

-- ── Coluna de estoque (migração idempotente) ─────────────────────────────────
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS estoque NUMERIC(10, 3);
UPDATE produtos SET estoque = 0 WHERE estoque IS NULL;

-- ── Código fixo por produto ──────────────────────────────────────────────────
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS codigo VARCHAR(40);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS codigo_origem VARCHAR(40);
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
       'Vinhos nacionais e importados selecionados para bares, restaurantes e revenda.',
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
  descricao = 'Vinhos nacionais e importados selecionados para bares, restaurantes e revenda.',
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

-- ── Casa dos Vinhos — produtos ───────────────────────────────────────────────
INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo)
SELECT e.id, 'Vinho Mazzei Zisola Sicilia Noto Rosso 750ml', 156.00, 'UN',
       'Vinho tinto siciliano Noto Rosso, garrafa 750 ml.',
       '/uploads/produtos/vinho-mazzei-zisola.png', 1
FROM empresas e WHERE e.cnpj = '30.001.001/0001-01'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Vinho Mazzei Zisola Sicilia Noto Rosso 750ml');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo)
SELECT e.id, 'Vinho Louis Latour Bourgogne Chardonnay 750 ml', 200.00, 'UN',
       'Vinho branco Bourgogne Chardonnay, garrafa 750 ml.',
       '/uploads/produtos/vinho-louis-latour.png', 1
FROM empresas e WHERE e.cnpj = '30.001.001/0001-01'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Vinho Louis Latour Bourgogne Chardonnay 750 ml');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo)
SELECT e.id, 'Vinho Naturelle Tinto Reserva 750 ML', 60.00, 'UN',
       'Vinho tinto reserva Naturelle, garrafa 750 ml.',
       '/uploads/produtos/vinho-naturelle-tinto-reserva.png', 1
FROM empresas e WHERE e.cnpj = '30.001.001/0001-01'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Vinho Naturelle Tinto Reserva 750 ML');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo)
SELECT e.id, 'Vinho Carmin De Peumo Carmenere 750 ml', 666.00, 'UN',
       'Vinho tinto Carmenere Carmin De Peumo, garrafa 750 ml.',
       '/uploads/produtos/vinho-carmin-de-peumo.png', 1
FROM empresas e WHERE e.cnpj = '30.001.001/0001-01'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Vinho Carmin De Peumo Carmenere 750 ml');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo)
SELECT e.id, 'Vinho Carolina Reserva Sauvignon Blanc 750 ml', 60.00, 'UN',
       'Vinho branco Sauvignon Blanc reserva Carolina, garrafa 750 ml.',
       '/uploads/produtos/vinho-carolina-sauvignon-blanc.png', 1
FROM empresas e WHERE e.cnpj = '30.001.001/0001-01'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Vinho Carolina Reserva Sauvignon Blanc 750 ml');

UPDATE produtos SET preco_venda = 156.00, ativo = 1,
  imagem_url = '/uploads/produtos/vinho-mazzei-zisola.png'
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.001.001/0001-01'
  AND produtos.nome = 'Vinho Mazzei Zisola Sicilia Noto Rosso 750ml';

UPDATE produtos SET preco_venda = 200.00, ativo = 1,
  imagem_url = '/uploads/produtos/vinho-louis-latour.png'
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.001.001/0001-01'
  AND produtos.nome = 'Vinho Louis Latour Bourgogne Chardonnay 750 ml';

UPDATE produtos SET preco_venda = 60.00, ativo = 1,
  imagem_url = '/uploads/produtos/vinho-naturelle-tinto-reserva.png'
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.001.001/0001-01'
  AND produtos.nome = 'Vinho Naturelle Tinto Reserva 750 ML';

UPDATE produtos SET preco_venda = 666.00, ativo = 1,
  imagem_url = '/uploads/produtos/vinho-carmin-de-peumo.png'
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.001.001/0001-01'
  AND produtos.nome = 'Vinho Carmin De Peumo Carmenere 750 ml';

UPDATE produtos SET preco_venda = 60.00, ativo = 1,
  imagem_url = '/uploads/produtos/vinho-carolina-sauvignon-blanc.png'
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.001.001/0001-01'
  AND produtos.nome = 'Vinho Carolina Reserva Sauvignon Blanc 750 ml';

-- ── Cervejaria Caruaru — produtos ────────────────────────────────────────────
INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo)
SELECT e.id, 'Cerveja Heineken Long Neck 330ml', 5.99, 'UN',
       'Cerveja Heineken long neck 330 ml.',
       '/uploads/produtos/cerveja-heineken-long-neck.png', 1
FROM empresas e WHERE e.cnpj = '30.002.002/0001-02'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Cerveja Heineken Long Neck 330ml');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo)
SELECT e.id, 'Cerveja Corona Long Neck', 6.99, 'UN',
       'Cerveja Corona Extra long neck 330 ml.',
       '/uploads/produtos/cerveja-corona-long-neck.png', 1
FROM empresas e WHERE e.cnpj = '30.002.002/0001-02'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Cerveja Corona Long Neck');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo)
SELECT e.id, 'Cerveja Amstel Lata 269ml', 3.19, 'UN',
       'Cerveja Amstel lata 269 ml.',
       '/uploads/produtos/cerveja-amstel-lata.png', 1
FROM empresas e WHERE e.cnpj = '30.002.002/0001-02'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Cerveja Amstel Lata 269ml');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo)
SELECT e.id, 'Cerveja Cerpa Export Long Neck 350ml', 8.39, 'UN',
       'Cerveja Cerpa Export long neck 350 ml.',
       '/uploads/produtos/cerveja-cerpa-export-long-neck.png', 1
FROM empresas e WHERE e.cnpj = '30.002.002/0001-02'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Cerveja Cerpa Export Long Neck 350ml');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo)
SELECT e.id, 'Cerveja Paulaner Munchen Weissbier 500ml', 11.19, 'UN',
       'Cerveja Paulaner Munchen Weissbier garrafa 500 ml.',
       '/uploads/produtos/cerveja-paulaner-munchen-weissbier.png', 1
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
INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo)
SELECT e.id, 'Whisky Escocês Royal Salute 21 Anos The Signature Blend 700ml', 989.00, 'UN',
       'Whisky escocês Royal Salute 21 anos, garrafa 700 ml.',
       '/uploads/produtos/whisky-royal-salute-21.png', 1
FROM empresas e WHERE e.cnpj = '30.003.003/0001-03'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Whisky Escocês Royal Salute 21 Anos The Signature Blend 700ml');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo)
SELECT e.id, 'Whisky The Balvenie PortWood 21 Anos 700ml Single Malt Escocês', 2795.30, 'UN',
       'Whisky single malt The Balvenie PortWood 21 anos, garrafa 700 ml.',
       '/uploads/produtos/whisky-balvenie-portwood-21.png', 1
FROM empresas e WHERE e.cnpj = '30.003.003/0001-03'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Whisky The Balvenie PortWood 21 Anos 700ml Single Malt Escocês');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo)
SELECT e.id, 'Whisky Glenfiddich Single Malt 12 Anos 750ml Escocês', 415.75, 'UN',
       'Whisky single malt Glenfiddich 12 anos, garrafa 750 ml.',
       '/uploads/produtos/whisky-glenfiddich-12.png', 1
FROM empresas e WHERE e.cnpj = '30.003.003/0001-03'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Whisky Glenfiddich Single Malt 12 Anos 750ml Escocês');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo)
SELECT e.id, 'Conhaque Martell L''Or de Jean Martell 700ml', 18711.00, 'UN',
       'Conhaque Martell L''Or de Jean Martell, garrafa 700 ml.',
       '/uploads/produtos/conhaque-martell-lor.png', 1
FROM empresas e WHERE e.cnpj = '30.003.003/0001-03'
AND NOT EXISTS (SELECT 1 FROM produtos p WHERE p.empresa_id = e.id AND p.nome = 'Conhaque Martell L''Or de Jean Martell 700ml');

INSERT INTO produtos (empresa_id, nome, preco_venda, unidade, descricao, imagem_url, ativo)
SELECT e.id, 'Whisky Suntory Hibiki Japanese Harmony 700ml | Blended Premium Japonês', 729.90, 'UN',
       'Whisky japonês Suntory Hibiki Japanese Harmony, garrafa 700 ml.',
       '/uploads/produtos/whisky-hibiki-japanese-harmony.png', 1
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

-- Desativa produtos extras dos 3 parceiros (catálogo oficial = 5 por loja)
UPDATE produtos SET ativo = 0
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.001.001/0001-01'
  AND produtos.nome NOT IN (
    'Vinho Mazzei Zisola Sicilia Noto Rosso 750ml',
    'Vinho Louis Latour Bourgogne Chardonnay 750 ml',
    'Vinho Naturelle Tinto Reserva 750 ML',
    'Vinho Carmin De Peumo Carmenere 750 ml',
    'Vinho Carolina Reserva Sauvignon Blanc 750 ml'
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
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.001.001/0001-01'
  AND produtos.nome = 'Vinho Naturelle Tinto Reserva 750 ML'
  AND (produtos.estoque IS NULL OR (produtos.estoque = 0 AND NOT EXISTS (
    SELECT 1 FROM itens_solicitacao_compra i WHERE i.produto_id = produtos.id)));

UPDATE produtos SET estoque = 200
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.001.001/0001-01'
  AND produtos.nome = 'Vinho Carolina Reserva Sauvignon Blanc 750 ml'
  AND (produtos.estoque IS NULL OR (produtos.estoque = 0 AND NOT EXISTS (
    SELECT 1 FROM itens_solicitacao_compra i WHERE i.produto_id = produtos.id)));

UPDATE produtos SET estoque = 150
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.001.001/0001-01'
  AND produtos.nome = 'Vinho Mazzei Zisola Sicilia Noto Rosso 750ml'
  AND (produtos.estoque IS NULL OR (produtos.estoque = 0 AND NOT EXISTS (
    SELECT 1 FROM itens_solicitacao_compra i WHERE i.produto_id = produtos.id)));

UPDATE produtos SET estoque = 100
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.001.001/0001-01'
  AND produtos.nome = 'Vinho Louis Latour Bourgogne Chardonnay 750 ml'
  AND (produtos.estoque IS NULL OR (produtos.estoque = 0 AND NOT EXISTS (
    SELECT 1 FROM itens_solicitacao_compra i WHERE i.produto_id = produtos.id)));

UPDATE produtos SET estoque = 80
FROM empresas e WHERE produtos.empresa_id = e.id AND e.cnpj = '30.001.001/0001-01'
  AND produtos.nome = 'Vinho Carmin De Peumo Carmenere 750 ml'
  AND (produtos.estoque IS NULL OR (produtos.estoque = 0 AND NOT EXISTS (
    SELECT 1 FROM itens_solicitacao_compra i WHERE i.produto_id = produtos.id)));

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
