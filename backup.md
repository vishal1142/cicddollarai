C:\Users\visha\full cicd\docker-compose.cicd.yml

version: '3.8'

services:
  # Jenkins CI Server
  ci-jenkins:
    image: jenkins/jenkins:lts-jdk11
    container_name: my-jenkins-server
    restart: unless-stopped
    ports:
      - "8080:8080"
      - "50000:50000"
    volumes:
      - jenkins-data:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
    environment:
      - JAVA_OPTS=-Djenkins.install.runSetupWizard=false
    networks:
      - my-ci-network

  # SonarQube Code Analysis
  code-analyzer:
    image: sonarqube:lts-community
    container_name: my-sonarqube
    restart: unless-stopped
    depends_on:
      - sonar-db
    environment:
      - SONAR_JDBC_URL=jdbc:postgresql://sonar-db:5432/sonar
      - SONAR_JDBC_USERNAME=sonar
      - SONAR_JDBC_PASSWORD=sonar
      - SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true
    volumes:
      - sonarqube-data:/opt/sonarqube/data
      - sonarqube-extensions:/opt/sonarqube/extensions
    ports:
      - "9000:9000"
    networks:
      - my-ci-network

  # PostgreSQL Database for SonarQube
  sonar-db:
    image: postgres:13
    container_name: sonar-postgres
    restart: unless-stopped
    environment:
      - POSTGRES_USER=sonar
      - POSTGRES_PASSWORD=sonar
      - POSTGRES_DB=sonar
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - my-ci-network

  # Nexus Repository Manager
  artifact-store:
    image: sonatype/nexus3
    container_name: my-nexus
    restart: unless-stopped
    ports:
      - "8081:8081"
    volumes:
      - nexus-data:/nexus-data
    networks:
      - my-ci-network

  # PgAdmin - PostgreSQL Web GUI
  pgadmin:
    image: dpage/pgadmin4
    container_name: pgadmin
    restart: unless-stopped
    ports:
      - "5050:80"
    environment:
      - PGADMIN_DEFAULT_EMAIL=admin@example.com
      - PGADMIN_DEFAULT_PASSWORD=admin
    volumes:
      - pgadmin-data:/var/lib/pgadmin
    networks:
      - my-ci-network

volumes:
  jenkins-data:
  sonarqube-data:
  sonarqube-extensions:
  postgres-data:
  nexus-data:
  pgadmin-data:

networks:
  my-ci-network:
    driver: bridge

# Jenkins:       http://localhost:8080
# SonarQube:     http://localhost:9000  
# Nexus:         http://localhost:8081
# pgAdmin:       http://localhost:5050  (admin@example.com/admin)
# PostgreSQL DB: jdbc:postgresql://sonar-db:5432/sonar (internal)
# Jira:          http://localhost:8082 (if added)

# Create and start all containers in detached mode
# docker-compose up -d
# Check status of all containers
# docker-compose ps

# Alternative: View all containers with their status and ports
# docker ps -a
### ✅ Jenkins ###
#docker exec -it my-jenkins-server /bin/bash
#docker logs -f my-jenkins-server
#docker restart my-jenkins-server

### ✅ SonarQube ###
#docker exec -it my-sonarqube /bin/bash
#docker logs -f my-sonarqube
#docker restart my-sonarqube

### ✅ PostgreSQL for Sonar ###
#docker exec -it sonar-postgres psql -U sonar -d sonar
#docker logs -f sonar-postgres
#docker restart sonar-postgres

### ✅ Nexus Repository ###
#docker exec -it my-nexus /bin/bash
#docker logs -f my-nexus
#docker restart my-nexus

### ✅ pgAdmin ###
#docker exec -it pgadmin /bin/sh
#docker logs -f pgadmin
#docker restart pgadmin

### 🧪 Jira PostgreSQL (Optional) ###
#docker exec -it jira-postgres psql -U postgres
#docker logs -f jira-postgres
#docker restart jira-postgres

### 🔁 Full System Control ###
#docker-compose up -d         # Start all containers
#docker-compose down -v       # Stop and remove all containers + volumes
#docker-compose ps            # Show container status
#docker ps -a                 # Show all running & exited containers

# Create the script inside the container
# docker exec -it 72a9d8671613 bash
# cd /var/jenkins_home
# vi delete_failed_builds.sh
# or nano delete_failed_builds.sh
# or quick way 
# cat << 'EOF' > /var/jenkins_home/delete_failed_builds.sh
#!/bin/bash

#JENKINS_URL="http://localhost:8080"
#JOB_NAME="mrdevops_java_app"
#USERNAME="your-jenkins-user"
#API_TOKEN="your-api-token"
#CRUMB=$(curl -s -u "$USERNAME:$API_TOKEN" "$JENKINS_URL/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,":",//crumb)")

#builds=$(curl -s -u "$USERNAME:$API_TOKEN" "$JENKINS_URL/job/$JOB_NAME/api/json?tree=builds[number,result]" | jq '.builds[] | select(.result=="FAILURE") | .number')

