#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>

int main (int argc, char *argv[]) {
  if (argc < 4) {
    printf("Usage: ./memory_test mem_size buf_size r|w\n");
    return 0;
  }
  long int bytes = (atol(argv[1]) << 30);
  long int bufSize = atol(argv[2]);
  long int numIters = bytes / bufSize;
  long int i, j;
  struct timeval start, end;
  float duration;

  char *buf = (char *)malloc(bufSize);
  memset(buf, 0, sizeof(char)*bufSize);
  char *mem = (char *)malloc(bytes);
  memset(mem, 0, sizeof(char)*bytes);

  if (!strcmp(argv[3],"w")) {
      for (i=0; i<5; i++) {
	  gettimeofday(&start, NULL);
	  for (j=0; j<numIters; j++)
	      memcpy(&mem[j*bufSize], buf, bufSize);
	  gettimeofday(&end, NULL);
	  
	  duration = (float)((end.tv_sec - start.tv_sec)*1000000 + 
			     (end.tv_usec - start.tv_usec))/1000.0;
	  printf("%f\n",duration);
      }
  } else if (!strcmp(argv[3],"r")){
      for (i=0; i<5; i++) {
	  gettimeofday(&start, NULL);
	  for (j=0; j<numIters; j++)
	      memcpy(buf, &mem[j*bufSize], bufSize);
	  gettimeofday(&end, NULL);
	  
	  duration = (float)((end.tv_sec - start.tv_sec)*1000000 + 
			     (end.tv_usec - start.tv_usec))/1000.0;
	  printf("%f\n",duration);
      }
  } else {
      printf("Unrecognized test type\n");
  }
  free(buf);
  free(mem);
  return 0;
}
