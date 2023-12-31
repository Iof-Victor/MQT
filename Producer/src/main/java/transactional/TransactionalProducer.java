package transactional;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionalProducer {

	private static final Logger LOG = LoggerFactory.getLogger(TransactionalProducer.class);

	private static final String BOOTSTRAP_SERVERS = ":9092";
	private static final String TOPIC1 = "events1";
	private static final String TOPIC2 = "events2";
	private static final String CLIENT_ID = "ex37";

	@SuppressWarnings("boxing")
	public static void main(String[] args) {

		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, CLIENT_ID);
		props.put(ProducerConfig.ACKS_CONFIG, "all");
		props.put(ProducerConfig.LINGER_MS_CONFIG, 0);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "my.id");
		props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");


		try (Producer<String, String> producer = new KafkaProducer<>(props)) {
			producer.initTransactions();
			producer.beginTransaction();
			final ProducerRecord<String, String> data1 = new ProducerRecord<>(TOPIC1, "trans_value1");
			final ProducerRecord<String, String> data2 = new ProducerRecord<>(TOPIC2, "trans_value2");
			try {
				RecordMetadata meta1 = producer.send(data1).get();
				LOG.info("-------------------partition = {}, key = {}, value = {} => partition = {}, offset= {}", data1.topic(), data1.key(), data1.value(), meta1.partition(), meta1.offset());
				RecordMetadata meta2 = producer.send(data2).get();
				LOG.info("ooooooooooooooooo partition = {}, key = {}, value = {} => partition = {}, offset= {}", data1.topic(), data2.key(), data2.value(), meta2.partition(), meta2.offset());
				Thread.sleep(10000);
				producer.commitTransaction();
			} catch (InterruptedException | ExecutionException e) {
				producer.abortTransaction();
				LOG.error("Something goes wrong: {}", e.getMessage(), e);
			} finally {
				producer.flush();
			}
		}
	}
}
