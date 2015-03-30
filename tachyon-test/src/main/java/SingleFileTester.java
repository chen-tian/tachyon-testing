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



import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Callable;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import tachyon.Constants;
import tachyon.TachyonURI;
import tachyon.Version;
import tachyon.client.OutStream;
import tachyon.client.TachyonByteBuffer;
import tachyon.client.TachyonFile;
import tachyon.client.TachyonFS;
import tachyon.client.WriteType;
import tachyon.client.FileInStream;
import tachyon.client.ReadType;
import tachyon.conf.TachyonConf;

public class SingleFileTester {
  //private static final Logger LOG = LoggerFactory.getLogger(Constants.LOGGER_TYPE);

  private final TachyonURI mMasterLocation;
  private final TachyonURI mFilePath;
  private final WriteType mWriteType;
  private final TachyonFS mTachyonClient; 
  private final int mBufferSize= 64*1024;
  private final double mFileSize;

  public SingleFileTester(TachyonURI masterLocation, TachyonURI filePath, WriteType writeType, double fsize) 
  throws Exception {
    mMasterLocation = masterLocation;
    mFilePath = filePath;
    mWriteType = writeType;
    mTachyonClient = TachyonFS.get(mMasterLocation, new TachyonConf());
    mFileSize= fsize;
  }

  private void createFile() throws IOException {
    if (mTachyonClient.exist(mFilePath)) {
       mTachyonClient.delete(mFilePath, false);
       System.out.println("Delete exisitn file " + mFilePath);
    }
    int fileId = mTachyonClient.createFile(mFilePath);
    System.out.println("Creating file " + mFilePath + " id =" + fileId );
  }

  private void writeFile() throws IOException {
    System.out.println("Start writing " + mFilePath);
  // prepare input data
    ByteBuffer buf = ByteBuffer.allocate(mBufferSize);
    buf.order(ByteOrder.nativeOrder());
    for (int k = 0; k < mBufferSize/Integer.SIZE ; k ++) {
      buf.putInt(k);
    }

    buf.flip();

  // we just open one file for the moment
    Timer t = new Timer();
    TachyonFile file = mTachyonClient.getFile(mFilePath);
    OutStream os = file.getOutStream(mWriteType);

    double numIters = mFileSize / (double)mBufferSize; 

    t.start(0);
    for (int i=0; i<numIters; i++) {
      os.write(buf.array());
      buf.flip();
    }
    t.end(0);
    t.dump();
    os.close();
    System.out.println("Finish writing " + mFileSize/(1024*1024) + " MB data to file " + mFilePath);
  }

    private void readFile() throws IOException {
	System.out.println("Start Reading " + mFilePath);
  
  // we just open one file for the moment
    Timer t = new Timer();
    TachyonFile file = mTachyonClient.getFile(mFilePath);
    int numIters = (int)mFileSize / mBufferSize;
    FileInStream is = (FileInStream) file.getInStream(ReadType.CACHE);
    byte[] buf = new byte [mBufferSize];
    t.start(0);
    
    int ret = is.read(buf);
    while (ret != -1) {
	ret = is.read(buf);
    }
    
    t.end(0);
    t.dump();
    is.close();
    //System.out.println("Finish reading " + mFileSize/(1024*1024) + " MB data to file " + mFilePath);
    }

  private void deleteFile() throws IOException {
    mTachyonClient.delete(mFilePath, false);
    System.out.println("Deleting file " + mFilePath);
  }

  public static void main(String[] args) throws IllegalArgumentException {
    if (args.length != 4) {
      System.out.println("java -cp pdct-test-1.0.jar  " 
         + "SingleFileTester <TachyonMasterAddress> <FilePath> <WriteType> <FileSize>");
      System.exit(-1);
    }

   double fileSize = Double.parseDouble((args[3].split("g|G"))[0])*1024*1024*1024;
   SingleFileTester sfw;
   try {
    sfw = new SingleFileTester(new TachyonURI(args[0]), new TachyonURI(args[1]), 
	WriteType.valueOf(args[2]), fileSize);
    sfw.createFile();
    sfw.writeFile();
    sfw.readFile();
    //sfw.deleteFile();
   } catch (Exception e) {
     System.err.println("Exception: " + e.getMessage());
   }
   return;
  }
}
