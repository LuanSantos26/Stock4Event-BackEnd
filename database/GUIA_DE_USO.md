# Guia de uso do banco Stock4event (PostgreSQL)

Este guia ensina a ligar, consultar, fazer backup e recriar o banco. Os comandos funcionam em Linux/macOS e no Git Bash do Windows.

## 1. O que tem nesta pasta

| Arquivo | Para que serve |
|---|---|
| `01_schema.sql` | Cria as 19 tabelas, chaves primárias, estrangeiras e únicas |
| `02_seed_catalogo.sql` | 4 distribuidoras parceiras (Casa dos Vinhos, Cervejaria Caruaru, Whisky Labs e SaborViva) e 40 produtos (é o `data.sql` do backend, com os ajustes explicados na seção 8) |
| `03_seed_demo.sql` | Empresa e usuário de demonstração, endereço, cartão, PIX e formas de pagamento |
| `build_from_scratch.sh` | Recria o banco do zero rodando os 3 arquivos acima |
| `dump_stock4event.sh` | Gera backup (SQL completo, só estrutura e formato compacto) |
| `restore_stock4event.sh` | Restaura um backup SQL em um banco novo |
| `gerar_er.py` | Regera o diagrama ER a partir do banco real |
| `stock4event_er.png / .svg / .dot` | Diagrama ER (PNG para olhar, SVG para ampliar, DOT é o código-fonte) |
| `stock4event_full.sql` | Dump completo (estrutura + dados) do banco oficial, restaurável com `restore_stock4event.sh` |
| `backups/` | Onde o `dump_stock4event.sh` grava os backups (fica fora do git) |

**Login de demonstração:** `demo@stock4event.com` / `demo1234`

## 2. Ligar o PostgreSQL

Windows: o serviço "postgresql-x64-18" (ou a versão instalada) em Serviços. Linux: `sudo systemctl start postgresql`.

No Windows, o `psql` fica em `C:\Program Files\PostgreSQL\18\bin`. Para usar os scripts `.sh` no Git Bash, coloque essa pasta no PATH:

```bash
export PATH="/c/Program Files/PostgreSQL/18/bin:$PATH"
```

## 3. Conectar com o psql

```bash
export PGPASSWORD='a_senha_do_application.properties'
psql -h localhost -U postgres -d stock4event
```

Dentro do `psql`, os comandos com barra são do próprio programa:

| Comando | O que faz |
|---|---|
| `\dt` | Lista as tabelas |
| `\d produtos` | Mostra colunas, chaves e FKs de uma tabela |
| `\d+ pedido` | Igual, com mais detalhes |
| `\l` | Lista os bancos |
| `\c outro_banco` | Troca de banco |
| `\x` | Liga/desliga exibição vertical (bom para tabelas largas) |
| `\q` | Sai |

SQL termina sempre com `;`.

## 4. Consultas para começar

```sql
-- Quantas linhas em cada tabela
SELECT relname AS tabela, n_live_tup AS linhas
FROM pg_stat_user_tables ORDER BY relname;

-- Produtos de cada distribuidora (JOIN pela FK produtos.empresa_id)
SELECT e.nome AS empresa, p.nome AS produto, p.preco_venda, p.estoque
FROM produtos p
JOIN empresas e ON e.id = p.empresa_id
WHERE p.ativo = 1
ORDER BY e.nome, p.nome;

-- Usuário, perfil e empresa (duas FKs)
SELECT u.nome, u.email, pf.nome AS perfil, e.nome AS empresa
FROM usuarios u
JOIN perfis pf ON pf.id = u.perfil_id
JOIN empresas e ON e.id = u.empresa_id;

-- Total vendido por empresa fornecedora (quando houver pedidos)
SELECT e.nome, COUNT(*) AS pedidos, SUM(p.valor_total) AS total
FROM pedido p JOIN empresas e ON e.id = p.empresa_fornecedora_id
WHERE p.status <> 'cancelado'
GROUP BY e.nome;
```

### Novidades da estrutura (22/10/2026)

