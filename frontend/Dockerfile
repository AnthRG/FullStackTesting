# --- Etapa de build ---
FROM node:24-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# Vite inyecta las variables VITE_* en build time.
# Editable con --build-arg VITE_API_URL=... (default: localhost)
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=${VITE_API_URL}

RUN npm run build

# --- Etapa de runtime ---
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
