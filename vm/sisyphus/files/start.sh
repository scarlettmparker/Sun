#!/usr/bin/env bash
#
# Launches the backend. AUTH and JVM sizing come from /opt/sun/backend/.env,
# which sisyphus.sh renders from the repo .env plus sisyphus.conf.

set -euo pipefail

AUTH="${AUTH:-on}"
JVM_XMS="${JVM_XMS:-32m}"
JVM_XMX="${JVM_XMX:-256m}"
JVM_METASPACE="${JVM_METASPACE:-128m}"
JVM_CPUS="${JVM_CPUS:-1}"
JVM_GC="${JVM_GC:-g1}"
JVM_CDS="${JVM_CDS:-on}"
NMT="${NMT:-off}"
COMPACT_HEADERS="${COMPACT_HEADERS:-off}"
APP_JAR="${APP_JAR:-/opt/sun/backend/sun-graphql.jar}"

jvm=(
  "-Xms$JVM_XMS"
  "-Xmx$JVM_XMX"
  "-XX:MaxMetaspaceSize=$JVM_METASPACE"
  "-XX:ReservedCodeCacheSize=96m"
  "-XX:ActiveProcessorCount=$JVM_CPUS"
  "-XX:+ExitOnOutOfMemoryError"
  "-XX:+HeapDumpOnOutOfMemoryError"
  "-XX:HeapDumpPath=/opt/sun/backend/heapdump.hprof"
  "-Xlog:gc*:file=/opt/sun/backend/gc.log:time,uptime,level,tags:filecount=3,filesize=5M"
  "-Xss512k"
)

case "$JVM_GC" in
  serial)
    jvm+=("-XX:+UseSerialGC")
    ;;
  *)
    jvm+=(
      "-XX:+UseG1GC"
      "-XX:MaxGCPauseMillis=100"
      "-XX:+ParallelRefProcEnabled"
      "-XX:MinHeapFreeRatio=20"
      "-XX:MaxHeapFreeRatio=50"
      "-XX:G1PeriodicGCInterval=300000"
      "-XX:+G1PeriodicGCInvokesConcurrent"
    )
    ;;
esac

if [ "$NMT" = "on" ]; then
  jvm+=("-XX:NativeMemoryTracking=summary")
fi

if [ "$COMPACT_HEADERS" = "on" ]; then
  jvm+=("-XX:+UnlockExperimentalVMOptions" "-XX:+UseCompactObjectHeaders")
fi

if [ "$JVM_CDS" = "on" ]; then
  jvm+=("-XX:+AutoCreateSharedArchive" "-XX:SharedArchiveFile=/opt/sun/backend/app.jsa")
fi

if [ "${JFR:-off}" = "on" ]; then
  jvm+=("-XX:StartFlightRecording=settings=profile,filename=/opt/sun/backend/recording.jfr,dumponexit=true")
fi

app=(
  "--server.tomcat.threads.max=24"
  "--server.tomcat.threads.min-spare=2"
  "--server.tomcat.max-connections=1000"
  "--spring.datasource.hikari.maximum-pool-size=4"
  "--spring.datasource.hikari.minimum-idle=1"
  "--spring.jpa.hibernate.ddl-auto=none"
)

if [ "$AUTH" != "on" ]; then
  app+=(
    "--app.bypass-permissions=true"
    "--permify.enabled=false"
    "--management.health.redis.enabled=false"
  )
fi

exec java "${jvm[@]}" -jar "$APP_JAR" "${app[@]}"
