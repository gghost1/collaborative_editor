# Python service to merge images and save them to database

Pixel‑consumer → PostgreSQL UPSERT.

Зависимости:
    pip install confluent-kafka psycopg2-binary python-dotenv

Переменные окружения (можно сложить в .env):

    # Kafka
    KAFKA_BOOTSTRAP_SERVERS = localhost:9092
    KAFKA_TOPIC            = frames_topic          # тот же, что слушает Альберт
    KAFKA_GROUP_ID         = pixel-updater

    # PostgreSQL
    PG_HOST     = localhost
    PG_PORT     = 5432
    PG_DB       = pixels_db
    PG_USER     = postgres
    PG_PASSWORD = secret