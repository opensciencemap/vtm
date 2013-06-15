#include <jni.h>
#include <string.h>
#include "glu.h"
#include "tess.h"
#include <stdio.h>
#include <stdlib.h>

#include <android/log.h>

/******************************************************************************/

typedef struct Triangle {
   int v[3];
   struct Triangle *prev;
} Triangle;

typedef struct Vertex {
   double pt[3];
   int index;
   struct Vertex *prev;
} Vertex;

typedef struct TessContext {
   Triangle *latest_t;
   int n_tris;

   Vertex *v_prev;
   Vertex *v_prevprev;
   Vertex *latest_v;
   GLenum current_mode;
   int odd_even_strip;

   void (*vertex_cb)(Vertex *, struct TessContext *);
} TessContext;

void skip_vertex(Vertex *v, TessContext *ctx);

/******************************************************************************/

TessContext *new_tess_context()
{
   TessContext *result = (TessContext *) malloc(sizeof(struct TessContext));
   result->latest_t = NULL;
   result->latest_v = NULL;
   result->n_tris = 0;
   result->v_prev = NULL;
   result->v_prevprev = NULL;
   result->v_prev = NULL;
   result->v_prev = NULL;
   result->vertex_cb = &skip_vertex;
   result->odd_even_strip = 0;
   return result;
}

void destroy_tess_context(TessContext *ctx)
{
   free(ctx);
}

Vertex *new_vertex(TessContext *ctx, double x, double y)
{
   Vertex *result = (Vertex *) malloc(sizeof(Vertex));
   result->prev = ctx->latest_v;
   result->pt[0] = x;
   result->pt[1] = y;
   result->pt[2] = 0;

   if (ctx->latest_v == NULL) {
      result->index = 0;
   }
   else {
      result->index = ctx->latest_v->index + 1;
   }
   return ctx->latest_v = result;
}

Triangle *new_triangle(TessContext *ctx, int v1, int v2, int v3)
{
   Triangle *result = (Triangle *) malloc(sizeof(Triangle));
   result->prev = ctx->latest_t;
   result->v[0] = v1;
   result->v[1] = v2;
   result->v[2] = v3;
   ctx->n_tris++;
   return ctx->latest_t = result;
}

/******************************************************************************/

void skip_vertex(Vertex *v, TessContext *ctx) {
}
;

void fan_vertex(Vertex *v, TessContext *ctx) {
   if (ctx->v_prevprev == NULL) {
      ctx->v_prevprev = v;
      return;
   }
   if (ctx->v_prev == NULL) {
      ctx->v_prev = v;
      return;
   }
   new_triangle(ctx, ctx->v_prevprev->index, ctx->v_prev->index, v->index);
   ctx->v_prev = v;
}

void strip_vertex(Vertex *v, TessContext *ctx)
{
   if (ctx->v_prev == NULL) {
      ctx->v_prev = v;
      return;
   }
   if (ctx->v_prevprev == NULL) {
      ctx->v_prevprev = v;
      return;
   }
   if (ctx->odd_even_strip) {
      new_triangle(ctx, ctx->v_prevprev->index, ctx->v_prev->index, v->index);
   }
   else {
      new_triangle(ctx, ctx->v_prev->index, ctx->v_prevprev->index, v->index);
   }
   ctx->odd_even_strip = !ctx->odd_even_strip;

   ctx->v_prev = ctx->v_prevprev;
   ctx->v_prevprev = v;
}

void triangle_vertex(Vertex *v, TessContext *ctx) {
   if (ctx->v_prevprev == NULL) {
      ctx->v_prevprev = v;
      return;
   }
   if (ctx->v_prev == NULL) {
      ctx->v_prev = v;
      return;
   }
   new_triangle(ctx, ctx->v_prevprev->index, ctx->v_prev->index, v->index);
   ctx->v_prev = ctx->v_prevprev = NULL;
}

void vertex(void *vertex_data, void *poly_data)
{
   Vertex *ptr = (Vertex *) vertex_data;
   TessContext *ctx = (TessContext *) poly_data;
   ctx->vertex_cb(ptr, ctx);
}

