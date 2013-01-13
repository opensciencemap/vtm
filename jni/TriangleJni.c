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

int
compare_dups (const void *a, const void *b)
{
  int da = *((const int*)a);
  int db = *((const int*)b);

  return (da > db) - (da < db);
}


typedef struct triangulateio TriangleIO;

jint Java_org_quake_triangle_TriangleJNI_triangulate(JNIEnv *env, jclass c,
													 jobject point_buf,
													 jint num_rings,
													 jobject indice_buf,
													 jint offset)
{
  jfloat* points = (jfloat*)(*env)->GetDirectBufferAddress(env, point_buf);
  jshort* indices = (jshort*)(*env)->GetDirectBufferAddress(env, indice_buf);

  TriangleIO in, out;
  char buf[128];
  int i, j;

  memset(&in, 0, sizeof(TriangleIO));

  //int num_points = (indices[0])>>1;
  in.numberofpoints = (indices[0])>>1;
  in.pointlist = (float *) points;

  // check if explicitly closed
  if (in.pointlist[0] == in.pointlist[indices[1]-2] &&
	in.pointlist[1] == in.pointlist[indices[1]-1])
	{
	  int point = 0;
	  for (i = 0; i < num_rings; i++)
		{
		  // remove last point in ring
		  indices[i+1] -= 2;
		  int last = point + indices[i+1] >> 1;

		  //sprintf(buf, "drop closing point at %d, %d:\n", point, last);
		  //mylog(buf);

		  if (in.numberofpoints - last > 1)
			memmove(in.pointlist + (last * 2),
				in.pointlist + ((last + 1)*2),
				(in.numberofpoints - last - 1)
				* 2 * sizeof(float));

		  in.numberofpoints--;
		  point = last;
		}
	}

  int dups = 0;

  float *i_points = points;
  int *skip_list = NULL;

  // check for duplicate vertices and keep a list
  // of dups and the first occurence
  for (i = 0; i < in.numberofpoints - 1; i++)
	{
	  float x = *i_points++;
	  float y = *i_points++;
	  float *j_points = i_points;

	  for (j = i + 1; j < in.numberofpoints; j++, j_points += 2)
		{
		  if ((*j_points == x) && (*(j_points+1) == y))
			{
			  skip_list = realloc(skip_list, (dups + 2) * 2 * sizeof(int));
			  skip_list[dups*2+0] = j;
			  skip_list[dups*2+1] = i;
			  dups++;
			}
		}
	}

#ifdef TESTING
  for (i = 0; i < in.numberofpoints; i++) {
  	sprintf(buf, "point: %f, %f\n", points[i*2], points[i*2+1]);
  	mylog(buf);
  }
  sprintf(buf, "points: %d, rings: %d\n", in.numberofpoints, num_rings);
  mylog(buf);
#endif
  in.segmentlist = (int *) malloc(in.numberofpoints * 2 * sizeof(int));
  in.numberofsegments = in.numberofpoints;
  in.numberofholes = num_rings - 1;

  int *rings = NULL;
  if (in.numberofholes > 0)
	{
	  in.holelist = (float *) malloc(in.numberofholes * 2 * sizeof(float));
	  rings = (int*) malloc(num_rings * sizeof(int));
	}

  int   *seg = in.segmentlist;
  float *hole = in.holelist;

  // counter going through all points
  int point;
  // counter going through all rings
  int ring;

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

		  *hole++ = centerx;
		  *hole++ = centery;
		}

	  // close ring
	  int last = point + (num_points - 1);
	  *seg++ = last;
	  *seg++ = point;

	  for (len = point + num_points - 1; point < len; point++)
		{
		  *seg++ = point;
		  *seg++ = point + 1;
		}
	}

  if (dups)
	{
	  for (i = 0; i < dups; i++)
		{
		  sprintf(buf, "duplicate points at %d, %d: %f,%f\n",
				  skip_list[i*2], skip_list[i*2+1],
				  in.pointlist[skip_list[i*2+1]*2],
				  in.pointlist[skip_list[i*2+1]*2+1]);
		  mylog(buf);
		}

	  if (dups == 2)
		{
		  if (skip_list[0] > skip_list[2])
			{
			  int tmp = skip_list[0];
			  skip_list[0] = skip_list[2];
			  tmp = skip_list[1];
			  skip_list[1] = skip_list[3];

			  sprintf(buf, "flip items\n");
			  mylog(buf);
			}
		}
	  else if (dups > 2)
		{
		  sprintf(buf, "sort dups\n");
		  mylog(buf);

		  qsort (skip_list, dups, 2 * sizeof (float), compare_dups);
		}

	  // shift segment indices while removing duplicate points
	  for (i = 0; i < dups; i++)
		{
		  // position of the duplicate vertex
		  int pos = skip_list[i*2] - i;
		  // first vertex
		  int replacement = skip_list[i*2+1];

		  sprintf(buf, "add offset: %d, from pos %d\n", i, pos);
		  mylog(buf);

		  for (seg = in.segmentlist, j = 0; j < in.numberofsegments*2; j++, seg++)
			{
			  if (*seg == pos)
				{
				  if (replacement >= pos)
					*seg = replacement - i;
				  else
					*seg = replacement;
				}
			  else if(*seg > pos)
				*seg -= 1;
			}

		  sprintf(buf, "move %d %d %d\n", pos, pos + 1,
				  in.numberofpoints - pos - 1);
		  mylog(buf);

		  if (in.numberofpoints - pos > 1)
			memmove(in.pointlist + (pos * 2),
					in.pointlist + ((pos + 1)*2),
					(in.numberofpoints - pos - 1) * 2 * sizeof(float));

		  in.numberofpoints--;

		  // print poly format to check with triangle/showme
		  for (j = 0; j < in.numberofpoints; j++)
			{
			  sprintf(buf, "%d %f %f\n", j,
					  in.pointlist[j*2],
					  in.pointlist[j*2+1]);
			  mylog(buf);
			}

		  seg = in.segmentlist;
		  for (j = 0; j < in.numberofsegments; j++, seg+=2)
			{
			  sprintf(buf, "%d %d %d\n",
					  j, *seg, *(seg+1));
			  mylog(buf);
			}
		}
	  for (j = 0; j < in.numberofholes; j++)
		{
		  sprintf(buf, "%d %f %f\n", j,
				  in.holelist[j*2], in.holelist[j*2+1]);
		  mylog(buf);
		}
	}

