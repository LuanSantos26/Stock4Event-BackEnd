# ⚙️ Stock4event - Backend API

[![Java](https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)

O **Stock4event Backend** é a inteligência e persistência de dados do ecossistema Stock4event. Desenvolvido com **Java 17** e **Spring Boot**, a API REST foi projetada para centralizar e otimizar a gestão operacional de estoques, vendas, eventos e controle financeiro de forma ágil e segura.

Este repositório fornece os dados e validações consumidas pelo [Aplicativo Mobile Stock4event](https://github.com/marcelo435/Mobile).

---

## 👥 Arquitetura e Perfis

A API fornece as permissões e dados para os dois fluxos principais do aplicativo, tratando as regras de negócio de cada ponta:

| Perfil | Tipo no Banco (`empresa.tipo`) | Permissões na API |
| ----- | ----- | ----- |
| **Cliente** | `CLIENTE` | Pode realizar pedidos de compra, montar barracas/quiosques e gerenciar estoques locais. |
| **Fornecedor** | `DISTRIBUIDOR` | Disponibiliza produtos no Marketplace, gerencia catálogo e atualiza status de envios. |
| **Ambos** | `AMBOS` | Acumula as permissões de compra no atacado e venda no varejo. |

---

## 🚀 Funcionalidades

* **🔒 Autenticação e Segurança:** Login via JWT, hash de senhas com BCrypt e controle de acesso aos endpoints.
* **🛍️ Gestão de Pedidos:** Registro de solicitações de compra e pedidos de PDV, interligados com a baixa de produtos.
* **📦 Estoque e Marketplace:** Catálogo de produtos, atualização automática de saldo e listagem de fornecedores disponíveis.
* **🎪 Eventos e Barracas:** Cadastro de quiosques e distribuição do estoque global da empresa para operações de eventos.
* **📊 Módulo Financeiro:** Cálculos de lucro, margem, histórico de vendas e consultas agrupadas por período.
* **🚚 Logística:** Gestão de caminhoneiros e motoristas, rotas, e simulação de linha do tempo de entrega baseada em status.
* **💳 Meios de Pagamento:** Gestão de cartões (mascarados) e chaves Pix salvas por empresa.
* **📁 Uploads:** Rota de upload e armazenamento estático local de imagens de produtos e logotipos.

---

## 🛠️ Tecnologias Utilizadas

* **Linguagem:** Java 17
* **Framework Principal:** Spring Boot 3.4.0
* **Persistência:** Spring Data JPA / Hibernate
* **Banco de Dados:** PostgreSQL
* **Segurança:** Spring Security + JWT (Biblioteca `jjwt`)
* **Documentação:** Swagger / SpringDoc OpenAPI
* **Build e Gerenciamento:** Maven

---

## 📁 Estrutura do Projeto

```text
📦 backend
 ┣ 📂 src/main/java/com/quickstock/backend/
 ┃ ┣ 📂 config/      # Inicializadores (DemoSeedRunner), CORS e WebConfig
 ┃ ┣ 📂 controller/  # Endpoints REST expostos para o aplicativo
 ┃ ┣ 📂 dto/         # Objetos de transferência de dados (Request/Response)
 ┃ ┣ 📂 entity/      # Entidades mapeadas para o banco de dados (JPA)
 ┃ ┣ 📂 exception/   # Tratamento centralizado de erros (GlobalExceptionHandler)
 ┃ ┣ 📂 repository/  # Interfaces do Spring Data para acesso ao PostgreSQL
 ┃ ┗ 📂 service/     # Regras de negócio, cálculos financeiros e validações
 ┣ 📂 src/main/resources/
 ┃ ┣ 📂 static/uploads/        # Diretório de armazenamento de imagens
 ┃ ┣ 📜 application.properties # Configurações de banco, porta e JWT
 ┃ ┗ 📜 data.sql               # Script de carga inicial e migrações
 ┗ 📜 pom.xml                  # Dependências do projeto Maven
```

---

## ⚙️ Pré-requisitos

* Java 17 (JDK) instalado
* PostgreSQL 14 ou superior (Rodando na porta padrão 5432)
* Git

---

## 🚀 Como Executar o Projeto

**1. Clone o repositório**

```bash
git clone [https://github.com/luansantos26/quickstock-backend.git](https://github.com/luansantos26/quickstock-backend.git)
cd quickstock-backend
```

**2. Crie o banco de dados**

Acesse o seu PostgreSQL (via pgAdmin, DBeaver ou terminal) e crie o banco vazio:

```sql
CREATE DATABASE quickstock;
```

**3. Verifique as credenciais do banco**

As configurações estão no arquivo `src/main/resources/application.properties`. Por padrão, espera-se que o usuário seja `postgres` e a senha `admin` (ou `sua_senha`). Altere se necessário.

**4. Inicie o servidor da API**

Utilize o Maven Wrapper incluso no projeto:

**No Windows:**
```bash
mvnw.cmd spring-boot:run
```

**No Linux/macOS:**
```bash
./mvnw spring-boot:run
```

O servidor iniciará e a API estará disponível na porta `8081`.

---

### 🔑 Conta de Demonstração e Dados (Seeders)

Ao iniciar a aplicação pela primeira vez, o Hibernate cria as tabelas e o `data.sql` as popula.
Em seguida, o componente `DemoSeedRunner` cria automaticamente a conta principal para você acessar o app:

* **E-mail:** `demo@quickstock.com`
* **Senha:** `demo1234`

Essa conta já possui cartões, PIX, endereços e permite transitar entre as telas de Cliente e Fornecedor. Além dela, 3 distribuidoras já vêm cadastradas com catálogo de produtos e imagens no Marketplace.

---

## 📑 Documentação dos Endpoints (Swagger)

A API possui documentação viva gerada automaticamente. Com o servidor rodando, acesse no seu navegador:

* **Interface Interativa:** `http://localhost:8081/swagger-ui/index.html`
* **Definição JSON:** `http://localhost:8081/v3/api-docs`

---

## 💡 Notas de Desenvolvimento

* **Uploads de Imagens:** As imagens enviadas pelo app não vão para um S3, elas ficam armazenadas estaticamente na pasta `src/main/resources/static/uploads/`.
* **Avanço Automático de Pedidos:** Para facilitar a demonstração do app, os pedidos mudam de status sozinhos baseados no tempo (ex: de "Aguardando" para "Em rota" após 20 segundos). Isso é controlado pela flag `quickstock.pedidos.status-automatico=true` no `application.properties`.
* **Mapeamento DTO:** Evitamos retornar entidades puras nos Controllers para não expor a senha em hash ou gerar loops infinitos nas relações JPA.

---

## 👨‍💻 Equipe

* Luan Feitosa Santos
* Jose Italo Silva Cordeiro Dantas
* Marcelo Vitor Viana da Silva
* Leticia Viviane Pereira da Silva
* José Lucas Luiz da Silva

---

## 📄 Licença

Este projeto é destinado a fins acadêmicos e de aprendizado, podendo ser expandido para utilização comercial mediante adequações futuras.
