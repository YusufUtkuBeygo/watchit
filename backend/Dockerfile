# WatchIt backend imajı (iki aşama).
# 1. aşama: Gradle ile çalıştırılabilir jar üretilir (testler CI'da koşuyor, burada atlanır).
# 2. aşama: yalnız Java çalışma ortamı + jar; derleyici ve kaynak kod son imaja girmez (küçük ve daha güvenli).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
# Önce yalnız Gradle dosyaları: bağımlılıklar değişmedikçe bu katman önbellekten gelir, her derlemede yeniden inmez
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null
COPY src src
RUN ./gradlew bootJar -x test --no-daemon -q && cp build/libs/*-SNAPSHOT.jar app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
# root olmayan kullanıcı: uygulamada bir açık bulunsa bile konteynerin içinde yetkisi sınırlı kalsın
RUN useradd --system --uid 1001 watchit
COPY --from=build /app/app.jar app.jar
USER watchit
EXPOSE 8080
# Ayarlar ortam değişkenleriyle verilir (DB_PASSWORD, JWT_SECRET, MINIO_SECRET_KEY, GEMINI_API_KEY,
# SPRING_DATASOURCE_URL, MINIO_URL, WHISPER_URL, MAIL_*...). Şifreler imaja yazılmaz.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
