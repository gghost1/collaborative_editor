from confluent_kafka import Consumer, KafkaException
import psycopg2
import json
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class KafkaToPostgresPipeline:
    def __init__(self, kafka_config, postgres_config):
        self.kafka_config = kafka_config
        self.postgres_config = postgres_config
        self.consumer = None
        self.conn = None
        
    def connect_kafka(self):
        try:
            self.consumer = Consumer(self.kafka_config)
            logger.info("Connected to Kafka successfully")
        except Exception as e:
            logger.error(f"Failed to connect to Kafka: {e}")
            raise

    def connect_postgres(self):
        try:
            self.conn = psycopg2.connect(**self.postgres_config)
            logger.info("Connected to PostgreSQL successfully")
        except Exception as e:
            logger.error(f"Failed to connect to PostgreSQL: {e}")
            raise

    def create_table_if_not_exists(self):
        create_table_sql = """
        CREATE TABLE IF NOT EXISTS frames (
            id SERIAL PRIMARY KEY,
            data JSONB NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """
        with self.conn.cursor() as cur:
            cur.execute(create_table_sql)
            self.conn.commit()
        logger.info("Ensured table exists")

    def consume_from_kafka(self, topic):
        """Consume messages from Kafka topic"""
        self.consumer.subscribe([topic])
        logger.info(f"Subscribed to topic: {topic}")
        
        try:
            while True:
                msg = self.consumer.poll(1.0)
                
                if msg is None:
                    continue
                if msg.error():
                    if msg.error().code() == KafkaException._PARTITION_EOF:
                        continue
                    else:
                        logger.error(f"Kafka error: {msg.error()}")
                        break
                
                try:
                    # Parse JSON data
                    data = json.loads(msg.value().decode('utf-8'))
                    self.save_to_postgres(data)
                    logger.info(f"Processed message: {msg.key()}")
                except json.JSONDecodeError as e:
                    logger.error(f"Failed to parse JSON: {e}")
                except Exception as e:
                    logger.error(f"Error processing message: {e}")
                    
        except KeyboardInterrupt:
            logger.info("Consumer interrupted")
        finally:
            self.consumer.close()
            if self.conn:
                self.conn.close()

    def save_to_postgres(self, data):
        """Save JSON data to PostgreSQL"""
        if not isinstance(data, list):
            data = [data]  # Ensure we always have a list of objects
            
        with self.conn.cursor() as cur:
            for item in data:
                # Validate required fields
                if not all(k in item for k in ['x', 'y', 'color']):
                    logger.warning(f"Skipping invalid item: {item}")
                    continue
                    
                cur.execute(
                    "INSERT INTO frames (data) VALUES (%s)",
                    (json.dumps(item),)
                )
            self.conn.commit()
        logger.info(f"Saved {len(data)} items to PostgreSQL")

if __name__ == "__main__":
    kafka_config = {
        'bootstrap.servers': 'localhost:9092',
        'group.id': 'frame-consumer-group',
        'auto.offset.reset': 'earliest'
    }
    
    postgres_config = {
        'host': 'localhost',
        'database': 'dnp',
        'user': 'admin',
        'password': '12345678'
    }
    
    pipeline = KafkaToPostgresPipeline(kafka_config, postgres_config)
    try:
        pipeline.connect_kafka()
        pipeline.connect_postgres()
        pipeline.create_table_if_not_exists()
        pipeline.consume_from_kafka('frames_topic')
    except Exception as e:
        logger.error(f"Pipeline failed: {e}")