void begin(GLenum which, void *poly_data)
{
   TessContext *ctx = (TessContext *) poly_data;
   ctx->v_prev = ctx->v_prevprev = NULL;
   ctx->odd_even_strip = 0;
   switch (which) {
   case GL_TRIANGLES:
      ctx->vertex_cb = &triangle_vertex;
      break;
   case GL_TRIANGLE_STRIP:
      ctx->vertex_cb = &strip_vertex;
      break;
   case GL_TRIANGLE_FAN:
      ctx->vertex_cb = &fan_vertex;
      break;
   default:
      fprintf(stderr, "ERROR, can't handle %d\n", (int) which);
      ctx->vertex_cb = &skip_vertex;
      break;
   }
}

void combine(const GLdouble newVertex[3],
      const void *neighborVertex[4],
      const GLfloat neighborWeight[4], void **outData, void *polyData)
{
   TessContext *ctx = (TessContext *) polyData;
   Vertex *result = new_vertex(ctx, newVertex[0], newVertex[1]);
   *outData = result;
}

void write_output(TessContext *ctx, float **coordinates_out, int **tris_out, int *vc, int *tc)
{
   int n_verts = 1 + ctx->latest_v->index;
   *vc = n_verts;
   int n_tris_copy = ctx->n_tris;
   *tc = ctx->n_tris;
   *coordinates_out = malloc(n_verts * sizeof(float) * 2);
   *tris_out = (ctx->n_tris ? malloc(ctx->n_tris * sizeof(int) * 3) : NULL);

   while (ctx->latest_v) {
      (*coordinates_out)[2 * ctx->latest_v->index] = ctx->latest_v->pt[0];
      (*coordinates_out)[2 * ctx->latest_v->index + 1] = ctx->latest_v->pt[1];
      Vertex *prev = ctx->latest_v->prev;
      free(ctx->latest_v);
      ctx->latest_v = prev;
   }

   while (ctx->latest_t) {
      (*tris_out)[3 * (n_tris_copy - 1)] = ctx->latest_t->v[0];
      (*tris_out)[3 * (n_tris_copy - 1) + 1] = ctx->latest_t->v[1];
      (*tris_out)[3 * (n_tris_copy - 1) + 2] = ctx->latest_t->v[2];
      Triangle *prev = ctx->latest_t->prev;
      free(ctx->latest_t);
      ctx->latest_t = prev;
      n_tris_copy--;
   }
}

TessContext *tessellate(
      int *nverts,
      int *ntris,
      const float **contoursbegin,
      const float **contoursend)
{
   const float *contourbegin, *contourend;
   Vertex *current_vertex;
   GLUtesselator *tess;
   TessContext *ctx;

   tess = gluNewTess();
   ctx = new_tess_context();

   gluTessCallback(tess, GLU_TESS_VERTEX_DATA, (GLvoid (*)()) &vertex);
   gluTessCallback(tess, GLU_TESS_BEGIN_DATA, (GLvoid (*)()) &begin);
   gluTessCallback(tess, GLU_TESS_COMBINE_DATA, (GLvoid (*)()) &combine);

   gluTessBeginPolygon(tess, ctx);
   do {
      contourbegin = *contoursbegin++;
      contourend = *contoursbegin;
      gluTessBeginContour(tess);
      while (contourbegin != contourend) {
         current_vertex = new_vertex(ctx, contourbegin[0], contourbegin[1]);
         contourbegin += 2;
         gluTessVertex(tess, current_vertex->pt, current_vertex);
      }
      gluTessEndContour(tess);
   } while (contoursbegin != (contoursend - 1));
   gluTessEndPolygon(tess);

   //write_output(ctx, verts, tris, nverts, ntris);
   //destroy_tess_context(ctx);

   gluDeleteTess(tess);

   return ctx;
}

#define printf(...) __android_log_print(ANDROID_LOG_DEBUG, "Tesselate", __VA_ARGS__)

#define CAST_CTX(x) (TessContext *)(uintptr_t) x

void Java_org_oscim_renderer_sublayers_MeshLayer_tessFinish(JNIEnv *env, jclass c,
      jlong ptr_context) {

   TessContext *ctx = CAST_CTX(ptr_context);

   while (ctx->latest_v) {
      Vertex *prev = ctx->latest_v->prev;
      free(ctx->latest_v);
      ctx->latest_v = prev;
   }

   while (ctx->latest_t) {
      Triangle *prev = ctx->latest_t->prev;
      free(ctx->latest_t);
      ctx->latest_t = prev;
   }

   destroy_tess_context(ctx);
}

