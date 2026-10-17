# New concepts Learned : 


## 1. Docker and docker basics

everything in https://courses.mooc.fi/org/uh-cs/courses/devops-with-docker-spring-2026

## 2. Nginx and Websockets

you add these to /ws so it's recognized as a websocket handshake

    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";

## 3. Java app Image

In production, it's preferable to build your image in multiple stages until the final part 
where even the JVM is omitted and only the JRE and minimal setup required is present making use of 
Docker's caching properties

## 4. Health Check in docker with depends on : condition :

    healthcheck:
      test: ["CMD-SHELL","pg_isready -U mac -d scok"]
      interval: 5s
      timeout: 5s
      retries: 5
      start_period: 10s.

## 5. Layers and Maven's pom.xml

this enables multilayer image building:

    <configuration>
    <layers>
    <enabled>true</enabled>
    </layers>
    </configuration>