| Onde | O que mudou | Para que serve |
|---|---|---|
| `usuarios.foto_url` | Coluna nova, `varchar(500)`, opcional | Foto de perfil do usuário (tela *Dados pessoais* do cliente) |
| `empresas.descricao`, `logo_url`, `capa_url` | Já existiam; agora o próprio fornecedor edita | Tela *Perfil da loja*: descrição, foto e capa exibidas aos clientes |
| `email_boas_vindas` | Tabela nova (19ª) | Registro de cada e-mail de boas-vindas enviado após o cadastro |

A tabela `email_boas_vindas` guarda um registro por conta: `usuario_id` é único, o que impede dois
e-mails de boas-vindas para a mesma pessoa. O vínculo com `usuarios` é lógico (sem chave estrangeira),
por isso ela aparece sem ligações no diagrama ER. Colunas principais:

| Coluna | Conteúdo |
|---|---|
| `tipo` | `CLIENTE` ou `FORNECEDOR` (versão do e-mail, pelo perfil salvo) |
| `status` | `PENDENTE`, `ENVIANDO`, `ENVIADO`, `FALHOU` ou `INDETERMINADO` |
| `tentativas` / `proxima_tentativa_em` | Controle das novas tentativas após falhas temporárias |
| `ultimo_erro` | Motivo da última falha |
| `enviado_em` | Quando o servidor de e-mail aceitou a mensagem |

As imagens de foto, logo e capa ficam em arquivos na pasta `uploads/` do backend; o banco guarda só o
caminho (ex.: `/uploads/usuarios/<uuid>.jpg`). Detalhes do e-mail: `docs/EMAIL_BOAS_VINDAS.md`.

```sql
-- Situação dos e-mails de boas-vindas
SELECT usuario_id, destinatario, tipo, status, tentativas, ultimo_erro, enviado_em
FROM email_boas_vindas ORDER BY id DESC;

-- Quantos por status
SELECT status, COUNT(*) FROM email_boas_vindas GROUP BY status;
```

### Status do pedido: 4 etapas manuais (29/10/2026)

O pedido do marketplace percorre quatro etapas, **sempre pela mão do fornecedor**. Nada avança
sozinho por tempo: a demonstração que pulava para "em rota" em 20 s e "entregue" em 55 s foi removida.

| Código no banco | O que o app mostra | Quem aciona |
|---|---|---|
| `recebido` | Pedido recebido | status inicial, gravado quando o pedido é criado |
| `preparando` | Preparando pedido | fornecedor, botão **Preparar pedido** |
| `em_rota` | Pedido saiu para a entrega | fornecedor, botão **Despachar** |
| `entregue` | Entregue | fornecedor, botão **Confirmar entrega** |
| `cancelada` | Pedido cancelado | fornecedor a qualquer momento antes da entrega; comprador só até `preparando` |

A ordem é obrigatória: não dá para pular etapa (de `recebido` direto para `entregue`) nem voltar atrás.
`entregue` e `cancelada` encerram o pedido. Ao entregar, o estoque do comprador é creditado; ao
cancelar, o estoque volta ao fornecedor e o pagamento é estornado.

As colunas `solicitacoes_compra.status` e `pedido.status` agora têm **CHECK**: o banco recusa qualquer
valor fora da lista, inclusive erro de digitação. Antes eram texto livre. A coluna `pedido.status`
aceita também os status do PDV (`aberto`, `concluido`, `cancelado`), porque a tabela atende os dois fluxos.

Quem já tem banco antigo não precisa fazer nada: ao subir o backend, o `data.sql` troca
`aguardando_liberacao` por `recebido`, cria os CHECKs e os índices das chaves estrangeiras.

```sql
-- Em que etapa está cada pedido
SELECT id, status, valor_total, criado_em FROM solicitacoes_compra ORDER BY id DESC;

-- Quantos pedidos em cada etapa
SELECT status, COUNT(*) FROM solicitacoes_compra GROUP BY status ORDER BY 1;
```

## 5. Backup e restauração

