import cc.mallet.pipe.*;
import cc.mallet.types.*;
import cc.mallet.cluster.*;
import cc.mallet.pipe.iterator.CsvIterator;

import java.io.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import java.util.List;


/**
 * Same pipe is a "pipe" that returns exactly what it was given.
 **/
class samePipe extends Pipe {
    public Instance pipe(Instance carrier) {
        return carrier;
    }
}

/**
 * KMeans app is a program that takes in a filename and the k-values
 * that the user wants to run the k-means algorithm on. The file is
 * to be formatted with the each line containing identifying name of
 * the vector, a tab, then the vector with each data entry seperated
 * by a comma.
 *
 * The program generates files with the name "filename" that was taken 
 * in followed by "_k" amount that was specified, in the tsv file format.
 * 
 * Example usage: java KMeans test 3 4 5
 * This will run the k-means algorithm on the data formatted in the test
 * file with k values of 3, 4, and 5. Files named test_3.tsv, test_4.tsv,
 * and test_5.csv will be generated with the appropriate clusters
 * printed on each row.
 *
 * @author Timothy Hong, Neeraj Mallampet
 */
public class KMeans {
    
    //Parse in the double array split by the delimiter
    public static double[] parse_double_arr(String vector, char delimiter) {
        double[] data = new double[232];
        try {
            String[] temp = vector.split("" + delimiter);
            for(int i = 0; i < temp.length; i++) {
                data[i] = Double.parseDouble(temp[i]);
            }
        }
        catch(Exception e) {
            System.err.println("parse_double_arr error: Not a number: " + vector);
            System.exit(1);
        }
        return data;
    }
    
    public static void main(String[] args) {
        try {
            if(args.length < 2) {
                System.err.println("Usage: filename k1 ...");
                System.exit(1);
            }
            
            //Read in the file into a list of strings
            List<String> lines = Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8);
            
            
            //Mallet requires an Alphabet to be created to hash values for
            //quicker access during k-means
            Alphabet A = new Alphabet(0);
            InstanceList instances = new InstanceList(A, null);
            
            //Make a pipe for the instances that will return itself during the kmeans algorithm
            samePipe same = new samePipe();
            instances.setPipe(same);
            
            //Split the lines into the two parts 
            //Create each instance, give it the name from part 0 
            //and the data from part 1 stored inside a Feature Vector
            for(String line: lines) {
                String[] parts = line.split("\\t");
                double[] data = parse_double_arr(parts[1], ',');
                instances.add(new Instance(new FeatureVector(A, data),
                              null,
                              parts[0],
                              null)); 
            }
            
            //For each k value specified, create a new file with k clusters
            //each seperated by row
            for(int num_k = 1; num_k < args.length; num_k++) {
                //The k-means should not change the instances at all, 
                //and if an empty cluster were to occur, 
                //add the furthest instance away from previous cluster
                //to this one.
                KMeans k = new KMeans(same,
                                      Integer.parseInt(args[num_k]),
                                      new NormalizedDotProductMetric(),
                                      KMeans.EMPTY_SINGLE);
                                      
                Clustering cluster = k.cluster(instances);
                
                //Create a file to write the results of this k out to
                PrintWriter outFile = new PrintWriter(new OutputStreamWriter(
                                                      new FileOutputStream(args[0] + "_" + args[num_k] + ".tsv"),
                                                      "UTF-8"));
                
                //Seperate each entry by a tab
                for(InstanceList list: cluster.getClusters()) {
                    for(int i = 0; i < list.size(); i++) {
                        if(i != 0) { outFile.print("\t"); }
                        outFile.print(list.get(i).getName());
                    }
                    outFile.println();
                }
                outFile.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    
    }
    
}