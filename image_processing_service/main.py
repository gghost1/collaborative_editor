from confluent_kafka import Consumer, KafkaException
import psycopg2
import json
import logging
from typing import List, Dict, Any
from merge_pixels_file import merge_pixels

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class KafkaToPostgresPipeline:
    def __init__(self, kafka_config: Dict[str, Any], postgres_config: Dict[str, Any]):
        self.kafka_config = kafka_config
        self.postgres_config = postgres_config
        self.consumer = None
        self.conn = None
        
    def connect_kafka(self) -> None:
        """Establish connection to Kafka"""
        try:
            self.consumer = Consumer(self.kafka_config)
            logger.info("Connected to Kafka successfully")
        except Exception as e:
            logger.error(f"Failed to connect to Kafka: {e}")
            raise

    def connect_postgres(self) -> None:
        """Establish connection to PostgreSQL"""
        try:
            self.conn = psycopg2.connect(**self.postgres_config)
            logger.info("Connected to PostgreSQL successfully")
        except Exception as e:
            logger.error(f"Failed to connect to PostgreSQL: {e}")
            raise

    def create_table_if_not_exists(self) -> None:
        """Ensure the target table exists in PostgreSQL"""
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

    def get_existing_pixels(self) -> List[Dict[str, Any]]:
        """Retrieve all existing pixel data from PostgreSQL"""
        with self.conn.cursor() as cur:
            cur.execute("SELECT data FROM frames")
            results = cur.fetchall()
            
        existing_pixels = []
        for row in results:
            try:
                data = row[0]
                if isinstance(data, list):
                    existing_pixels.extend(data)
                else:
                    existing_pixels.append(data)
            except Exception as e:
                logger.error(f"Error parsing existing pixel data: {e}")
                
        return existing_pixels

    def save_to_postgres(self, data: List[Dict[str, Any]]) -> None:
        """Save merged pixel data to PostgreSQL"""
        # Clear old data and save new merged data
        clear_table_sql = "TRUNCATE TABLE frames"
        insert_sql = "INSERT INTO frames (data) VALUES (%s)"
        
        with self.conn.cursor() as cur:
            # Clear existing data
            cur.execute(clear_table_sql)
            
            # Insert new merged data as a single JSON array
            cur.execute(insert_sql, (json.dumps(data),))
            self.conn.commit()
            
        logger.info(f"Saved {len(data)} pixels to PostgreSQL")

    def process_kafka_message(self, msg_value: bytes) -> None:
        """Process a single Kafka message"""
        try:
            # Parse incoming data
            new_pixels = json.loads(msg_value.decode('utf-8'))
            if not isinstance(new_pixels, list):
                new_pixels = [new_pixels]
                
            # Validate pixel data
            for pixel in new_pixels:
                if not all(k in pixel for k in ['x', 'y', 'color']):
                    raise ValueError(f"Invalid pixel format: {pixel}")
                    
            # Get existing data
            existing_pixels = self.get_existing_pixels()
            
            # Merge data
            merged_pixels = merge_pixels(existing_pixels, new_pixels)
            
            # Save merged data
            self.save_to_postgres(merged_pixels)
            
            logger.info(f"Processed {len(new_pixels)} new pixels, total {len(merged_pixels)} pixels after merge")
            
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON: {e}")
        except ValueError as e:
            logger.error(f"Invalid data format: {e}")
        except Exception as e:
            logger.error(f"Error processing message: {e}")

    def consume_from_kafka(self, topic: str) -> None:
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
                
                self.process_kafka_message(msg.value())
                    
        except KeyboardInterrupt:
            logger.info("Consumer interrupted")
        finally:
            if self.consumer:
                self.consumer.close()
            if self.conn:
                self.conn.close()

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