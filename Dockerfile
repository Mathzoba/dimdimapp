# =====================================================================
# Dockerfile - Projeto DimDimApp (CP3 - DevOps Tools & Cloud Computing)
# Imagem personalizada da aplicacao (API RESTful Java + Spring Boot)
# Build em multiplos estagios: compila com Maven e roda numa imagem enxuta.
# =====================================================================

# --------- ESTAGIO 1: BUILD (compila o projeto e gera o .jar) ---------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copia primeiro o pom.xml e baixa as dependencias (aproveita o cache).
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o codigo-fonte e gera o .jar (sem rodar os testes no build).
COPY src ./src
RUN mvn clean package -DskipTests

# --------- ESTAGIO 2: EXECUCAO (imagem final, leve) ------------------
FROM eclipse-temurin:17-jre

# ENV: variaveis de ambiente acessiveis durante a execucao do container.
# (Requisito 03 - "Utilizar variaveis de ambiente - pelo menos uma")
ENV APP_PORT=8080 \
    DB_HOST=db-dimdim \
    DB_PORT=5432 \
    DB_USER=dimdim \
    DB_PASSWORD=dimdim123 \
    DB_NAME=dimdimdb

# WORKDIR: diretorio de trabalho dentro do container.
# (Requisito 03 - "Definir um diretorio de trabalho - nomeacao livre")
WORKDIR /opt/dimdimapp

# Cria um usuario e grupo NAO-ROOT chamado "dimdimuser".
# (Requisito 03 - "Executar com um usuario nao root")
RUN groupadd --system dimdimgroup \
    && useradd --system --gid dimdimgroup dimdimuser

# Copia apenas o .jar gerado no estagio de build.
COPY --from=builder /build/target/dimdimapp.jar app.jar

# Garante que o usuario nao-root seja dono dos arquivos da aplicacao.
RUN chown -R dimdimuser:dimdimgroup /opt/dimdimapp

# USER: a partir daqui o container roda como usuario nao-root.
USER dimdimuser

# EXPOSE: documenta a porta em que a aplicacao escuta.
EXPOSE 8080

# CMD: comando padrao executado quando o container inicia.
CMD ["java", "-jar", "app.jar"]
