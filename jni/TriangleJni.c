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

// from www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
#if 0
int pnpoly(int nvert, float *vert, float testx, float testy)
{
  int i, j, c = 0;
  for (i = 0, j = (nvert-1)*2; i < nvert * 2; j = i++) {
    if ( ((vert[i*2+1] > testy) != (vert[j*j+1] > testy)) &&
	 (testx < (vert[j*2]-vert[i*2])
	  * (testy - vert[i*2+1])
	  / (vert[j*2+1]-vert[i*2+1]) + vert[i*2]) )
       c = !c;
  }
  return c;
}
#endif

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

  int invalid = 0;

  float *i_points = points;

  for (i = 0; i < num_points - 1 && !invalid; i++)
	{
	  float x = *i_points++;
	  float y = *i_points++;
	  float *j_points = i_points;

	  for (j = i + 1; j < num_points; j++, j_points += 2)
		{
		  if ((*j_points == x) && (*(j_points+1) == y))
			{
			  invalid = 1;
			  break;
			}
		}
	}

  if (invalid)
	{
	  snprintf(buf, 128, "\ninavlid polygon: duplicate points at %d, %d:\n", i-1, j);
	  mylog(buf);

	  for (i = 0; i < num_points; i++) {
		snprintf(buf, 128, "%d point: %f, %f\n", i, points[i*2], points[i*2+1]);
		mylog(buf);
	  }
	  snprintf(buf, 128, "points: %d, rings: %d\n\n", num_points, num_rings);
	  mylog(buf);
	  return 0;
	}

#ifdef TESTING
  for (i = 0; i < num_points; i++) {
  	snprintf(buf, 128, "point: %f, %f\n", points[i*2], points[i*2+1]);
  	mylog(buf);
  }
  snprintf(buf, 128, "points: %d, rings: %d\n", num_points, num_rings);
  mylog(buf);
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

		  float nx = in.pointlist[k++];
		  float ny = in.pointlist[k++];

		  float cx, cy, vx, vy;

		  // try to find a large enough segment
		  for (len = (point + num_points) * 2; k < len;)
			{
			  cx = nx;
			  cy = ny;

			  nx = in.pointlist[k++];
			  ny = in.pointlist[k++];

			  vx = nx - cx;
			  vy = ny - cy;

			  if (vx > 4 || vx < -4 || vy > 4 || vy < -4)
				break;
			}

		  float a = sqrt(vx*vx + vy*vy);

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

  // p - use polygon input, for CDT
  // z - zero offset array offsets...
  // P - no poly output
  // N - no node output
  // B - no bound output
  // Q - be quiet!
  triangulate("pzPNBQ", &in, &out, (TriangleIO *) NULL);

  if (in.numberofpoints < out.numberofpoints)
	{
	  snprintf(buf, 128, "polygon input is bad! points in:%d out%d\n",
				in.numberofpoints, out.numberofpoints);
	  mylog(buf);

	  free(in.segmentlist);
	  free(in.holelist);
	  free(rings);
	  return 0;
	}


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

  // when a ring has an odd number of points one (or rather two)
  // additional vertices will be added. so the following rings
  // needs extra offset...

  int start = offset;

  for (j = 0, m = in.numberofholes; j < m; j++)
	{
	  start += rings[j] * stride;
	  // even number of points?
	  if (!(rings[j] & 1))
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
