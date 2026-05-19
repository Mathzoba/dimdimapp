# DimDimApp — CP3 DevOps Tools & Cloud Computing

Ambiente conteinerizado da Instituicao Financeira **DimDim**, construido com
**Dockerfile** e boas praticas de automacao, isolamento e persistencia de dados.

O projeto sobe **dois containers** numa rede Docker dedicada:

| Container | Funcao | Imagem |
|-----------|--------|--------|
| `db-dimdim-RM556649` | Banco de dados PostgreSQL com **volume nomeado** | Imagem publica `postgres:16-alpine` |
| `app-dimdim-RM556649` | API RESTful (Java + Spring Boot) com CRUD | **Imagem personalizada** via Dockerfile |

---

## Tecnologias

- Java 17 + Spring Boot 3 (Spring Web + Spring Data JPA)
- PostgreSQL 16 (banco de dados)
- Maven (build) — executado dentro do Dockerfile
- Dockerfile / Docker
- Git e GitHub

---

## Estrutura do projeto

```
dimdimapp/
├── Dockerfile                 # imagem personalizada da aplicacao (multi-stage)
├── pom.xml                    # dependencias e build Maven
├── README.md                  # este arquivo (How to)
└── src/main/
    ├── java/br/com/fiap/dimdimapp/
    │   ├── DimDimAppApplication.java     # classe principal
    │   ├── controller/
    │   │   ├── ClienteController.java    # CRUD REST
    │   │   └── HealthController.java     # /health
    │   ├── model/Cliente.java            # entidade JPA (tabela clientes)
    │   └── repository/ClienteRepository.java
    └── resources/application.properties  # config via variaveis de ambiente
```

---

## Pre-requisitos

- Uma VM Linux **em nuvem** — Azure (a VM Linux do material). **NAO use localhost** (penalidade: zera o CP)
- Docker instalado na VM
- Portas liberadas no Network Security Group (NSG) da Azure:
  - `22` (SSH) e `8080` (aplicacao web)

### Instalar o Docker na VM Azure (Ubuntu/Debian)

```bash
sudo apt-get update
sudo apt-get install -y docker.io
sudo systemctl enable --now docker
sudo usermod -aG docker $USER   # depois faca logout/login
```

> Se a VM Azure for AlmaLinux (como no material dos CPs anteriores):
> ```bash
> sudo dnf install -y docker
> sudo systemctl enable --now docker
> ```

---

## How to — passo a passo (do clone ate os testes)

### 1. Clonar o repositorio na VM em nuvem

```bash
git clone https://github.com/SEU-USUARIO/dimdimapp.git
cd dimdimapp
```

### 2. Criar a rede Docker

Ambos os containers vao se comunicar por esta rede (requisito 02).

```bash
docker network create dimdim-network
```

### 3. Criar o volume nomeado do banco

Garante que os dados dos correntistas **persistam** mesmo que o container
do banco seja removido (requisito 01 / penalidade -2,0 se faltar).

```bash
docker volume create dimdim-db-data
```

### 4. Subir o container do Banco de Dados (imagem publica)

Roda em **background** (`-d`), com o **RM no nome** e o **volume nomeado**.

```bash
docker run -d \
  --name db-dimdim-RM000000 \
  --network dimdim-network \
  -v dimdim-db-data:/var/lib/postgresql/data \
  -e POSTGRES_USER=dimdim \
  -e POSTGRES_PASSWORD=dimdim123 \
  -e POSTGRES_DB=dimdimdb \
  postgres:16-alpine
```

### 5. Construir a imagem personalizada da aplicacao (Dockerfile)

O Dockerfile faz o build com Maven internamente — nao precisa instalar
Maven nem Java na VM.

```bash
docker build -t dimdimapp-imagem .
```

### 6. Subir o container da Aplicacao

Roda em **background**, na **mesma rede**, com o **RM no nome**.
A variavel `DB_HOST` aponta para o **nome do container do banco**
(DNS interno da rede Docker — nunca `localhost`).

```bash
docker run -d \
  --name app-dimdim-RM000000 \
  --network dimdim-network \
  -p 8080:8080 \
  -e DB_HOST=db-dimdim-RM000000 \
  dimdimapp-imagem
```