jint Java_org_oscim_renderer_sublayers_MeshLayer_tessGetCoordinates(JNIEnv *env, jclass c,
      jlong ptr_context, jshortArray obj_coords, jfloat scale) {

   TessContext *ctx = CAST_CTX(ptr_context);

   int length = (*env)->GetArrayLength(env, obj_coords);

   jshort* coords = (jshort*) (*env)->GetPrimitiveArrayCritical(env, obj_coords, 0);
   if (coords == NULL) {
      return 0;
   }

   int n_verts = 1 + ctx->latest_v->index;
   int n_tris_copy = ctx->n_tris;

   int cnt = 0;
   for (; ctx->latest_v && cnt < length; cnt += 2) {
      coords[cnt + 0] = (ctx->latest_v->pt[0] * scale) + 0.5f;
      coords[cnt + 1] = (ctx->latest_v->pt[1] * scale) + 0.5f;
      Vertex *prev = ctx->latest_v->prev;
      free(ctx->latest_v);
      ctx->latest_v = prev;
   }
   (*env)->ReleasePrimitiveArrayCritical(env, obj_coords, coords, JNI_ABORT);

   return cnt;
}

jint Java_org_oscim_renderer_sublayers_MeshLayer_tessGetIndices(JNIEnv *env, jclass c,
      jlong ptr_context, jshortArray obj_indices) {

   TessContext *ctx = CAST_CTX(ptr_context);

   int length = (*env)->GetArrayLength(env, obj_indices);

   jshort* tris = (jshort*) (*env)->GetPrimitiveArrayCritical(env, obj_indices, 0);
   if (tris == NULL) {
      return 0;
   }

   int n_tris_copy = ctx->n_tris;

   int cnt = 0;

   for (; ctx->latest_t && cnt < length; cnt += 3) {
      tris[cnt + 0] = ctx->latest_t->v[0];
      tris[cnt + 1] = ctx->latest_t->v[1];
      tris[cnt + 2] = ctx->latest_t->v[2];
      Triangle *prev = ctx->latest_t->prev;

      free(ctx->latest_t);
      ctx->latest_t = prev;
      n_tris_copy--;
   }

   ctx->n_tris = n_tris_copy;

   (*env)->ReleasePrimitiveArrayCritical(env, obj_indices, tris, JNI_ABORT);

   return cnt;
}

jlong Java_org_oscim_renderer_sublayers_MeshLayer_tessellate(JNIEnv *env, jclass c,
      jfloatArray obj_points, jint pos,
      jshortArray obj_index, jint ipos,
      jint num_rings) { //, jintArray obj_out) {

   jboolean isCopy;

   printf("add %d %d %d\n", pos, ipos, num_rings);

   float* orig_points = (float*) (*env)->GetPrimitiveArrayCritical(env, obj_points, &isCopy);
   if (orig_points == NULL)
      return 0;

   const float *points = orig_points + pos;

   jshort* orig_indices = (jshort*) (*env)->GetPrimitiveArrayCritical(env, obj_index, &isCopy);
   if (orig_indices == NULL) {
      (*env)->ReleasePrimitiveArrayCritical(env, obj_points, orig_points, JNI_ABORT);
      return 0;
   }

   jshort* indices = orig_indices + ipos;

   const float **rings = malloc(sizeof(float*) * (num_rings + 1));
   int offset = 0;
   for (int i = 0; i < num_rings; i++) {
      rings[i] = points + offset;
      offset += indices[i];
   }
   rings[num_rings] = points + offset;

   int nverts, ntris;

   TessContext *ctx = tessellate(&nverts, &ntris,
         rings, rings + (num_rings + 1));

   free(rings);

   (*env)->ReleasePrimitiveArrayCritical(env, obj_index, orig_indices, JNI_ABORT);
   (*env)->ReleasePrimitiveArrayCritical(env, obj_points, orig_points, JNI_ABORT);

   return (long) ctx;
}
