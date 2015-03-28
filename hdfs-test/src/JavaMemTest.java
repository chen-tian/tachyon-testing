import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.lang.reflect.Field;
import sun.misc.Unsafe;

public class JavaMemTest {
    private static Unsafe unsafe;
    private static long memPtr;
    private static long bufPtr;
    private static long memSize;
    private static long blockSize;
    private static long numIters;
    private static Timer t;

    private static void ResInit () {
	try {
	    unsafe = getUnsafe();
	    memPtr = unsafe.allocateMemory(memSize);
	    bufPtr = unsafe.allocateMemory(blockSize);
	    unsafe.setMemory(memPtr, memSize, (byte)0);
	    unsafe.setMemory(bufPtr, blockSize, (byte)1);
	    numIters = memSize / blockSize;
	    t = new Timer();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private static void ResDestroy () {
	try {
	    unsafe.freeMemory(memPtr);
	    unsafe.freeMemory(bufPtr);
	} catch (Exception e) {
	    e.printStackTrace ();
	}
    }

    private static Unsafe getUnsafe() throws Exception {
	Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
	field.setAccessible(true);
	return (sun.misc.Unsafe) field.get(null);
    }

    private static void ReadAlloc () {
	try {
	    t.start(0);
	    for (long i=0; i<numIters; i++) {
		unsafe.copyMemory(memPtr+i*blockSize, bufPtr, blockSize);
	    }
	    t.end(0);
	    t.dump();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private static void WriteAlloc () {
	try {
	    t.start(0);
	    for (long i=0; i<numIters; i++) {
		unsafe.copyMemory(bufPtr, memPtr+i*blockSize, blockSize);
	    }
	    t.end(0);
	    t.dump();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private static void ReadMMap () {
	try {
	    byte [] bbuf = new byte [(int)blockSize];
	    String fname = Long.toString(memSize>>30);
	    File file = new File(fname);
	    FileChannel fc = new RandomAccessFile(file,"r").getChannel();
	    MappedByteBuffer buf = fc.map(FileChannel.MapMode.READ_ONLY, 
					  0, fc.size());
	    if (buf.isLoaded()) {
		t.start(0);
		for (long i=0; i<numIters; i++) {
		    buf.get(bbuf);
		}
		t.end(0);
		t.dump();
	    }
	    buf.clear();
	    fc.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private static void WriteMMap () {
	try {
	    byte [] bbuf = new byte [(int)blockSize];
	    String fname = Long.toString(memSize>>30);
	    File file = new File(fname);
	    FileChannel fc = new RandomAccessFile(file,"rw").getChannel();
	    MappedByteBuffer buf = fc.map(FileChannel.MapMode.READ_WRITE, 
					  0, fc.size());
	    if (buf.isLoaded()) {
		t.start(0);
		for (long i=0; i<numIters; i++) {
		    buf.put(bbuf);
		}
		t.end(0);
		t.dump();
	    }
	    buf.clear();
	    fc.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static void main (String[] args) {
	if (args.length < 4) {
	    System.out.println("./java_mem_test [size in gigs] [block size in bytes] [test type read|write] [mem type alloc|mmap]");
	    return;
	}
	memSize = (Long.parseLong(args[0]))<<30;
	blockSize = (Long.parseLong(args[1]));
	ResInit();

	if (args[2].equals("read")) {
	    if (args[3].equals("alloc")) {
		ReadAlloc();
	    } else if (args[3].equals("mmap")) {
		ReadMMap();
	    } else {
		System.err.println("Unrecognized Mem Type: "+args[3]);
	    }
	} else if (args[2].equals("write")) {
	    if (args[3].equals("alloc")) {
		WriteAlloc();
	    } else if (args[3].equals("mmap")) {
		WriteMMap();
	    } else {
		System.err.println("Unrecognized Mem Type: "+args[3]);
	    }
	} else {
	    System.err.println("Unrecognized Test: "+args[2]);
	}
	
	ResDestroy();
	return;
    }
}