### 7. Verificar se os dois containers estao no ar

```bash
docker ps
```

Acompanhe a subida da aplicacao (o Spring Boot leva alguns segundos):

```bash
docker logs -f app-dimdim-RM000000
```

---

## Testes — evidencias do CRUD

A aplicacao expoe um CRUD completo sobre a tabela `clientes`.
Teste a API via terminal com `curl` (troque `<IP-PUBLICO>` pelo IP da VM):

```bash
# CREATE - cadastra cliente
curl -X POST http://<IP-PUBLICO>:8080/clientes \
  -H "Content-Type: application/json" \
  -d '{"nome":"Joao Silva","email":"joao@dimdim.com","saldo":1500.00}'

# READ - lista todos os clientes
curl http://<IP-PUBLICO>:8080/clientes

# READ - busca por id
curl http://<IP-PUBLICO>:8080/clientes/1

# UPDATE - atualiza um cliente
curl -X PUT http://<IP-PUBLICO>:8080/clientes/1 \
  -H "Content-Type: application/json" \
  -d '{"saldo":2000.00}'

# DELETE - remove um cliente
curl -X DELETE http://<IP-PUBLICO>:8080/clientes/1
```

| Metodo | Rota | Operacao |
|--------|------|----------|
| POST   | `/clientes`      | Create |
| GET    | `/clientes`      | Read (lista) |
| GET    | `/clientes/{id}` | Read (um) |
| PUT    | `/clientes/{id}` | Update |
| DELETE | `/clientes/{id}` | Delete |

---

## Evidencias exigidas pelo CP

### Requisito 06 — acessar os containers com `docker container exec`

```bash
# Container da aplicacao: estrutura de diretorios e usuario conectado
docker container exec -it app-dimdim-RM000000 sh
ls
pwd
whoami        # deve retornar "dimdimuser" (usuario NAO-root)
exit
```

### Penalidade 02 — evidenciar cada operacao por SELECT direto no Banco

Apos cada operacao (CREATE / UPDATE / DELETE) do CRUD, conecte-se ao
banco e rode um SELECT para comprovar a alteracao:

```bash
docker container exec -it db-dimdim-RM000000 \
  psql -U dimdim -d dimdimdb -c "SELECT * FROM clientes;"
```

### Comprovar a persistencia de dados (volume nomeado)

```bash
# 1. Cadastre alguns clientes pela API
# 2. Remova e recrie o container do banco:
docker rm -f db-dimdim-RM000000
docker run -d --name db-dimdim-RM000000 --network dimdim-network \
  -v dimdim-db-data:/var/lib/postgresql/data \
  -e POSTGRES_USER=dimdim -e POSTGRES_PASSWORD=dimdim123 \
  -e POSTGRES_DB=dimdimdb postgres:16-alpine
# 3. Rode o SELECT de novo: os dados continuam la (graças ao volume).
docker container exec -it db-dimdim-RM000000 \
  psql -U dimdim -d dimdimdb -c "SELECT * FROM clientes;"
```

---

## Como os requisitos sao atendidos

| Requisito | Onde |
|-----------|------|
| Dois containers (banco + app) | passos 4 e 6 |
| Banco com volume nomeado | `dimdim-db-data` (passo 3) |
| App com CRUD e tabela `clientes` | `ClienteController.java` + `Cliente.java` |
| Mesma rede Docker | `dimdim-network` (passo 2) |
| Usuario nao-root | `dimdimuser` no Dockerfile |
| Diretorio de trabalho | `WORKDIR /opt/dimdimapp` |
| Variavel de ambiente | `ENV` no Dockerfile + `-e` no run |
| Dockerfile e imagem personalizada | `Dockerfile` + `dimdimapp-imagem` |
| Containers em background | flag `-d` |
| RM no nome dos containers | `-RM000000` |
| Execucao em nuvem | VM Azure Linux |

---

## Limpeza do ambiente

```bash
docker rm -f app-dimdim-RM000000 db-dimdim-RM000000
docker network rm dimdim-network
docker volume rm dimdim-db-data   # CUIDADO: apaga os dados persistidos
```
