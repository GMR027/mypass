#!/usr/bin/env bash
# ===========================================================================
# deploy.sh — construye, publica y despliega My Pass en Kubernetes.
#
# Uso:
#   ./deploy.sh                 # tag autoincremental, base limpia (solo migraciones)
#   ./deploy.sh 7               # tag concreto
#   ./deploy.sh --import-dump   # importa mypass-2026-11-22.sql si la base está vacía
#
# Flujo:
#   1. Verifica docker, helm, kubectl, openssl y sesión de Docker Hub
#   2. Genera helm/mypass/values.local.yaml si no existe (secretos aleatorios)
#   3. helm lint / helm template
#   4. docker build → gmr27/mypass:<tag> (+ :latest) y docker push
#   5. helm upgrade --install en el namespace mypass
#   6. Espera MySQL, migraciones, rollout, certificado TLS y prueba de humo
#
# La base y el storage viven en PVCs: no se tocan en los upgrades.
# La primera vez se ejecutan solo las migraciones; el dump NO se importa
# salvo que se pase --import-dump explícitamente.
# ===========================================================================
set -euo pipefail

REPO="gmr27/mypass"
NAMESPACE="mypass"
RELEASE="mypass"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHART_DIR="$ROOT_DIR/helm/mypass"
VALUES_LOCAL="$CHART_DIR/values.local.yaml"
TAG_FILE="$ROOT_DIR/.deploy-tag"
HOST="mypass.iguzman.com.mx"
DUMP_FILE="${MYPASS_DUMP:-$ROOT_DIR/../mypass-2026-11-22.sql}"
APK_FILE="${MYPASS_APK:-$ROOT_DIR/android/app/build/outputs/apk/release/app-release.apk}"
VERSION_FILE="$ROOT_DIR/android/version.properties"
NOTES_FILE="$ROOT_DIR/android/release-notes.json"

IMPORT_DUMP=0
TAG=""

verde()    { printf '\033[32m%s\033[0m\n' "$*"; }
amarillo() { printf '\033[33m%s\033[0m\n' "$*"; }
rojo()     { printf '\033[31m%s\033[0m\n' "$*" >&2; }

for arg in "$@"; do
    case "$arg" in
        --import-dump) IMPORT_DUMP=1 ;;
        --help|-h)
            sed -n '2,25p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *)
            if [[ -n "$TAG" ]]; then
                rojo "Parámetro no reconocido: $arg"
                exit 1
            fi
            TAG="$arg"
            ;;
    esac
done

# --- Tag -------------------------------------------------------------------
if [[ -z "$TAG" ]]; then
    ULTIMO=0
    [[ -f "$TAG_FILE" ]] && ULTIMO="$(cat "$TAG_FILE")"
    [[ "$ULTIMO" =~ ^[0-9]+$ ]] || ULTIMO=0
    TAG=$((ULTIMO + 1))
fi
[[ "$TAG" =~ ^[A-Za-z0-9._-]+$ ]] || { rojo "Tag inválido: $TAG"; exit 1; }
verde "==> Tag de esta versión: $TAG"

# --- Requisitos -------------------------------------------------------------
for cmd in docker helm kubectl openssl curl; do
    command -v "$cmd" >/dev/null 2>&1 || { rojo "Falta '$cmd' en el PATH."; exit 1; }
done

if ! grep -q '"auths"' "$HOME/.docker/config.json" 2>/dev/null \
   || grep -q '"auths"[[:space:]]*:[[:space:]]*{}' "$HOME/.docker/config.json" 2>/dev/null; then
    rojo "No hay credenciales de Docker Hub. Ejecuta primero: docker login"
    exit 1
fi

# --- Secretos locales -------------------------------------------------------
if [[ ! -f "$VALUES_LOCAL" ]]; then
    DB_PASSWORD="${MYPASS_DB_PASSWORD:-$(openssl rand -hex 24)}"
    MYSQL_ROOT_PASSWORD="${MYPASS_MYSQL_ROOT_PASSWORD:-$(openssl rand -hex 24)}"
    APP_KEY="base64:$(openssl rand -base64 32)"

    if [[ -n "${MYPASS_SMTP_PASSWORD:-}" ]]; then
        MAIL_ENABLED="true"
        MAIL_PASSWORD="${MYPASS_SMTP_PASSWORD}"
    else
        MAIL_ENABLED="false"
        MAIL_PASSWORD=""
    fi

    umask 077
    cat > "$VALUES_LOCAL" <<EOF
