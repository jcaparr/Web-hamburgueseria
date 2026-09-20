# The frontend is static files, so the same container that serves them also terminates
# HTTPS and proxies /api to the backend: one origin, which is what lets the session
# cookie be SameSite without any CORS exception.
FROM node:22-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Same origin as the site, so no absolute URL is baked into the bundle.
# Public by design; empty simply hides the Google button.
ARG VITE_GOOGLE_CLIENT_ID=""
ENV VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID
ENV VITE_API_URL=/api
RUN npm run build

FROM caddy:2-alpine
COPY --from=build /build/dist /srv
# The Caddyfile is mounted by docker-compose so it can be edited on the server
# without rebuilding the frontend.