#ifdef TESTING
  for (i = 0; i < in.numberofsegments; i++)
  	{
  	  sprintf(buf, "segment: %d, %d\n",
			   in.segmentlist[i*2], in.segmentlist[i*2+1]);
  	  mylog(buf);
  	}

  for (i = 0; i < in.numberofholes; i++)
  	{
  	  sprintf(buf, "hole: %f, %f\n",
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
	  sprintf(buf, "polygon input is bad! points in:%d out%d\n",
				in.numberofpoints, out.numberofpoints);
	  mylog(buf);

	  free(in.segmentlist);
	  free(in.holelist);
	  free(rings);
	  return 0;
	}


#ifdef TESTING
  sprintf(buf, "triangles: %d\n", out.numberoftriangles);
  mylog(buf);

  for (i = 0; i < out.numberoftriangles; i++)
	{
	  sprintf(buf, "> %d, %d, %d\n",out.trianglelist[i*3],
			   out.trianglelist[i*3+1],
			   out.trianglelist[i*3+2]);
	  mylog(buf);
	}
#endif

  INDICE *tri;
  int n, m;

  /* shift back indices from removed duplicates */
  for (i = 0; i < dups; i++)
	{
	  int pos = skip_list[i*2] + i;

	  tri = out.trianglelist;
	  n = out.numberoftriangles * 3;

	  for (;n-- > 0; tri++)
		if (*tri >= pos)
		  *tri += 1;
	}

  /* fix offset to vertex buffer indices */

  // scale to stride and add offset
  short stride = 2;

  if (offset < 0)
	offset = 0;

  tri = out.trianglelist;
  for (n = out.numberoftriangles * 3; n > 0; n--)
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
  free(skip_list);

  return out.numberoftriangles;
}
