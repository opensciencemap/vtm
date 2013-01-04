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
  int num_segments = num_points;
  in.segmentlist = (int *) malloc(num_segments * 2 * sizeof(int));
  in.numberofsegments = num_segments;
  in.numberofholes = num_rings - 1;
  int *rings = NULL;
  if (in.numberofholes > 0)
	{
	  in.holelist = (float *) malloc(in.numberofholes * 2 * sizeof(float));
	  rings = (int*) malloc(num_rings * sizeof(int));
	}

  int seg = 0;
  int hole = 0;

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
	  // 1 unit.
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

		  snprintf(buf, 128, "a: %f in:(%.2f %.2f) "
				   "cur:(%.2f %.2f), next:(%.2f %.2f)\n",
				   a, centerx, centery, cx, cy, nx,ny);
		  mylog(buf);

		  in.holelist[hole++] = centerx;
		  in.holelist[hole++] = centery;
		}

	  in.segmentlist[seg++] = point + (num_points - 1);
	  in.segmentlist[seg++] = point;

	  for (len = point + num_points - 1; point < len; point++)
		{
		  in.segmentlist[seg++] = point;
		  in.segmentlist[seg++] = point + 1;
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
  // scale to stride
  short stride = 2;
  int n, m;

  for (i = 0, n = out.numberoftriangles * 3; i < n; i++)
	out.trianglelist[i] *= stride;

  // correct offsetting is tricky (and probably not a general case):
  // when a ring has an odd number of points one (or rather two)
  // additional vertices will be added. so the following rings
  // needs extra offset...

  if (offset < 0)
	offset = 0;

  short off = offset;
  int add = 0;
  int start = 0;

  for (j = 0, m = in.numberofholes; j < m; j++)
	{
	  start += rings[j] * stride;
	  if (rings[j] % 2 == 0)
		continue;

#ifdef TESTING
	  snprintf(buf, 128, "add offset: %d\n", start);
	  mylog(buf);
#endif

	  for (i = 0, n = out.numberoftriangles * 3; i < n; i++)
		if (out.trianglelist[i] >= start)
		  out.trianglelist[i] += stride;

	  start += stride;

#ifdef TESTING
	  for (i = 0; i < out.numberoftriangles; i++)
		{
		  snprintf(buf, 128, "> %d, %d, %d\n",out.trianglelist[i*3],
				   out.trianglelist[i*3+1],
				   out.trianglelist[i*3+2]);
		  mylog(buf);
		}
#endif
	}

  // flip direction and add offset
  for (i = 0, n = out.numberoftriangles * 3; i < n; i += 3)
	{
	  out.trianglelist[i+0] = out.trianglelist[i+0] + offset;
	  unsigned short tmp = out.trianglelist[i+1];
	  out.trianglelist[i+1] = out.trianglelist[i+2]  + offset;
	  out.trianglelist[i+2] = tmp + offset;
	}

  free(in.segmentlist);
  free(in.holelist);
  free(rings);

  return out.numberoftriangles;
}
