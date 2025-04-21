#!/usr/bin/env python3

import json
import os
import signal
import sys
from contextlib import contextmanager
from typing import List, Dict, Any

import psycopg2
from psycopg2 import sql, extras
from confluent_kafka import Consumer, KafkaError, KafkaException
from dotenv import load_dotenv

import logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger(__name__)

load_dotenv()

# ─────────────────────── PostgreSQL ────────────────────────────────────────────
@contextmanager
def pg_conn():
    conn = psycopg2.connect(
        host=os.getenv("PG_HOST", "localhost"),
        port=os.getenv("PG_PORT", 5432),
        dbname=os.getenv("PG_DB", "postgres"),
        user=os.getenv("PG_USER", "postgres"),
        password=os.getenv("PG_PASSWORD", ""),
    )
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def ensure_pixels_table(conn):
    with conn.cursor() as cur:
        cur.execute(
            """
            CREATE TABLE IF NOT EXISTS pixels (
                x     INT NOT NULL,
                y     INT NOT NULL,
                color TEXT NOT NULL,
                PRIMARY KEY (x, y)
            );
            """
        )
    conn.commit()


def upsert_pixels(conn, pixels: List[Dict[str, Any]]):
    if not pixels:
        return
    values = [(p["x"], p["y"], p["color"]) for p in pixels]

    with conn.cursor() as cur:
        query = sql.SQL(
            """
            INSERT INTO pixels (x, y, color)
            VALUES %s
            ON CONFLICT (x, y) DO UPDATE
              SET color = EXCLUDED.color
            """
        )
        extras.execute_values(cur, query, values, template="(%s,%s,%s)")


# ───────────────────────── Kafka ───────────────────────────────────────────────
def create_consumer() -> Consumer:
    cfg = {
        "bootstrap.servers": os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
        "group.id": os.getenv("KAFKA_GROUP_ID", "pixel-updater"),
        "auto.offset.reset": "earliest",
        "enable.auto.commit": False,
    }
    c = Consumer(cfg)
    c.subscribe([os.getenv("KAFKA_TOPIC", "frames_topic")])
    return c


# ─────────────────────── Главный цикл ─────────────────────────────────────────
running = True


def shutdown(sig, frame):
    global running
    log.info("Останавливаюсь по сигналу…")
    running = False


signal.signal(signal.SIGINT, shutdown)
signal.signal(signal.SIGTERM, shutdown)


def main():
    consumer = create_consumer()
    log.info("Pixel‑consumer запущен и ждёт сообщения")

    with pg_conn() as conn:
        ensure_pixels_table(conn)

    try:
        while running:
            msg = consumer.poll(1.0)
            if msg is None:
                continue
            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    continue
                raise KafkaException(msg.error())

            try:
                payload = json.loads(msg.value().decode())
                

                if not isinstance(payload, list):
                    payload = [payload]
                pixels = [
                    {"x": int(p["x"]), "y": int(p["y"]), "color": str(p["color"])}
                    for p in payload
                    if {"x", "y", "color"} <= p.keys()
                ]
                if not pixels:
                    log.warning("Сообщение без корректных пикселей, пропускаю")
                    consumer.commit(msg)
                    continue

                with pg_conn() as conn:
                    upsert_pixels(conn, pixels)

                consumer.commit(msg)
                log.info("✓ Обновил %d пикселей (offset %s)", len(pixels), msg.offset())

            except json.JSONDecodeError as e:
                log.error("Неверный JSON: %s", e)
                consumer.commit(msg)

    finally:
        consumer.close()
        log.info("Consumer закрыт")


if __name__ == "__main__":
    try:
        main()
    except Exception as err:
        log.exception("Критическая ошибка: %s", err)
        sys.exit(1)
