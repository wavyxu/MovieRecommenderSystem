import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
/**
 * Created by Wei Xu on 3/12/18.
 */
public class CoOccurrenceMatrixGenerator {
    public static class MatrixGeneratorMapper extends Mapper<LongWritable, Text, Text, IntWritable> {

        // map method
        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            //value = userid\t movie1:rating movie2:rating....
            //[1,2,3] -> 1,1 1,2 1,3 2,1 2,2 2,3...
            String line = value.toString().trim();
            String[] user_movieRatings = line.split("\t");
            if(user_movieRatings.length != 2) {
                return;
            }

            String[] movie_ratings = user_movieRatings[1].split(",");
            for(int i = 0; i < movie_ratings.length; i++) {
                String movie1 = movie_ratings[i].trim().split(":")[0];
                for (int j = 0; j < movie_ratings.length; j++) {
                    String movie2 = movie_ratings[j].trim().split(":")[0];
                    //movie1:movie2 1
                    context.write(new Text(movie1 + ":" +movie2), new IntWritable(1));
                }
            }

        }
    }

    public static class MatrixGeneratorReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        // reduce method
        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {

            int sum = 0;
            for (IntWritable value: values) {
                sum += value.get();
            }
            context.write(key, new IntWritable(sum));
        }
    }

    public static void main(String[] args) throws Exception{

        Configuration conf = new Configuration();

        Job job = Job.getInstance(conf);
        job.setMapperClass(MatrixGeneratorMapper.class);
        job.setReducerClass(MatrixGeneratorReducer.class);

        job.setJarByClass(CoOccurrenceMatrixGenerator.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        TextInputFormat.setInputPaths(job, new Path(args[0]));
        TextOutputFormat.setOutputPath(job, new Path(args[1]));

        job.waitForCompletion(true);

    }
}
