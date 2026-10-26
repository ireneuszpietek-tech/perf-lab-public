Portainer CE (Community Edition) free & light.

core-kotlin rebuild
docker compose --profile core up -d --build core-kotlin

all rebuild
docker compose --profile core down -v --remove-orphans && docker compose --profile core up --build -d

**Docker UI**
docker volume create portainer_data


2. Run Portainer (web panel)
docker run -d \
-p 9000:9000 -p 9443:9443 \
--name portainer \
--restart=always \
-v /var/run/docker.sock:/var/run/docker.sock \
-v portainer_data:/data \
portainer/portainer-ce:latest

web:
➡️ http://localhost:9000

(on the first time crrate admin account and log)