```bash
export PGPASSWORD='sua_senha'

# Gerar backup (cria a pasta ./backups com 3 arquivos)
./dump_stock4event.sh

# Restaurar em um banco novo (pede confirmação se o banco já existir)
./restore_stock4event.sh backups/stock4event_full_AAAAMMDD_HHMMSS.sql stock4event_copia
```

Regra de ouro: **faça um dump antes de qualquer mudança** na estrutura ou nos dados.

## 6. Recriar o banco do zero

```bash
export PGPASSWORD='sua_senha'
./build_from_scratch.sh                 # recria o banco 'stock4event'
./build_from_scratch.sh stock4event_teste   # ou em outro nome, para testar sem risco
```

Atenção: esse script **apaga** o banco de destino se ele já existir.

## 7. Regerar o diagrama ER

Precisa de Graphviz (`dot`) instalado.

```bash
export PGPASSWORD='sua_senha'
python3 gerar_er.py stock4event .
```

Se você mudar o banco, rode de novo e o diagrama acompanha.

## 8. Ligação com o backend e cuidados importantes

O `application.properties` do backend já aponta para este banco (`localhost:5432/stock4event`, usuário `postgres`). Então basta subir o backend com o banco ligado.

Há dois jeitos de ter o banco pronto, e os dois chegam ao mesmo resultado:

1. **Pelos scripts desta pasta:** `build_from_scratch.sh` (ou restaurar o `stock4event_full.sql`).
2. **Pelo próprio backend:** crie o banco vazio (`CREATE DATABASE stock4event;`) e suba o backend. O Hibernate cria as tabelas, o `data.sql` cadastra o catálogo e os runners criam a conta demo.

Ao subir, o backend também ajusta o banco: troca os códigos provisórios `LEG-<id>` dos produtos pelos códigos oficiais `MKT-...` (`ProdutoCatalogoFixRunner`) e cria colunas e tabelas novas das entidades (`ddl-auto=update`). É assim que um banco antigo recebe `usuarios.foto_url` e a tabela `email_boas_vindas` sem precisar rodar nenhum script.

**Correção feita no backend:** o `data.sql` falhava em banco **vazio**, porque inseria produtos sem o campo `codigo`, que é `NOT NULL`. Agora cada `INSERT` de produto informa o código oficial do catálogo. O `02_seed_catalogo.sql` continua usando um gatilho temporário para preencher `codigo`, então também funciona sozinho.

**Integração testada:** backend subindo num banco vazio e no banco restaurado deste dump, com login demo, `/api/usuarios/me`, marketplace, endereços, formas de pagamento, cartões, PIX, notificações, financeiro, quiosques e pedidos respondendo 200.

## 9. Interface gráfica (opcional)

Se preferir clicar em vez de digitar, use DBeaver ou pgAdmin com estes dados de conexão:

| Campo | Valor |
|---|---|
| Host | `localhost` |
| Porta | `5432` |
| Banco | `stock4event` |
| Usuário | `postgres` |
| Senha | a do `application.properties` |

No DBeaver, clique com o botão direito no banco e escolha **View Diagram** para ver o ER direto na ferramenta.

## 10. Cuidados

- Excluir um registro "pai" que tem "filhos" dá erro 23503. É a proteção das chaves estrangeiras funcionando. Veja a seção 5 do documento do modelo.
- Nunca rode `DROP` ou `DELETE` sem `WHERE` num banco que importa. Teste primeiro dentro de uma transação: `BEGIN; ...comando...; ROLLBACK;`
- Não suba a senha do banco, o segredo JWT nem a senha do servidor de e-mail (SMTP) para o GitHub.
- **Codificação no Windows:** os scripts `.sql` estão em UTF-8 e começam com `SET client_encoding = 'UTF8';`.
  Sem isso, o `psql` do Windows lê o arquivo como WIN1252: o script pode parar com
  `0x81 ... não tem equivalente na codificação "UTF8"` ou, pior, gravar acentos corrompidos (`Ã©` no lugar
  de `é`). Os scripts `.sh` também definem `PGCLIENTENCODING=UTF8`. Ao criar um `.sql` novo, mantenha
  essa primeira linha. (O `data.sql` do backend não precisa dela, porque é executado pelo Java.)
