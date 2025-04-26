from confluent_kafka import Consumer
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
            canvas_id TEXT PRIMARY KEY,
            data JSONB NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """
        with self.conn.cursor() as cur:
            cur.execute(create_table_sql)
            self.conn.commit()
        logger.info("Ensured table exists")

    def get_existing_pixels(self, canvas_id) -> List[Dict[str, Any]]:
        """Retrieve existing pixel data for a specific canvas from PostgreSQL"""
        with self.conn.cursor() as cur:
            cur.execute("SELECT data FROM frames WHERE canvas_id = %s", (canvas_id,))
            result = cur.fetchone()
            
        if not result:
            return []
            
        try:
            data = result[0]
            if isinstance(data, list):
                return data
            else:
                return [data]
        except Exception as e:
            logger.error(f"Error parsing existing pixel data: {e}")
            return []

    def save_to_postgres(self, canvas_id, data: List[Dict[str, Any]]) -> None:
        """Save merged pixel data to PostgreSQL using upsert"""
        upsert_sql = """
        INSERT INTO frames (canvas_id, data) 
        VALUES (%s, %s)
        ON CONFLICT (canvas_id) 
        DO UPDATE SET data = %s, created_at = CURRENT_TIMESTAMP
        """
        
        with self.conn.cursor() as cur:
            # JSON serialize the data
            json_data = json.dumps(data)
            
            # Execute upsert
            cur.execute(upsert_sql, (canvas_id, json_data, json_data))
            self.conn.commit()
            
        logger.info(f"Saved {len(data)} pixels to PostgreSQL for canvas {canvas_id}")

    def process_kafka_message(self, msg_value: bytes) -> None:
        """Process a single Kafka message"""
        try:
            logger.info(f"Received message from kafka")
            
            # Parse incoming data
            message_json = json.loads(msg_value.decode('utf-8'))
            
            # Extract canvas ID
            canvas_id = message_json.get('canvasId', 'default')
            
            # Extract pixels from the nested structure
            if 'updatedCells' in message_json and 'value' in message_json['updatedCells']:
                # Get pixels from the nested structure
                new_pixels = message_json['updatedCells']['value']
            else:
                # Handle the case if the message format is already a direct array
                new_pixels = message_json if isinstance(message_json, list) else [message_json]
            
            # Validate pixel data
            valid_pixels = []
            for pixel in new_pixels:
                if isinstance(pixel, dict) and all(k in pixel for k in ['x', 'y', 'color']):
                    valid_pixels.append(pixel)
                else:
                    logger.warning(f"Invalid pixel format: {pixel}")
            
            if not valid_pixels:
                logger.warning("No valid pixels found in message")
                return
                
            # Get existing data
            existing_pixels = self.get_existing_pixels(canvas_id)
            
            # Merge data
            merged_pixels = merge_pixels(existing_pixels, valid_pixels)
            
            # Save merged data
            self.save_to_postgres(canvas_id, merged_pixels)
            
            logger.info(f"Processed {len(valid_pixels)} new pixels for canvas {canvas_id}, total {len(merged_pixels)} pixels after merge")
            
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON: {e}")
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
                    error_code = msg.error().code()
                    # Check if it's end of partition without using the constant
                    if error_code == -191:  # This is the numeric value for PARTITION_EOF in many Kafka clients
                        continue
                    else:
                        logger.error(f"Kafka error: {msg.error()} (code: {error_code})")
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
        'bootstrap.servers': 'kafka:29092',
        'group.id': 'image-processing-group',
        'auto.offset.reset': 'earliest'
    }
    
    postgres_config = {
        'host': 'db',
        'database': 'postgresql',
        'user': 'postgres',
        'password': '4000'
    }
    
    pipeline = KafkaToPostgresPipeline(kafka_config, postgres_config)
    try:
        pipeline.connect_kafka()
        pipeline.connect_postgres()
        pipeline.create_table_if_not_exists()
        pipeline.consume_from_kafka('db-draw')
    except Exception as e:
        logger.error(f"Pipeline failed: {e}")