package tutorial3;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

public class ElasticSearchConsumer {
    private static Gson gson = new Gson();
    public ElasticSearchConsumer() {

    }

    public static void main(String[] args) throws IOException {
        Logger logger = LoggerFactory.getLogger(ElasticSearchConsumer.class.getName());
        //https://bifd1g4jlr:3pppuuqski@lpracki-test-8198335644.us-east-1.bonsaisearch.net:443

        RestHighLevelClient client = createClient();
        String topic= "twitter_tweets";
        IndexRequest indexRequest = new IndexRequest("twitter");
        KafkaConsumer<String,String> kafkaConsumer=createKafkaConsumer(topic);
        while(true){
            ConsumerRecords<String,String> records = kafkaConsumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String,String> rec : records){
                String val = rec.value();
                String id_str=extractIdFromTweet(val);
                logger.info(id_str);

                indexRequest.id(id_str);
                indexRequest.source(val, XContentType.JSON);

                IndexResponse indexResponse = client.index(indexRequest,RequestOptions.DEFAULT);

                String id = indexResponse.getId();
                logger.info(id);
                try{
                    Thread.sleep(10000);
                }
                catch (InterruptedException ex){
                    ex.printStackTrace();
//                    client.close();
                }

            }

        }




//



    }
    public static KafkaConsumer<String,String> createKafkaConsumer(String topic){
        String bootstrapserver= "127.0.0.1:9093";
        String groupId= "my-7-app";


        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapserver);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,groupId );
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<String, String>(properties);
        kafkaConsumer.subscribe(Arrays.asList(topic));
        return kafkaConsumer;
    }
    public static RestHighLevelClient createClient(){
        String host="lpracki-test-8198335644.us-east-1.bonsaisearch.net";
        String user="bifd1g4jlr";
        String password="3pppuuqski";
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,new UsernamePasswordCredentials(user,password));
        RestClientBuilder builder = RestClient.builder(new HttpHost(host,443,"https")).setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                return httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
        });
        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }
    public static String extractIdFromTweet(String tweet){
        JsonTweet obj2 = gson.fromJson(tweet, JsonTweet.class);
        return obj2.id_str;
    }
    class JsonTweet
    {
        String id_str;
    }
    
}
