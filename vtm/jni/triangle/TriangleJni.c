#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "triangle.h"

#ifdef __ANDROID__
#include <android/log.h>
#define printf(...) __android_log_print(ANDROID_LOG_DEBUG, "Triangle", __VA_ARGS__)
#endif

// from www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
#if 0
int pnpoly(int nvert, float *vert, float testx, float testy)
{
   int i, j, c = 0;
   for (i = 0, j = (nvert-1)*2; i < nvert * 2; j = i++)
   {
      if ( ((vert[i*2+1] > testy) != (vert[j*j+1] > testy)) &&
            (testx < (vert[j*2]-vert[i*2])
                  * (testy - vert[i*2+1])
                  / (vert[j*2+1]-vert[i*2+1]) + vert[i*2]) )
      c = !c;
   }
   return c;
}

int compare_dups(const void *a, const void *b) {
   int da = *((const long*) a);
   int db = *((const long*) b);
   return (da > db) - (da < db);
}

void shiftSegment(TriangleIO *in, int *seg, int pos) {
   int size = (in->numberofsegments - pos - 1) * sizeof(int) * 2;
   printf("shift %d - %d %d\n", size, in->numberofsegments, pos);
   if (size > 0)
      memmove(seg, seg + 2, size);

   in->numberofsegments -= 1;
}
struct {
   int p1;
   int p2;
} segment;

#endif

static void printPoly(TriangleIO *in) {
   // print poly format to check with triangle/showme
   printf("%d 2 0 0\n", in->numberofpoints);
   for (int j = 0; j < in->numberofpoints; j++)
      printf("%d %f %f\n", j, in->pointlist[j*2], in->pointlist[j*2+1]);

   int *seg = in->segmentlist;
   printf("%d 0\n", in->numberofsegments);
   for (int j = 0; j < in->numberofsegments; j++, seg += 2)
      printf("%d %d %d\n", j, *seg, *(seg+1));

   printf("%d 0\n", in->numberofholes);
   for (int j = 0; j < in->numberofholes; j++) {
      printf("%d %f %f\n", j, in->holelist[j*2], in->holelist[j*2+1]);
   }
}

