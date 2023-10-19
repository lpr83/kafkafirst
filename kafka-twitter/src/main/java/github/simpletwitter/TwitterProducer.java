package github.simpletwitter;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TwitterProducer {
    Logger logger  = LoggerFactory.getLogger(TwitterProducer.class.getName());
    public static void main(String[] args) {
        new TwitterProducer().Run();
    }

    public TwitterProducer() {
    }
    public void Run(){

        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(1000);
        Client cl = createTwitterClient(msgQueue);
        cl.connect();

        KafkaProducer<String,String> kafkaProducer = createKafkaProducer();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("stopping");
            cl.stop();
            logger.info("client stopped");
            kafkaProducer.close();
            logger.info("producer stopped");
        }));

        while (!cl.isDone()) {
            String msg = null;
            try{
                msg = msgQueue.poll(5, TimeUnit.SECONDS);
            }
            catch (InterruptedException ex){
                ex.printStackTrace();
                cl.stop();
            }
            if(msg!=null){
                kafkaProducer.send(new ProducerRecord<>("twitter_tweets", null, msg), new Callback() {
                            @Override
                            public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                                if(e!=null){
                                    logger.error("dupa");
                                }
                            }
                        });
                logger.info(msg);
            }
        }

    }

    private KafkaProducer<String, String> createKafkaProducer() {
        String bootstrapServer = "127.0.0.1:9093";

        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return new KafkaProducer<String, String>(properties);
    }

    public Client createTwitterClient(BlockingQueue<String> msgQueue ){
        /** Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */


        /** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
// Optional: set up some followings and track terms
        List<String> terms = Lists.newArrayList("bitcoin");
        hosebirdEndpoint.trackTerms(terms);

        String consumerKey="WQJtDvB3XtJablMUf5iuqE6eb";
        String consumerSecret="bgQAvBpiKf7UtmXAeuR0iRiowdCXlQ2K3ackr6SH8Jr6PGvELv";
        String token="1342226366155137025-FdOEDP8oEtEWPGWXEx9qDDFAQYFF5O";
        String secret="Xmg5EAr1Wt3Jr03ciEOQhKAibxab8hqmByKkGOCozYFWI";
        // These secrets should be read from a config file
        Authentication hosebirdAuth = new OAuth1(consumerKey, consumerSecret, token, secret);

        ClientBuilder builder = new ClientBuilder()
                .name("Client-01")                              // optional: mainly for the logs
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));

        Client hosebirdClient = builder.build();
        return hosebirdClient;
// Attempts to establish a connection.

    }
}
