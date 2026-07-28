#!/bin/sh
set -eu

: "${API_URL:?falta API_URL}"
: "${KEYCLOAK_URL:?falta KEYCLOAK_URL}"
: "${KEYCLOAK_REALM:=fullstacktesting}"
: "${KEYCLOAK_CLIENT_ID:=frontend}"

export API_URL KEYCLOAK_URL KEYCLOAK_REALM KEYCLOAK_CLIENT_ID

envsubst < /etc/frontend/config.template.js > /usr/share/nginx/html/config.js

exec nginx -g 'daemon off;'
