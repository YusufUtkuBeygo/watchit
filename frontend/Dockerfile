# WatchIt frontend imajı: Node ile derlenir, nginx ile sunulur (son imajda Node yok, yalnız statik dosyalar)
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# nginx-unprivileged: nginx'in root olmadan çalışan resmî sürümü (güvenlik taraması, 28 Kasım: root ile çalışıyordu).
# Root olmayan kullanıcı 1024'ün altındaki portları açamaz: konteyner içinde 8080, dışarıdan 80 (compose.prod.yaml)
FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