# Generado por deploy.sh — NO se versiona.
# Consérvalo: cambiarlo después de inicializar MySQL rompe la conexión.
secrets:
  dbPassword: "$DB_PASSWORD"
  mysqlRootPassword: "$MYSQL_ROOT_PASSWORD"
  appKey: "$APP_KEY"

mail:
  enabled: $MAIL_ENABLED
  password: "$MAIL_PASSWORD"
EOF

    verde "==> Creado $VALUES_LOCAL con secretos aleatorios."
    amarillo "    Guarda una copia segura de este archivo."
fi

chmod 600 "$VALUES_LOCAL"

if [[ "$IMPORT_DUMP" = "1" && ! -f "$DUMP_FILE" ]]; then
    rojo "No existe el dump: $DUMP_FILE"
    exit 1
fi

# --- Validación del APK -----------------------------------------------------
# El APK se publica en /downloads/mypass.apk. Si se compiló con la URL local
# por defecto, la app instalada falla con "failed to connect 127.0.0.1:38022".
apk_api_base_url() {
    python3 - "$1" <<'PY'
import re, sys, zipfile

with zipfile.ZipFile(sys.argv[1]) as apk:
    dex = b"".join(
        apk.read(name)
        for name in apk.namelist()
        if re.fullmatch(r"classes\d*\.dex", name)
    )
url = re.search(rb"https?://[^\x00-\x20\"']+/api/v1", dex)
print(url.group(0).decode() if url else "")
PY
}

apk_version_property() {
    sed -n "s/^$1=//p" "$VERSION_FILE" 2>/dev/null | tail -1
}

sdk_dir() {
    if [[ -n "${ANDROID_HOME:-}" ]]; then
        printf '%s\n' "$ANDROID_HOME"
        return
    fi
    sed -n 's/^sdk.dir=//p' "$ROOT_DIR/android/local.properties" 2>/dev/null | tail -1
}

APKANALYZER=""
SDK_DIR="$(sdk_dir)"
if [[ -n "$SDK_DIR" && -x "$SDK_DIR/cmdline-tools/latest/bin/apkanalyzer" ]]; then
    APKANALYZER="$SDK_DIR/cmdline-tools/latest/bin/apkanalyzer"
fi

APK_VERSION_CODE="$(apk_version_property versionCode)"
APK_VERSION_NAME="$(apk_version_property versionName)"
MIN_SUPPORTED_VERSION_CODE="$(apk_version_property minSupportedVersionCode)"
[[ "$MIN_SUPPORTED_VERSION_CODE" =~ ^[0-9]+$ ]] || MIN_SUPPORTED_VERSION_CODE=0

if [[ -f "$APK_FILE" ]]; then
    APK_URL="$(apk_api_base_url "$APK_FILE")"
    if [[ "$APK_URL" != "https://$HOST/api/v1" ]]; then
        rojo "El APK apunta a '${APK_URL:-una URL desconocida}', no al clúster."
        rojo "Recompílalo antes de desplegar:"
        rojo "  cd android && ./gradlew assembleRelease -Pmypass.apiBaseUrl=https://$HOST/api/v1"
        exit 1
    fi

    if [[ -n "$APKANALYZER" ]]; then
        APK_VERSION_CODE="$("$APKANALYZER" manifest version-code "$APK_FILE")"
        APK_VERSION_NAME="$("$APKANALYZER" manifest version-name "$APK_FILE")"
    else
        amarillo "    apkanalyzer no disponible; se usa $VERSION_FILE para la versión."
    fi

    if [[ ! "$APK_VERSION_CODE" =~ ^[0-9]+$ || -z "$APK_VERSION_NAME" ]]; then
        rojo "No se pudo determinar la versión del APK."
        exit 1
    fi

    PUBLISHED_VERSION_CODE=""
    PUBLISHED_MANIFEST="$(kubectl get pod -n "$NAMESPACE" -l app=mypass \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)"
    if [[ -n "$PUBLISHED_MANIFEST" ]]; then
        PUBLISHED_VERSION_CODE="$(kubectl exec -n "$NAMESPACE" "$PUBLISHED_MANIFEST" -- \
            cat /var/www/html/storage/app/private/downloads/version.json 2>/dev/null \
            | python3 -c 'import json,sys; print(json.load(sys.stdin).get("versionCode", ""))' 2>/dev/null || true)"
    fi

    if [[ -n "$PUBLISHED_VERSION_CODE" && "$APK_VERSION_CODE" -le "$PUBLISHED_VERSION_CODE" ]]; then
        if [[ "${MYPASS_ALLOW_SAME_VERSION:-0}" != "1" ]]; then
            rojo "El APK tiene versionCode $APK_VERSION_CODE, pero ya está publicada la $PUBLISHED_VERSION_CODE."
            rojo "Incrementa versionCode en android/version.properties y vuelve a compilar."
            exit 1
        fi
    fi
