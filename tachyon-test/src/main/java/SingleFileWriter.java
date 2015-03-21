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
import tachyon.conf.TachyonConf;

public class SingleFileWriter {
  //private static final Logger LOG = LoggerFactory.getLogger(Constants.LOGGER_TYPE);

  private final TachyonURI mMasterLocation;
  private final TachyonURI mFilePath;
  private final WriteType mWriteType;
  private final TachyonFS mTachyonClient; 
  private final int mNumbers = 20;

  public SingleFileWriter(TachyonURI masterLocation, TachyonURI filePath, WriteType writeType) 
  throws Exception {
    mMasterLocation = masterLocation;
    mFilePath = filePath;
    mWriteType = writeType;
    mTachyonClient = TachyonFS.get(mMasterLocation, new TachyonConf());
  }

  
  private void createFile() throws IOException {
    int fileId = mTachyonClient.createFile(mFilePath);
    System.out.println("Creating file " + mFilePath + " id =" + fileId );
  }

  private void writeFile() throws IOException {
    System.out.println("Start writing " + mFilePath);
    int totalBytes=0; 
    ByteBuffer buf = ByteBuffer.allocate(64*1024);
    buf.order(ByteOrder.nativeOrder());
    for (int k = 0; k < mNumbers; k ++) {
      buf.putInt(k);
    }

    buf.flip();

    TachyonFile file = mTachyonClient.getFile(mFilePath);
    OutStream os = file.getOutStream(mWriteType);
    for (int i=0; i<1024*1024; i++) {
      os.write(buf.array());
      buf.flip();
      totalBytes = totalBytes + buf.capacity();
    }
    os.close();
    System.out.println("Finish writing " + totalBytes/(1024*1024) + " MB data to file " + mFilePath);
  }

  private void deleteFile() throws IOException {
    mTachyonClient.delete(mFilePath, false);
    System.out.println("Deleting file " + mFilePath);
  }

  public static void main(String[] args) throws IllegalArgumentException {
    if (args.length != 3) {
      System.out.println("java -cp pdct-test-1.0.jar  " 
         + "SingleFileWriter <TachyonMasterAddress> <FilePath> <WriteType>");
      System.exit(-1);
    }

   SingleFileWriter sfw;
   try {
    sfw = new SingleFileWriter(new TachyonURI(args[0]), new TachyonURI(args[1]), 
	WriteType.valueOf(args[2]));
    sfw.createFile();
    sfw.writeFile();
    //sfw.deleteFile();
   } catch (Exception e) {
     System.err.println("Exception: " + e.getMessage());
   }
   return;
  }
}
