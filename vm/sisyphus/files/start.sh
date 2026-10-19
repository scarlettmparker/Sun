#!/usr/bin/env bash
#
# Launches the backend. AUTH and JVM sizing come from /opt/sun/backend/.env,
# which sisyphus.sh renders from the repo .env plus sisyphus.conf.

set -euo pipefail

AUTH="${AUTH:-on}"
JVM_XMX="${JVM_XMX:-1g}"
JVM_METASPACE="${JVM_METASPACE:-256m}"
APP_JAR="${APP_JAR:-/opt/sun/backend/sun-graphql.jar}"

jvm=(
  "-Xms256m"
  "-Xmx$JVM_XMX"
  "-XX:MaxMetaspaceSize=$JVM_METASPACE"
  "-XX:+UseSerialGC"
  "-XX:+ExitOnOutOfMemoryError"
  "-Xss512k"
)

app=(
  "--server.tomcat.threads.max=50"
  "--server.tomcat.threads.min-spare=5"
  "--server.tomcat.max-connections=1000"
  "--spring.datasource.hikari.maximum-pool-size=8"
  "--spring.datasource.hikari.minimum-idle=2"
)

if [ "$AUTH" != "on" ]; then
  app+=(
    "--app.bypass-permissions=true"
    "--permify.enabled=false"
  )
fi

exec java "${jvm[@]}" -jar "$APP_JAR" "${app[@]}"
