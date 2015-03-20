/*
 * Licensed to the University of California, Berkeley under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
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

package com.hadoop.hdfs_test;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.util.ToolRunner;

public class HdfsWriter extends Configured implements Tool {
    public static final String FS_PARAM_NAME = "fs.defaultFS";

    public int run (String[] args) throws Exception {
	
	if (args.length < 1) {
	    System.err.println ("HdfsWriter [fileSize ie. 1g/10g/40g]");
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
       
	String localFolder = "/home/hduser/projects/hdfs_test/input/";
	String hdfsFolder = "/hdfs_test/";
	int numFiles = 5;
	short replication = 1;
	String hdfsFile;
	long startTime, endTime, duration = 0;
	long avg = 0, min = Long.MAX_VALUE, max = Long.MIN_VALUE;
	String fileName = args[0]+"-avg.txt";
	File avgFile = new File (fileName);
	PrintWriter avgPW = new PrintWriter(avgFile);
	fileName = args[0]+"-min.txt";
	File minFile = new File (fileName);
	PrintWriter minPW = new PrintWriter (minFile);
	fileName = args[0]+"-max.txt";
	File maxFile = new File (fileName);
	PrintWriter maxPW = new PrintWriter (maxFile);

	boolean overWrite = true;
	int bufferSize[] = new int[] {4096, 16384, 65536, 262144};
	long blockSize[] = new long[] {67108864, 134217728, 268435456};

	Configuration conf = getConf ();
	System.out.println("configured filesystem = " + conf.get(FS_PARAM_NAME));
	FileSystem fs = FileSystem.get (conf);

	for (int i=0; i<4; i++){ // different buffer size
	    for (int j=0; j<3; j++) { // different block size
		double numIters = fileSize / (double)bufferSize[i];
		byte[] buf = new byte [bufferSize[i]];
		for (int m = 0; m < bufferSize[i]; m+=4) {
		    buf[m] = (byte) m;
		}

		for (int k=1; k<=numFiles; k++) {
		    hdfsFile = hdfsFolder + args[0] + "/" + i + ".in";
		    Path outputPath = new Path (hdfsFile);
		    OutputStream os = fs.create(outputPath, overWrite, bufferSize[i], replication, blockSize[j]);
		    startTime = System.currentTimeMillis ();
		    for (long m=0; m<numIters; m++) {
			os.write (buf);
		    }
		    endTime = System.currentTimeMillis ();
		    os.close ();
		    fs.delete (outputPath, true);

		    duration = endTime - startTime;
		    avg += duration;
		    if (duration < min) {
			min = duration;
		    }
		    if (duration > max) {
			max = duration;
		    }
		}
		// write result to output
		Double avgBW = fileSizeInMB*1000.0*(double) numFiles/(double)avg; 
		avgPW.print(avgBW);
		avgPW.print("\t");
		double minBW = fileSizeInMB*1000.0/(double)max; 
		minPW.print (minBW);
		minPW.print ("\t");
		double maxBW = fileSizeInMB*1000.0/(double)min;
		maxPW.print (maxBW);
		maxPW.print ("\t");
		
		duration = 0;
		avg = 0;
		min = Long.MAX_VALUE;
		max = Long.MIN_VALUE;
	    }
	    avgPW.println();
	    minPW.println();
	    maxPW.println();
	}

	//avgFile.close();
	avgPW.close();
	//minFile.close();
	minPW.close();
	//maxFile.close();
	maxPW.close();
	/*
	System.out.println ("avg: " + (fileSizeInMB*1000.0*(double)numFiles/(double)avg)
			    + " max: " + (fileSizeInMB*1000.0)/(double)min
			    + " min: " + (fileSizeInMB*1000.0)/(double)max);
	*/
	return 0;
    }

    public static void main (String[] args) throws Exception {
	int returnCode = ToolRunner.run(new HdfsWriter(), args);
	System.exit (returnCode);
    }
}
