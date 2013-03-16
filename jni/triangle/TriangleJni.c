#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "triangle.h"

#include <android/log.h>
#define printf(...) __android_log_print(ANDROID_LOG_DEBUG, "Triangle", __VA_ARGS__)

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
#endif

//#define TESTING

int compare_dups(const void *a, const void *b)
{
	int da = *((const int*) a);
	int db = *((const int*) b);

	return (da > db) - (da < db);
}

typedef struct triangulateio TriangleIO;

jint Java_org_oscim_renderer_layer_ExtrusionLayer_triangulate(JNIEnv *env,
		jclass c, jfloatArray obj_points, jint pos, jint len, jint num_rings,
		jobject indice_buf, jint offset)
{

	jshort* indices = (jshort*) (*env)->GetDirectBufferAddress(env, indice_buf);
	jboolean isCopy;

	float* orig_points = (float*) (*env)->GetPrimitiveArrayCritical(env,
			obj_points, &isCopy);
	if (orig_points == NULL)
		return 0;

	if (isCopy)
		printf("Poor bugger: VM copied array");

	float *points = orig_points + pos;

	TriangleIO in, out;
	char buf[128];

	memset(&in, 0, sizeof(TriangleIO));

	in.numberofpoints = len >> 1;
	in.pointlist = (float *) points;

	// check if explicitly closed
	if (in.pointlist[0] == in.pointlist[indices[0] - 2]
			&& in.pointlist[1] == in.pointlist[indices[0] - 1])
	{
		int point = 0;
		for (int i = 0; i < num_rings; i++)
		{
			// remove last point in ring
			indices[i] -= 2;
			int last = point + (indices[i] >> 1);

			if (in.numberofpoints - last > 1)
				memmove(in.pointlist + (last * 2),
						in.pointlist + ((last + 1) * 2),
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
	for (int i = 0; i < in.numberofpoints - 1; i++)
	{
		float x = *i_points++;
		float y = *i_points++;
		float *j_points = i_points;

		for (int j = i + 1; j < in.numberofpoints; j++, j_points += 2)
		{
			if ((*j_points == x) && (*(j_points + 1) == y))
			{
				skip_list = realloc(skip_list, (dups + 2) * 2 * sizeof(int));
				skip_list[dups * 2 + 0] = j;
				skip_list[dups * 2 + 1] = i;
				dups++;
			}
		}
	}

#ifdef TESTING
	for (i = 0; i < in.numberofpoints; i++)
	{
		printf("point: %f, %f\n", points[i*2], points[i*2+1]);
	}
	printf("points: %d, rings: %d\n", in.numberofpoints, num_rings);
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

	int *seg = in.segmentlist;
	float *hole = in.holelist;

	// counter going through all points
	int point;
	// counter going through all rings
	int ring;

	// assign all points to segments for each ring
	for (ring = 0, point = 0; ring < num_rings; ring++, point++)
	{
		int len;
		int num_points = indices[ring] >> 1;

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

			float a = sqrt(vx * vx + vy * vy);

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
		for (int i = 0; i < dups; i++)
		{
			printf( "duplicate points at %d, %d: %f,%f\n",
					skip_list[i*2], skip_list[i*2+1],
					in.pointlist[skip_list[i*2+1]*2],
					in.pointlist[skip_list[i*2+1]*2+1]);
		}
		// print poly format to check with triangle/showme
		for (int j = 0; j < in.numberofpoints; j++)
			printf("%d %f %f\n", j, in.pointlist[j*2], in.pointlist[j*2+1]);

		seg = in.segmentlist;
		for (int j = 0; j < in.numberofsegments; j++, seg += 2)
			printf("%d %d %d\n", j, *seg, *(seg+1));

		for (int j = 0; j < in.numberofholes; j++)
		{
			printf("%d %f %f\n", j, in.holelist[j*2], in.holelist[j*2+1]);
		}

		if (0)
		{
			free(in.segmentlist);
			free(in.holelist);
			free(rings);
			free(skip_list);
			return 0;
		}
		if (dups == 2)
		{
			if (skip_list[0] > skip_list[2])
			{
				int tmp = skip_list[0];
				skip_list[0] = skip_list[2];
				skip_list[2] = tmp;

				tmp = skip_list[1];
				skip_list[1] = skip_list[3];
				skip_list[3] = tmp;

				printf("flip items\n");
			}
		}
		else if (dups > 2)
		{
			printf("sort dups\n");

			qsort(skip_list, dups, 2 * sizeof(float), compare_dups);
		}

		// shift segment indices while removing duplicate points
		for (int i = 0; i < dups; i++)
		{
			// position of the duplicate vertex
			int pos = skip_list[i * 2] - i;
			// first vertex
			int replacement = skip_list[i * 2 + 1];

			printf("add offset: %d, from pos %d\n", i, pos);
			seg = in.segmentlist;
			for (int j = 0; j < in.numberofsegments * 2; j++, seg++)
			{
				if (*seg == pos)
				{
					if (replacement >= pos)
						*seg = replacement - i;
					else
						*seg = replacement;
				}
				else if (*seg > pos)
					*seg -= 1;
			}

			printf(
					"move %d %d %d\n", pos, pos + 1, in.numberofpoints - pos - 1);

			if (in.numberofpoints - pos > 1)
				memmove(in.pointlist + (pos * 2),
						in.pointlist + ((pos + 1) * 2),
						(in.numberofpoints - pos - 1) * 2 * sizeof(float));

			in.numberofpoints--;
		}
	}

#ifdef TESTING
	for (i = 0; i < in.numberofsegments; i++)
	{
		printf("segment: %d, %d\n",
				in.segmentlist[i*2], in.segmentlist[i*2+1]);
	}

	for (i = 0; i < in.numberofholes; i++)
	{
		printf("hole: %f, %f\n",
				in.holelist[i*2], in.holelist[i*2+1]);
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
		printf( "polygon input is bad! points in:%d out%d\n",
				in.numberofpoints,
				out.numberofpoints);

		for (int j = 0; j < in.numberofpoints; j++)
		{
			printf("%d %f %f\n", j, in.pointlist[j*2], in.pointlist[j*2+1]);
		}

		seg = in.segmentlist;
		for (int j = 0; j < in.numberofsegments; j++, seg += 2)
		{
			printf("%d %d %d\n", j, *seg, *(seg+1));
		}
		(*env)->ReleasePrimitiveArrayCritical(env, obj_points, orig_points,
				JNI_ABORT);
		free(in.segmentlist);
		free(in.holelist);
		free(rings);
		return 0;
	}

#ifdef TESTING
	printf( "triangles: %d\n", out.numberoftriangles);

	for (int i = 0; i < out.numberoftriangles; i++)
	{
		printf("> %d, %d, %d\n",out.trianglelist[i*3],
				out.trianglelist[i*3+1],
				out.trianglelist[i*3+2]);
	}
#endif

	INDICE *tri;

	/* shift back indices from removed duplicates */
	for (int i = 0; i < dups; i++)
	{
		int pos = skip_list[i * 2] + i;

		tri = out.trianglelist;
		int n = out.numberoftriangles * 3;

		for (; n-- > 0; tri++)
			if (*tri >= pos)
				*tri += 1;
	}

	/* fix offset to vertex buffer indices */

	// scale to stride and add offset
	short stride = 2;

	if (offset < 0)
		offset = 0;

	tri = out.trianglelist;
	for (int n = out.numberoftriangles * 3; n > 0; n--)
		*tri++ = *tri * stride + offset;

	// when a ring has an odd number of points one (or rather two)
	// additional vertices will be added. so the following rings
	// needs extra offset...
	int start = offset;
	for (int j = 0, m = in.numberofholes; j < m; j++)
	{
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

	(*env)->ReleasePrimitiveArrayCritical(env, obj_points, orig_points,
			JNI_ABORT);

	free(in.segmentlist);
	free(in.holelist);
	free(rings);
	free(skip_list);

	return out.numberoftriangles;
}
