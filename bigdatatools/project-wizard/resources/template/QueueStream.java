package $$$FILE_PACKAGE$$$;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public final class QueueStream {
    public static void main(String[] args) throws Exception {
        SparkSession spark = SparkSession.builder().appName("JavaSparkPi").getOrCreate();
        // Create the context with a 1 second batch size
        JavaStreamingContext ssc = new JavaStreamingContext(spark.sparkContext().conf(), Durations.seconds(1));


        // Create and push some RDDs into the queue
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }

        Queue<JavaRDD<Integer>> rddQueue = new LinkedList<>();
        for (int i = 0; i < 30; i++) {
            rddQueue.add(ssc.sparkContext().parallelize(list));
        }

        // Create the QueueInputDStream and use it do some processing
        JavaDStream<Integer> inputStream = ssc.queueStream(rddQueue);
        JavaPairDStream<Integer, Integer> mappedStream = inputStream.mapToPair(i -> new Tuple2<>(i % 10, 1));
        JavaPairDStream<Integer, Integer> reducedStream = mappedStream.reduceByKey(Integer::sum);

        reducedStream.print();
        ssc.start();
        ssc.awaitTermination();
    }
}