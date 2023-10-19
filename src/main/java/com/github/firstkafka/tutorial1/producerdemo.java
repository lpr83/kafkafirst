package com.github.firstkafka.tutorial1;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class producerdemo {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final Logger logger = LoggerFactory.getLogger(producerdemo.class);

        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9093");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,"true");
        properties.setProperty(ProducerConfig.ACKS_CONFIG,"all");
        properties.setProperty(ProducerConfig.RETRIES_CONFIG,Integer.toString(Integer.MAX_VALUE));
        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,"5");
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG,"snappy");
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG,"50");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG,Integer.toString(32 * 1024));
        


        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
        for (Integer i = 10; i < 20; i++) {
            String key = "id_"+ i.toString();
            String value = "hello"+ i.toString();
            ProducerRecord<String, String> record = new ProducerRecord<String, String>("first_topic", key,value);
            logger.info("sent:" + record.value().toString()+"\n");
            producer.send(record, new Callback() {
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    if (e == null) {
                        logger.info("Received data" + "\n" +
                                "Topic:" + recordMetadata.topic() + "\n" +
                                "Part:" + recordMetadata.partition() + "\n" +
                                "Offset:" + recordMetadata.offset() + "\n" +
                                "Time:" + recordMetadata.timestamp()+ "\n"
                        );

                    } else {
                        logger.info("Error", e);
                    }
                }
            }).get();
        }
        producer.flush();
        producer.close();
    }
}