fi

# --- Validación del chart ---------------------------------------------------
verde "==> Validando el chart..."
helm lint "$CHART_DIR" -f "$CHART_DIR/values.yaml" -f "$VALUES_LOCAL" >/dev/null
helm template "$RELEASE" "$CHART_DIR" \
    -f "$CHART_DIR/values.yaml" -f "$VALUES_LOCAL" >/dev/null

# --- Build ------------------------------------------------------------------
COMMIT="$(git -C "$ROOT_DIR" rev-parse --short HEAD 2>/dev/null || echo sin-git)"
FECHA_BUILD="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

verde "==> Construyendo imagen $REPO:$TAG..."
docker build \
    --label "org.opencontainers.image.revision=$COMMIT" \
    --label "org.opencontainers.image.created=$FECHA_BUILD" \
    --label "org.opencontainers.image.title=mypass" \
    -t "$REPO:$TAG" \
    -t "$REPO:latest" \
    "$ROOT_DIR/server"

# --- Push -------------------------------------------------------------------
verde "==> Subiendo a Docker Hub..."
docker push "$REPO:$TAG"
docker push "$REPO:latest"

# --- Helpers de base de datos ----------------------------------------------
mysql_pod() {
    kubectl get pod -n "$NAMESPACE" -l app=mypass-mysql \
        -o jsonpath='{.items[0].metadata.name}'
}

db_has_users() {
    local pod tables users
    pod="$(mysql_pod)"
    tables="$(kubectl exec -n "$NAMESPACE" "$pod" -- sh -c '
        MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -s -e \
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=\"$MYSQL_DATABASE\" AND table_name=\"users\";"
    ')"
    [[ "$tables" = "1" ]] || return 1
    users="$(kubectl exec -n "$NAMESPACE" "$pod" -- sh -c '
        MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -s -e \
        "SELECT COUNT(*) FROM users;" "$MYSQL_DATABASE"
    ')"
    [[ "$users" != "0" ]]
}

import_dump() {
    local pod
    pod="$(mysql_pod)"
    amarillo "==> Importando $DUMP_FILE en $NAMESPACE..."
    kubectl exec -i -n "$NAMESPACE" "$pod" -- sh -c '
        MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 \
            -e "CREATE DATABASE IF NOT EXISTS $MYSQL_DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
        MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 "$MYSQL_DATABASE"
        MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$MYSQL_DATABASE" \
            -e "DELETE FROM sessions; TRUNCATE TABLE cache; TRUNCATE TABLE cache_locks;"
    ' < "$DUMP_FILE"
    verde "    Dump importado y sesiones/caché limpiadas."
}

helm_deploy() {
    local replicas="$1"
    helm upgrade --install "$RELEASE" "$CHART_DIR" \
        --namespace "$NAMESPACE" \
        --create-namespace \
        -f "$CHART_DIR/values.yaml" \
        -f "$VALUES_LOCAL" \
        --set-string image.tag="$TAG" \
        --set replicaCount="$replicas" \
        --timeout 15m
}

publish_apk() {
    local apk="$1" pod manifest

    if [[ ! -f "$apk" ]]; then
        amarillo "    No hay APK en $apk; /downloads/mypass.apk quedará 404."
        return 0
    fi

    manifest="$(dirname "$apk")/version.json"
    python3 - "$apk" "$APK_VERSION_CODE" "$APK_VERSION_NAME" "$HOST" "$NOTES_FILE" \
        "$MIN_SUPPORTED_VERSION_CODE" "$manifest" <<'PY'
import hashlib
import json
import os
import sys
from datetime import datetime, timezone

apk, code, name, host, notes_path, min_code, out_path = sys.argv[1:8]

digest = hashlib.sha256()
with open(apk, "rb") as handle:
    for chunk in iter(lambda: handle.read(1024 * 1024), b""):
        digest.update(chunk)

notes = {}
if notes_path and os.path.isfile(notes_path):
    with open(notes_path, encoding="utf-8") as handle:
        notes = json.load(handle)

data = {
    "versionCode": int(code),
    "versionName": name,
    "apkUrl": f"https://{host}/downloads/mypass.apk?v={code}",
    "sha256": digest.hexdigest(),
    "sizeBytes": os.path.getsize(apk),
    "minSupportedVersionCode": int(min_code or 0),
    "publishedAt": datetime.now(timezone.utc)
        .replace(microsecond=0)
        .isoformat()
        .replace("+00:00", "Z"),
    "notes": notes,
}

with open(out_path, "w", encoding="utf-8") as handle:
    json.dump(data, handle, ensure_ascii=False, indent=2)
    handle.write("\n")
PY

    pod="$(kubectl get pod -n "$NAMESPACE" -l app=mypass -o jsonpath='{.items[0].metadata.name}')"
    kubectl exec -n "$NAMESPACE" "$pod" -- sh -c \
        'mkdir -p /var/www/html/storage/app/private/downloads'
    kubectl cp "$apk" "$NAMESPACE/$pod:/var/www/html/storage/app/private/downloads/mypass.apk"
    kubectl cp "$manifest" "$NAMESPACE/$pod:/var/www/html/storage/app/private/downloads/version.json"
    kubectl exec -n "$NAMESPACE" "$pod" -- sh -c \
        'chown www-data:www-data /var/www/html/storage/app/private/downloads/mypass.apk /var/www/html/storage/app/private/downloads/version.json && chmod 644 /var/www/html/storage/app/private/downloads/mypass.apk /var/www/html/storage/app/private/downloads/version.json'
    verde "    APK publicado en https://$HOST/downloads/mypass.apk (v$APK_VERSION_CODE)"
    verde "    Manifiesto publicado en https://$HOST/api/v1/app/version"
}

