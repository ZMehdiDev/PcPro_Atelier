# Build du front, puis Caddy qui le sert avec l'API (voir infra/Caddyfile et infra/docker-compose.prod.yml)
FROM node:26-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npx ng build

FROM caddy:2-alpine
COPY --from=build /app/dist/front/browser /srv/front
