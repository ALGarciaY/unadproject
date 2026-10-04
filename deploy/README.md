# Poner CorpForms en el VPS

Escrito para el mismo servidor de Finanzas y Dentalis (`vps-41ee6417`), con el
Caddy compartido de `/opt/proxy`. Mismo mecanismo de despliegue; la única
diferencia es que **trae su propia base** (es temporal).

| Pieza | Decisión |
|---|---|
| Dominio | `corpforms.janierzapata.com` (Cloudflare en «Solo DNS») |
| Borde | El Caddy de `/opt/proxy`. CorpForms **no publica puertos** |
| BFF | El nginx de `corpforms-web` sirve la SPA y reenvía `/api/` y `/uploads/` a la API |
| API | `corpforms-api` (Spring Boot 4 / Java 21), solo en la red `corpforms` |
| Base | `corpforms-db` (postgres:16-alpine) propio, solo en la red `corpforms`. **No** usa `controlapp_postgres` |
| Despliegue | push a `main` → Integración → GHCR (tag = SHA); tag `release-X.Y.Z` → VPS |

```
Internet ──▶ proxy_caddy ──(red proxy)──▶ corpforms-web (nginx)
                                              │  /api/, /uploads/
                                         (red corpforms)
                                              ▼
                                        corpforms-api ──▶ corpforms-db
```

---

## 1. Repositorio

`github.com/ALGarciaY/unadproject` (transferido a Janier). Las imágenes quedan en
`ghcr.io/algarciay/unadproject/{api,web}` — en minúsculas: GHCR no acepta
mayúsculas y los workflows lo convierten solos.

## 2. Secrets del repo

| Secret | Valor |
|---|---|
| `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`, `VPS_PORT` | los mismos de Finanzas |
| `PROXY_NETWORK` | `proxy` |
| `POSTGRES_PASSWORD` | `openssl rand -hex 24` |
| `JWT_SECRET` | `openssl rand -hex 32` (sin `$`) |
| `ADMIN_EMAIL`, `ADMIN_DOCUMENT` | admin inicial. El login es correo + nº de documento |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | opcional (recuperación por correo) |

Variable opcional `APP_DOMAIN` (por defecto `corpforms.janierzapata.com`).

## 3. Comprobar el terreno (en el VPS)

```bash
docker network ls | grep proxy                    # la red de Caddy
dig +short corpforms.janierzapata.com             # debe dar…
curl -s ifconfig.me                               # …esta IP
```

## 4. Desplegar

Con Integración en verde en `main`:

```bash
git tag release-1.0.0 && git push origin release-1.0.0
```

## 5. Caddy

Pega `deploy/Caddyfile.bloque` en `/opt/proxy/Caddyfile` y:

```bash
cd /opt/proxy
docker compose exec caddy caddy reload --config /etc/caddy/Caddyfile
```

## 6. Verificar

```bash
docker ps --filter name=corpforms
curl -sI https://corpforms.janierzapata.com | head -1
curl -s  https://corpforms.janierzapata.com/api/categories | head -c 200
# La API y la base NO deben ser alcanzables desde fuera:
docker inspect corpforms-api corpforms-db --format '{{.Name}} {{json .NetworkSettings.Ports}}'
```

## Volver atrás

```bash
cd /opt/corpforms
sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=$(cat .version-anterior)/" .env
docker compose -f docker-compose.prod.yml up -d
```

## Retirarlo (es temporal)

```bash
cd /opt/corpforms
docker compose -f docker-compose.prod.yml down -v   # -v borra base y archivos subidos
```
Y quitar el bloque del Caddyfile + `caddy reload`.

---

### Notas

- **CSP y el script inline de `index.html`.** El bloque de Caddy permite ese
  script por su hash. Si se modifica, recalcula y reemplaza el `sha256-…`:
  ```bash
  cd Front && VITE_API_URL="" npm run build && python3 -c "import re,hashlib,base64;s=open('dist/index.html').read();[print('sha256-'+base64.b64encode(hashlib.sha256(m.encode()).digest()).decode()) for m in re.findall(r'<script>(.*?)</script>',s,re.S)]"
  ```
- **Sin GitHub Actions** (a mano, desde el Mac; el VPS es amd64):
  ```bash
  docker buildx build --platform linux/amd64 -t ghcr.io/algarciay/unadproject/api:main --push Back
  docker buildx build --platform linux/amd64 -t ghcr.io/algarciay/unadproject/web:main --push Front
  ```
  y en el VPS copiar `docker-compose.prod.yml` + `.env` (de `.env.prod.example`)
  a `/opt/corpforms` y `docker compose -f docker-compose.prod.yml up -d`.
- El esquema lo crea Hibernate (`ddl-auto: update`) en el arranque.