jint Java_org_oscim_utils_geom_Triangulator_triangulate(JNIEnv *env, jclass c,
      jfloatArray obj_points, jint pos, jint len, jint num_rings, jobject indice_buf, jint offset) {

   jshort* indices = (jshort*) (*env)->GetDirectBufferAddress(env, indice_buf);
   jboolean isCopy;

   float* orig_points = (float*) (*env)->GetPrimitiveArrayCritical(env, obj_points, &isCopy);
   if (orig_points == NULL)
      return 0;

   float *points = orig_points + pos;

   TriangleIO in, out;

   memset(&in, 0, sizeof(TriangleIO));

   in.numberofpoints = len >> 1;
   in.pointlist = (float *) points;

   // check if explicitly closed
   if (in.pointlist[0] == in.pointlist[indices[0] - 2]
         && in.pointlist[1] == in.pointlist[indices[0] - 1]) {
      int point = 0;
      for (int i = 0; i < num_rings; i++) {
         // remove last point in ring
         indices[i] -= 2;
         int last = point + (indices[i] >> 1);

         if (in.numberofpoints - last > 1)
            memmove(in.pointlist + (last * 2), in.pointlist + ((last + 1) * 2),
                  (in.numberofpoints - last - 1) * 2 * sizeof(float));

         in.numberofpoints--;
         point = last;
      }
   }

   int dups = 0;

   float *i_points = points;
   int *skip_list = NULL;

   // check for duplicate vertices and keep a list
   // of dups and the first occurence
   for (int i = 0; i < in.numberofpoints - 1; i++) {
      float x = *i_points++;
      float y = *i_points++;
      float *j_points = i_points;

      for (int j = i + 1; j < in.numberofpoints; j++, j_points += 2) {
         if ((*j_points == x) && (*(j_points + 1) == y)) {
            skip_list = realloc(skip_list, (dups + 2) * 2 * sizeof(int));
            skip_list[dups * 2 + 0] = j;
            skip_list[dups * 2 + 1] = i;
            dups++;
         }
      }
   }

   in.segmentlist = (int *) malloc(in.numberofpoints * 2 * sizeof(int));
   in.numberofsegments = in.numberofpoints;
   in.numberofholes = num_rings - 1;

   int *rings = NULL;
   if (in.numberofholes > 0) {
      in.holelist = (float *) malloc(in.numberofholes * 2 * sizeof(float));
      rings = (int*) malloc(num_rings * sizeof(int));
   }

   int *seg = in.segmentlist;
   float *hole = in.holelist;

   // counter going through all points
   int point;
   // counter going through all rings
   int ring;

   // assign all points to segments for each ring
   for (ring = 0, point = 0; ring < num_rings; ring++, point++) {
      int len;
      int num_points = indices[ring] >> 1;

      if (rings)
         rings[ring] = num_points;

      // add holes: we need a point inside the hole...
      // this is just a heuristic, assuming that two
      // 'parallel' lines have a distance of at least
      // 1 unit. you'll notice when things went wrong
      // when the hole is rendered instead of the poly
      if (ring > 0) {
         int k = point * 2;

         float nx = in.pointlist[k++];
         float ny = in.pointlist[k++];

         float cx = 0, cy = 0, vx = 0, vy = 0;

         // try to find a large enough segment
         for (len = (point + num_points) * 2; k < len;) {
            cx = nx;
            cy = ny;

            nx = in.pointlist[k++];
            ny = in.pointlist[k++];

            vx = nx - cx;
            vy = ny - cy;

            if (vx > 4 || vx < -4 || vy > 4 || vy < -4)
               break;
         }

         float a = sqrt(vx * vx + vy * vy);

         float ux = -vy / a;
         float uy = vx / a;

         float centerx = cx + vx / 2.0 - (ux * 0.1);
         float centery = cy + vy / 2.0 - (uy * 0.1);

         *hole++ = centerx;
         *hole++ = centery;
      }

      // close ring
      int last = point + (num_points - 1);
      *seg++ = last;
      *seg++ = point;

      for (len = point + num_points - 1; point < len; point++) {
         *seg++ = point;
         *seg++ = point + 1;
      }
   }

   if (dups) {
      for (int i = 0; i < dups; i++) {
         printf("duplicate points at %d, %d: %f,%f\n",
               skip_list[i*2], skip_list[i*2+1],
               in.pointlist[skip_list[i*2+1]*2],
               in.pointlist[skip_list[i*2+1]*2+1]);
      }
      printPoly(&in);

      // replace duplicate positions with first occurence
      for (int i = 0; i < dups; i++) {
         // position of the duplicate vertex
         int pos = skip_list[i * 2] - i;
         // first vertex
         int replacement = skip_list[i * 2 + 1];

         seg = in.segmentlist;
         for (int j = 0; j < in.numberofsegments * 2; j++, seg++) {
            if (*seg == pos) {
               printf("%d: %d <- %d", j, pos, replacement);
               *seg = replacement;
            }
         }
      }
   }

   memset(&out, 0, sizeof(TriangleIO));
   out.trianglelist = (INDICE*) indices;

   // p - use polygon input, for CDT
   // z - zero offset array offsets...
   // P - no poly output
   // N - no node output
   // B - no bound output
   // Q - be quiet!

   TriangleOptions opt;
   memset(&opt, 0, sizeof(TriangleOptions));

   opt.dwyer = 1;
   opt.steiner = -1;
   opt.order = 1;
   opt.maxarea = -1.0;

   opt.poly = 1;
   opt.usesegments = 1;
   opt.nopolywritten = 1;
   opt.nonodewritten = 1;
   opt.nobound = 1;
   opt.quiet = 1;

   triangulate(&opt, &in, &out, (TriangleIO *) NULL);

   if (in.numberofpoints < out.numberofpoints) {
      // TODO rerun with 'nonodewritten = 0'
      printf( "polygon input is bad! points in:%d out%d\n", in.numberofpoints, out.numberofpoints);
      out.numberoftriangles = 0;
   }
   else if (out.trianglelist)
   {
      // scale to stride and add offset
      short stride = 2;

      if (offset < 0)
         offset = 0;

      INDICE *tri = out.trianglelist;

      for (int n = out.numberoftriangles * 3; n > 0; n--, tri++)
         *tri = *tri * stride + offset;

      // when a ring has an odd number of points one (or rather two)
      // additional vertices will be added. so the following rings
      // needs extra offset...
      int start = offset;
      for (int j = 0, m = in.numberofholes; j < m; j++) {
         start += rings[j] * stride;

         // even number of points?
         if (!(rings[j] & 1))
            continue;

         tri = out.trianglelist;
         int n = out.numberoftriangles * 3;

         for (; n-- > 0; tri++)
            if (*tri >= start)
               *tri += stride;

         start += stride;
      }
   }
   else
   {
      printf( "triangle failed %d\n", out.numberofpoints);
   }

   (*env)->ReleasePrimitiveArrayCritical(env, obj_points, orig_points, JNI_ABORT);

   free(in.segmentlist);
   free(in.holelist);
   free(rings);
   free(skip_list);

   return out.numberoftriangles;
}
