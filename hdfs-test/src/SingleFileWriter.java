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
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.util.ToolRunner;

public class SingleFileWriter extends Configured implements Tool {
  
    public int run (String[] args) throws Exception {	
	if (args.length < 1) {
	    System.err.println ("SingleFileWriter [fileSize ie. 1g/10g/100g]");
	    return 1;
	}

	double fileSize = Double.parseDouble((args[0].split("g|G"))[0])
	    *1024*1024*1024;
       
	String hdfsFolder = "/hdfs_test/";
	String hdfsFile = hdfsFolder + args[0];
	short replication = 1;
	boolean overWrite = true;
	int bufferSize = 65536;
	int blockSize = 536870912;
	double numIters = fileSize/(double)bufferSize;

	/* Initialize byte buffer */
	ByteBuffer buf = ByteBuffer.allocate(bufferSize);
	buf.order(ByteOrder.nativeOrder());
	for (int k=0; k<bufferSize/Integer.SIZE; k++) {
	    buf.putInt(k);
	}
	buf.flip();
	
	/* Create file on HDFS */
	Configuration conf = getConf ();
	FileSystem fs = FileSystem.get (conf);
	Path hdfsFilePath = new Path (hdfsFile);
	OutputStream os = fs.create(hdfsFilePath, overWrite, bufferSize, 
				    replication, blockSize);
	/* Write the content of the byte buffer 
	 to the HDFS file*/
	Timer t = new Timer();
	t.start(0);
	for (long i=0; i<numIters; i++) {
	    os.write(buf.array());
	    buf.flip();
	}
	t.end(0);
	os.close();
	fs.delete(hdfsFilePath, true);
	
	t.dump();
	
	return 0;
    }

    public static void main (String[] args) throws Exception {
	int returnCode = ToolRunner.run(new SingleFileWriter(), args);
	System.exit (returnCode);
    }
}
