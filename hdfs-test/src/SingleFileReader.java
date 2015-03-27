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


import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.util.ToolRunner;

public class SingleFileReader extends Configured implements Tool {
    private Path hdfsFilePath;
    FileSystem fs;
    double fileSize;
    int bufferSize = 65536;
    Timer t;

    private void writeFile (String fSize) throws Exception {
	fileSize = Double.parseDouble((fSize.split("g|G"))[0])
	    *1024*1024*1024;
	String hdfsFolder = "/hdfs_test/";
	String hdfsFile = hdfsFolder + fSize;
	short replication = 1;
	boolean overWrite = true;
	int blockSize = 536870912;
	double numIters = fileSize/(double)bufferSize;

	Configuration conf = getConf ();
	fs = FileSystem.get (conf);
	hdfsFilePath = new Path (hdfsFile);
	OutputStream os = fs.create(hdfsFilePath, overWrite, bufferSize, 
				    replication, blockSize);

	/* Initialize byte buffer */
	ByteBuffer buf = ByteBuffer.allocate(bufferSize);
	buf.order(ByteOrder.nativeOrder());
	for (int k=0; k<bufferSize/Integer.SIZE; k++) {
	    buf.putInt(k);
	}
	buf.flip();

	/* Write the content of the byte buffer 
	 to the HDFS file*/
	t = new Timer();
	t.start(0);
	for (long i=0; i<numIters; i++) {
	    os.write(buf.array());
	    buf.flip();
	}
	t.end(0);
	os.close();
    }

    private void seqRead () throws Exception {
	//bufferSize = 4; /*Tachyon reads one int a time*/
	FSDataInputStream is = fs.open(hdfsFilePath);
	// byte[] bbuf = new byte[bufferSize];
	ByteBuffer buf = ByteBuffer.allocate(bufferSize);
	t.start(1);
	int bytesRead = is.read(buf);
	buf.flip();
	while(bytesRead != -1) {
	    bytesRead = is.read(buf);
	    buf.flip();
	}
	t.end(1);
	is.close();
    };

    private void randRead () throws Exception {
	//bufferSize = 4; /*Tachyon reads one int a time*/
	FSDataInputStream is = fs.open(hdfsFilePath);
	// byte[] bbuf = new byte[bufferSize];
	ByteBuffer buf = ByteBuffer.allocate(bufferSize);
	double offsetMax = fileSize - bufferSize - 1;
	long offset = (long)(Math.random()*offsetMax);
	long numIters = (long)(fileSize / bufferSize);
	t.start(1);
	while (numIters != 0) {
	    /*
	    if (numIters % 500 == 0) {
		System.out.println(offset);
	    }
	    */
	    is.seek(offset);
	    int bytesRead = is.read(buf);
	    buf.flip();
	    offset = (long)(Math.random()*offsetMax);
	    numIters = numIters - 1;
	}
	t.end(1);
	is.close();
    }

    public int run (String[] args) throws Exception {	
	if (args.length < 2) {
	    System.err.println ("SingleFileReader [fileSize ie. 1g/10g/100g] [ReadType ie. seq/rand]");
	    return 1;
	}

	/* Create file on HDFS */
	writeFile(args[0]);
	
	/* Read the same file from HDFS */
	if (args[1].equals("seq")) {
	    seqRead();
	} else if (args[1].equals("rand")) {
	    randRead();
	} else {
	    System.err.println("Unknown read type. No read is performed");
	}

	/* Clean up */
	fs.delete(hdfsFilePath, true);
	t.dump();
	
	return 0;
    }

    public static void main (String[] args) throws Exception {
	int returnCode = ToolRunner.run(new SingleFileReader(), args);
	System.exit (returnCode);
    }
}