#for build_number in $builds; do
#  echo "Deleting failed build #$build_number"
#  curl -X POST -u "$USERNAME:$API_TOKEN" -H "$CRUMB" "$JENKINS_URL/job/$JOB_NAME/$build_number/doDelete"
#done
#EOF


# chmod +x /var/jenkins_home/delete_failed_builds.sh



C:\Users\visha\full cicd\docker-compose.jira.yml


version: '3.8'

services:
  postgres:
    image: postgres:13-alpine
    container_name: jira-postgres
    environment:
      POSTGRES_DB: jiradb
      POSTGRES_USER: jirauser
      POSTGRES_PASSWORD: jira_password
      POSTGRES_HOST_AUTH_METHOD: md5
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - jira-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U jirauser -d jiradb"]
      interval: 5s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  jira:
    image: atlassian/jira-software:9.4.0
    container_name: jira-app
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      - JIRA_HOME=/var/atlassian/application-data/jira
      - JIRA_DATABASE_URL=jdbc:postgresql://postgres:5432/jiradb
      - JIRA_DB_USER=jirauser
      - JIRA_DB_PASSWORD=jira_password
      - JVM_MINIMUM_MEMORY=1024m
      - JVM_MAXIMUM_MEMORY=2048m
      - JVM_SUPPORT_RECOMMENDED_ARGS=-Djava.awt.headless=true
      - ATL_TOMCAT_PORT=8082  # Internal container port
    volumes:
      - jira_data:/var/atlassian/application-data/jira
      - jira_plugins:/opt/atlassian/jira/atlassian-jira/WEB-INF/atlassian-bundled-plugins
      - jira_logs:/opt/atlassian/jira/logs
    ports:
      - "8082:8082"  # Host:Container port mapping
    networks:
      - jira-network
    restart: unless-stopped

volumes:
  postgres_data:
  jira_data:
  jira_plugins:
  jira_logs:

networks:
  jira-network:
    driver: bridge

# Note: The following URLs are examples and should be replaced with actual values based on your setup.
#http://localhost:8081 #Jira URL
#http://localhost:8081/login.jsp #Jira login page
#http://localhost:8081/secure/admin/ConfigureApplication!default.jspa #Jira admin page
#http://localhost:8081/secure/admin/ConfigureUserServer!default.jspa #Jira user management page
#http://localhost:8081/secure/admin/ConfigureGroupServer!default.jspa #Jira group management page
#postgres://jirauser:jira_password@localhost:5432/jiradb #PostgreSQL connection string
#http://localhost:8081/secure/admin/ConfigureUserServer!default.jspa #Jira user management page 
#http://localhost:8081/secure/admin/ConfigureGroupServer!default.jspa #Jira group management page
#http://localhost:8081/secure/admin/ConfigureApplication!default.jspa #Jira admin page
## Create and start all containers in detached mode
# docker-compose -f docker-compose2.yml up -d
## Check status of all containers
# docker-compose -f docker-compose2.yml ps
# Check the logs of a specific container (e.g., Jira)
# docker-compose -f docker-compose2.yml logs -f jira
# Check the logs of a specific container (e.g., PostgreSQL)
# docker-compose -f docker-compose2.yml logs -f postgres  
# Check the logs of all containers
# docker-compose -f docker-compose2.yml logs -f
# Check the logs of all containers with timestamps
# docker-compose -f docker-compose2.yml logs -f --timestamps
# Check the logs of all containers with timestamps and follow mode
# docker-compose -f docker-compose2.yml logs -f --timestamps --follow 
# Check the logs of all containers with timestamps and follow mode, and limit to 100 lines
# docker-compose -f docker-compose2.yml logs -f --timestamps --follow --tail=100
# Check the logs of all containers with timestamps and follow mode, and limit to 100 lines, and show only errors
# docker-compose -f docker-compose2.yml logs -f --timestamps --follow --tail=100 --filter "error"
# Check the logs of all containers with timestamps and follow mode, and limit to 100 lines, and show only warnings
# docker-compose -f docker-compose2.yml logs -f --timestamps --follow --tail=100 --filter "warning"
# Alternative: View all containers with status and ports
# docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
# docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter "name=postgres" --filter "name=jira"
# docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter "name=postgres" --filter "name=jira" --filter "status=running"
# docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter "name=postgres" --filter "name=jira" --filter "status=exited"
# docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter "name=postgres" --filter "name=jira" --filter "status=created"
# docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter "name=postgres" --filter "name=jira" --filter "status=paused"
# docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter "name=postgres" --filter "name=jira" --filter "status=dead"
# docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter "name=postgres" --filter "name=jira" --filter "status=removing"
# docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter "name=postgres" --filter "name=jira" --filter "status=stopped"
# docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter "name=postgres" --filter "name=jira" --filter "status=all"