# --- Deploy -----------------------------------------------------------------
FIRST_INSTALL=0
if ! kubectl get deployment mypass -n "$NAMESPACE" >/dev/null 2>&1; then
    FIRST_INSTALL=1
fi

if [[ "$FIRST_INSTALL" = "1" ]]; then
    verde "==> Primera instalación: MySQL primero, app en 0 réplicas..."
    helm_deploy 0
else
    verde "==> Actualizando el despliegue..."
    helm_deploy 1
fi

verde "==> Esperando a MySQL..."
kubectl rollout status deployment/mypass-mysql -n "$NAMESPACE" --timeout=300s

if [[ "$IMPORT_DUMP" = "1" ]]; then
    if db_has_users; then
        amarillo "    La base ya tiene usuarios: no se importa el dump."
    else
        if [[ "$FIRST_INSTALL" = "0" ]]; then
            kubectl scale deployment/mypass -n "$NAMESPACE" --replicas=0
            kubectl wait --for=delete pod -l app=mypass -n "$NAMESPACE" --timeout=120s 2>/dev/null || true
        fi
        import_dump
        if [[ "$FIRST_INSTALL" = "0" ]]; then
            kubectl scale deployment/mypass -n "$NAMESPACE" --replicas=1
        fi
    fi
fi

if [[ "$FIRST_INSTALL" = "1" ]]; then
    verde "==> Arrancando la app y aplicando migraciones..."
    helm_deploy 1
fi

echo "$TAG" > "$TAG_FILE"

# --- Verificación -----------------------------------------------------------
verde "==> Esperando el rollout de la app..."
kubectl rollout status deployment/mypass -n "$NAMESPACE" --timeout=300s

verde "==> Publicando APK para descarga..."
publish_apk "$APK_FILE"

verde "==> Esperando el certificado TLS..."
kubectl wait --for=condition=Ready certificate/mypass-cert -n "$NAMESPACE" --timeout=300s 2>/dev/null \
    || amarillo "    El certificado aún puede estar emitiéndose; revisa: kubectl get certificate -n $NAMESPACE"

verde "==> Prueba de humo..."
if curl -fsS --max-time 20 "https://$HOST/api/v1/health" | grep -q '"status":"ok"'; then
    verde "    API: OK"
else
    rojo "    La API no respondió correctamente."
    exit 1
fi

if [[ "$(curl -fsS --max-time 20 -o /dev/null -w '%{http_code}' "https://$HOST/up")" = "200" ]]; then
    verde "    /up: OK"
else
    rojo "    /up no respondió 200."
    exit 1
fi

if [[ -f "$APK_FILE" ]]; then
    APK_CODE="$(curl -fsS --max-time 20 -o /dev/null -w '%{http_code}' -r 0-1023 "https://$HOST/downloads/mypass.apk")"
    if [[ "$APK_CODE" = "200" || "$APK_CODE" = "206" ]]; then
        verde "    APK: OK"
    else
        rojo "    El APK no se descarga correctamente (HTTP $APK_CODE)."
        exit 1
    fi
fi

echo
verde "==> Despliegue completado. Estado actual:"
kubectl get pods,svc,ingress,pvc -n "$NAMESPACE"
echo
verde "URL: https://$HOST"
amarillo "Android: recompila con -Pmypass.apiBaseUrl=https://$HOST/api/v1"
