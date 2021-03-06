/*
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
//package com.hadoop.hdfs_test;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.util.EnumSet;
import java.nio.ByteBuffer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.ReadOption;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.io.ElasticByteBufferPool;
import org.apache.hadoop.fs.FSDataInputStream;

public class HdfsCacheReader extends Configured implements Tool {
    public static final String FS_PARAM_NAME = "fs.defaultFS";
    
    public int run(String[] args) throws Exception {
	if (args.length < 1) {
	    System.err.println ("HdfsReader [FileSize i.e. 1g/10g/100g/200g]");
	    return 1;
	}

	double fileSize;
        double fileSizeInMB;
        if (args[0].equals("1g")) {
            fileSize = 1073741824.0;
            fileSizeInMB = 1024.0;
        } else if (args[0].equals("10g")) {
            fileSize = 10737418240.0;
            fileSizeInMB = 10240.0;
        } else if (args[0].equals("100g")) {
            fileSize = 107374182400.0;
            fileSizeInMB = 102400.0;
        } else if (args[0].equals("200g")) {
            fileSize = 214748364800.0;
            fileSizeInMB = 204800.0;
        }
        else {
            throw new IllegalArgumentException("Invalid arg: " + args[0]);
        }

	String fileName = "cacheRead-" + args[0] + "-avg.txt";
	File avgFile = new File (fileName);
	PrintWriter avgPW = new PrintWriter (avgFile);
	fileName = "cacheRead-" + args[0] + "-min.txt";
	File minFile = new File (fileName);
	PrintWriter minPW = new PrintWriter (minFile);
	fileName = "cacheRead-" + args[0] + "-max.txt";
	File maxFile = new File (fileName);
	PrintWriter maxPW = new PrintWriter (maxFile);

	int numIters = 10;
	int bufferSize = 65536;
	long blockSize[] = new long[] {67108864, 134217728, 268435456, 536870912, 1073741824};
	short replication[] = new short[] {1, 4};
	String hdfsFile = "/hdfs_test/" + args[0] + "/1.in";
	Configuration conf = getConf ();
	FileSystem fs = FileSystem.get (conf);
	Path hdfsFilePath = new Path (hdfsFile);

	for (int i=0; i<5; i++) { // blockSize
	    for (int j=0; j<2; j++) { // replication
		OutputStream os = fs.create (hdfsFilePath, true, bufferSize, replication[j], blockSize[i]);
		byte[] buf = new byte [bufferSize];
		for (int m=0; m<bufferSize; m+=4) {
		    buf[m] = (byte) m;
		}
		double numBufPerFile = fileSize / (double)bufferSize;

		for (double m=0.0; m<numBufPerFile; m++) {
		    os.write (buf);
		}
		os.close ();
		String cmdStr = "/usr/local/hadoop/bin/hdfs cacheadmin -addDirective -path " + hdfsFile + " -pool hdfs_test";
		Process p = Runtime.getRuntime().exec(cmdStr);
		p.waitFor ();
		String cmdOutLine = "";
		StringBuffer cmdOut = new StringBuffer ();
		BufferedReader cmdOutReader = new BufferedReader (new InputStreamReader(p.getInputStream()));
		while ((cmdOutLine = cmdOutReader.readLine()) != null) {
		    cmdOut.append(cmdOutLine + "\n");
		}
		// System.out.println (cmdOut.toString());

		long avg = 0, min = Long.MAX_VALUE, max = Long.MIN_VALUE;
		for (int k=0; k<numIters; k++) {
		    FSDataInputStream in = fs.open (hdfsFilePath);
		    ByteBuffer bbuf = null;
		    ElasticByteBufferPool ebbp = new ElasticByteBufferPool ();
		    long startTime = System.currentTimeMillis();
		    while ((bbuf = in.read(ebbp, bufferSize, EnumSet.of(ReadOption.SKIP_CHECKSUMS))) != null) {
			in.releaseBuffer (bbuf);
		    }
		    long endTime = System.currentTimeMillis();
		    in.close ();
		    long duration = (endTime - startTime);
		    avg += duration;
		    if (duration < min) {
			min = duration;
		    }
		    if (duration > max) {
			max = duration;
		    }
		}
		// write result to output
		double avgBW = fileSizeInMB * 1000.0 * (double)numIters/ (double) avg;
		avgPW.print(avgBW);
		avgPW.print("\t");
		double minBW = fileSizeInMB * 1000.0 / (double) max;
		minPW.print(minBW);
		minPW.print("\t");
		double maxBW = fileSizeInMB * 1000.0 / (double) min;
		maxPW.print(maxBW);
		maxPW.print("\t");
		cmdStr = "/usr/local/hadoop/bin/hdfs cacheadmin -removeDirectives -path " + hdfsFile;
		p = Runtime.getRuntime().exec(cmdStr);
		p.waitFor ();
		cmdOutLine = "";
		cmdOut.setLength (0);
		cmdOutReader = new BufferedReader (new InputStreamReader(p.getInputStream()));
		while ((cmdOutLine = cmdOutReader.readLine()) != null) {
		    cmdOut.append(cmdOutLine + "\n");
		}
		// System.out.println (cmdOut.toString());
		fs.delete (hdfsFilePath, true);
	    }
	    avgPW.println();
	    minPW.println();
	    maxPW.println();
	}
	avgPW.close();
	minPW.close();
	maxPW.close();
	return 0;
    }

    public static void main (String[] args) throws Exception {
	int returnCode = ToolRunner.run (new HdfsCacheReader(), args);
	System.exit (returnCode);
    }
}