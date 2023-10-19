package github.firstkafka.tutorial1;


import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class consumerdemoseek {
    private Logger logger = LoggerFactory.getLogger(consumerdemoseek.class.getName());
    public static void main(String[] args) {
            new consumerdemoseek().run();
    }
    public consumerdemoseek() {}
    public void run(){
        String bootstrapserver= "127.0.0.1:9093";
        String groupId= "my-6-app";
        String topic= "first_topic";
        CountDownLatch latch = new CountDownLatch(1);
        logger.info("Creating thread");
        Runnable myConsumerRunnable = new ConsumerRunnable(bootstrapserver, groupId,topic ,latch);

        Thread myThread = new Thread(myConsumerRunnable);
        myThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {logger.info("cought shuutdown");
            ((ConsumerRunnable)myConsumerRunnable).shutdown();
            try {
                latch.await();
            }
            catch (InterruptedException ex) {
                logger.error("app interrupted",ex);
            }
            finally {
                logger.info("App is closing");
            }
            logger.info("App has exited");
        }));

        try {
            latch.await();
        }
        catch (InterruptedException ex) {
            logger.error("app interrupted",ex);
        }
        finally {
            logger.info("App is closing");
        }
    }
    public class ConsumerRunnable implements Runnable{
        private CountDownLatch latch;
        private KafkaConsumer<String,String> consumer;
        public ConsumerRunnable(String bootstrapserver, String groupId, String topic, CountDownLatch latch) {
            this.latch=latch;
            Properties properties = new Properties();
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapserver);
            properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,groupId );
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            consumer = new KafkaConsumer<String, String>(properties);
            consumer.subscribe(Collections.singleton(topic));
        }
        @Override
        public void run() {
            try {
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> rec : records) {
                        logger.info("Key:" + rec.key() + "\n" +
                                "Val:" + rec.value() + "\n" +
                                "Par:" + rec.partition() + "\n" +
                                "Offs:" + rec.offset()
                        );
                    }
                }
            }
            catch(WakeupException ex)
            {
                logger.info("Receive shutdown signal");
            }
            finally{
                consumer.close();
                latch.countDown();
            }
        }
        public void shutdown(){
            consumer.wakeup();
        }
    }
}
