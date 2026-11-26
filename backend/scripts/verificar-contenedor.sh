#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
name="crm-ci-$RANDOM-$$"
network="$name"
db="$name-db"
api="$name-api"
admin_password="$(openssl rand -hex 20)"
db_password="$(openssl rand -hex 20)"
root_password="$(openssl rand -hex 20)"
jwt_secret="$(openssl rand -hex 32)"
cleanup() {
  docker rm -f "$api" "$db" >/dev/null 2>&1 || true
  docker network rm "$network" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker network create "$network" >/dev/null
docker run -d --name "$db" --network "$network" \
  -e MYSQL_ROOT_PASSWORD="$root_password" -e MYSQL_DATABASE=crm \
  -e MYSQL_USER=crm -e MYSQL_PASSWORD="$db_password" mysql:8.4 >/dev/null

for attempt in {1..60}; do
  if docker exec -e MYSQL_PWD="$db_password" "$db" mysql -h127.0.0.1 -ucrm crm -e 'SELECT 1' >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
docker exec -i -e MYSQL_PWD="$db_password" "$db" mysql -h127.0.0.1 -ucrm crm < "$root/database/01_esquema.sql"

docker run -d --name "$api" --network "$network" -p 127.0.0.1::8080 \
  -e MYSQL_URL="jdbc:mysql://$db:3306/crm" -e MYSQL_USER=crm -e MYSQL_PASSWORD="$db_password" \
  -e JWT_SECRETO="$jwt_secret" -e ADMIN_PASSWORD="$admin_password" -e DATOS_DEMO=true \
  crm-cobranzas:ci >/dev/null
base="http://$(docker port "$api" 8080/tcp)"

ready=false
for attempt in {1..45}; do
  if curl -fsS "$base/actuator/health/readiness" >/dev/null 2>&1; then
    ready=true
    break
  fi
  if [[ "$(docker inspect -f '{{.State.Running}}' "$api")" != true ]]; then
    break
  fi
  sleep 2
done
if [[ "$ready" != true ]]; then
  docker logs "$api"
  exit 1
fi

curl -fsS -H 'Content-Type: application/json' \
  --data "{\"usuario\":\"admin\",\"password\":\"$admin_password\"}" "$base/api/auth/login" >/dev/null
[[ "$(curl -s -o /dev/null -w '%{http_code}' "$base/h2-console/")" == 401 ]]
[[ "$(docker exec -e MYSQL_PWD="$db_password" "$db" mysql -h127.0.0.1 -ucrm crm -N -e 'SELECT COUNT(*) FROM clientes')" == 0 ]]
[[ "$(docker exec -e MYSQL_PWD="$db_password" "$db" mysql -h127.0.0.1 -ucrm crm -N -e 'SELECT COUNT(*) FROM usuarios')" == 1 ]]
