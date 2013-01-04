#include <jni.h>
#include <android/log.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "triangle.h"

static void mylog(const char *msg)
{
  __android_log_write(ANDROID_LOG_INFO,"triangle", msg);
}


//#define TESTING

typedef struct triangulateio TriangleIO;

jint Java_org_quake_triangle_TriangleJNI_triangulate(JNIEnv *env, jclass c,
													 jobject point_buf,
													 jint num_rings,
													 jobject indice_buf,
													 jint offset)
{
  TriangleIO in, out;
  jfloat* points = (jfloat*)(*env)->GetDirectBufferAddress(env, point_buf);
  jshort* indices = (jshort*)(*env)->GetDirectBufferAddress(env, indice_buf);

  char buf[128];
  int i, j;

  memset(&in, 0, sizeof(TriangleIO));

  int num_points = (indices[0])>>1;
  in.numberofpoints = num_points;
  in.pointlist = (float *) points;

#ifdef TESTING
  for (i = 0; i < num_points; i++) {
  	snprintf(buf, 128, "point: %f, %f\n", points[i*2], points[i*2+1]);
  	mylog(buf);
  }
#endif
  int num_segments = num_points; // - (closed ? (num_rings - 1) : 0);
  in.segmentlist = (int *) malloc(num_segments * 2 * sizeof(int));
  in.numberofsegments = num_segments;
  in.numberofholes = num_rings - 1;
  int *rings = NULL;
  if (in.numberofholes > 0)
	{
	  in.holelist = (float *) malloc(in.numberofholes * 2 * sizeof(float));
	  rings = (int*) malloc(num_rings * sizeof(int));
	}

  int   *seg = in.segmentlist;
  float *hole = in.holelist;

  int ring;
  int point;
  // assign all points to segments for each ring
  for (ring = 0, point = 0; ring < num_rings; ring++, point++)
	{
	  int len;
	  int num_points = indices[ring+1] >> 1;

	  if (rings)
		rings[ring] = num_points;

	  // add holes: we need a point inside the hole...
	  // this is just a heuristic, assuming that two
	  // 'parallel' lines have a distance of at least
	  // 1 unit. you'll notice when things went wrong
	  // when the hole is rendered instead of the poly
	  if (ring > 0)
		{
		  int k = point * 2;

		  float cx = in.pointlist[k+0];
		  float cy = in.pointlist[k+1];

		  float nx = in.pointlist[k+2];
		  float ny = in.pointlist[k+3];

		  float vx = nx - cx;
		  float vy = ny - cy;

		  float a = sqrt(vx*vx + vy*vy);

		  // fixme: need to check a == 0?
		  //if (a > 0.00001 || a < -0.0001)

		  float ux = -vy / a;
		  float uy = vx / a;

		  float centerx = cx + vx / 2 - ux;
		  float centery = cy + vy / 2 - uy;

		  /* snprintf(buf, 128, "a: %f in:(%.2f %.2f) " */
		  /* 		   "cur:(%.2f %.2f), next:(%.2f %.2f)\n", */
		  /* 		   a, centerx, centery, cx, cy, nx,ny); */
		  /* mylog(buf); */

		  *hole++ = centerx;
		  *hole++ = centery;
		}

	  //if (!closed){
	  *seg++ = point + (num_points - 1);
	  *seg++ = point;
	  //}
	  
	  for (len = point + num_points - 1; point < len; point++)
		{
		  *seg++ = point;
		  *seg++ = point + 1;
		}
	}
#ifdef TESTING
  for (i = 0; i < in.numberofsegments; i++)
  	{
  	  snprintf(buf, 128, "segment: %d, %d\n",
			   in.segmentlist[i*2], in.segmentlist[i*2+1]);
  	  mylog(buf);
  	}

  for (i = 0; i < in.numberofholes; i++)
  	{
  	  snprintf(buf, 128, "hole: %f, %f\n",
			   in.holelist[i*2], in.holelist[i*2+1]);
  	  mylog(buf);
  	}
#endif

  memset(&out, 0, sizeof(TriangleIO));
  out.trianglelist = (INDICE*) indices;

  triangulate("pzPNBQ", &in, &out, (TriangleIO *) NULL);

  //if (offset || stride)
  //{
  //  if (stride <= 0)
  //	stride = 1;

#ifdef TESTING
  snprintf(buf, 128, "triangles: %d\n", out.numberoftriangles);
  mylog(buf);

  for (i = 0; i < out.numberoftriangles; i++)
	{
	  snprintf(buf, 128, "> %d, %d, %d\n",out.trianglelist[i*3],
			   out.trianglelist[i*3+1],
			   out.trianglelist[i*3+2]);
	  mylog(buf);
	}
#endif

  // ----------- fix offset to vertex buffer indices -------------

  // scale to stride and add offset
  short stride = 2;
  int n, m;

  if (offset < 0)
	offset = 0;

  INDICE *tri = out.trianglelist;
  n = out.numberoftriangles * 3;

  while (n-- > 0)
	*tri++ = *tri * stride + offset;

  // correct offsetting is tricky (but this is probably not a
  // general case):
  // when a ring has an odd number of points one (or rather two)
  // additional vertices will be added. so the following rings
  // needs extra offset...

  int start = offset;

  for (j = 0, m = in.numberofholes; j < m; j++)
	{
	  start += rings[j] * stride;
	  if (rings[j] % 2 == 0)
		continue;

	  tri = out.trianglelist;
	  n = out.numberoftriangles * 3;

	  for (;n-- > 0; tri++)
		if (*tri >= start)
		  *tri += stride;

	  start += stride;
	}

  free(in.segmentlist);
  free(in.holelist);
  free(rings);

  return out.numberoftriangles;
}
