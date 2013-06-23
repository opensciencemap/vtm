#include "triangle_private.h"

/*****************************************************************************/
/*                                                                           */
/*  quality_statistics()   Print statistics about the quality of the mesh.   */
/*                                                                           */
/*****************************************************************************/

void quality_statistics(struct mesh *m, struct behavior *b) {
   struct otri triangleloop;
   vertex p[3];
   REAL cossquaretable[8];
   REAL ratiotable[16];
   REAL dx[3], dy[3];
   REAL edgelength[3];
   REAL dotproduct;
   REAL cossquare;
   REAL triarea;
   REAL shortest, longest;
   REAL trilongest2;
   REAL smallestarea, biggestarea;
   REAL triminaltitude2;
   REAL minaltitude;
   REAL triaspect2;
   REAL worstaspect;
   REAL smallestangle, biggestangle;
   REAL radconst, degconst;
   int angletable[18];
   int aspecttable[16];
   int aspectindex;
   int tendegree;
   int acutebiggest;
   int i, ii, j, k;

   printf("Mesh quality statistics:\n\n");
   radconst = PI / 18.0;
   degconst = 180.0 / PI;
   for (i = 0; i < 8; i++) {
      cossquaretable[i] = cos(radconst * (REAL) (i + 1));
      cossquaretable[i] = cossquaretable[i] * cossquaretable[i];
   }
   for (i = 0; i < 18; i++) {
      angletable[i] = 0;
   }

   ratiotable[0] = 1.5;
   ratiotable[1] = 2.0;
   ratiotable[2] = 2.5;
   ratiotable[3] = 3.0;
   ratiotable[4] = 4.0;
   ratiotable[5] = 6.0;
   ratiotable[6] = 10.0;
   ratiotable[7] = 15.0;
   ratiotable[8] = 25.0;
   ratiotable[9] = 50.0;
   ratiotable[10] = 100.0;
   ratiotable[11] = 300.0;
   ratiotable[12] = 1000.0;
   ratiotable[13] = 10000.0;
   ratiotable[14] = 100000.0;
   ratiotable[15] = 0.0;
   for (i = 0; i < 16; i++) {
      aspecttable[i] = 0;
   }

   worstaspect = 0.0;
   minaltitude = m->xmax - m->xmin + m->ymax - m->ymin;
   minaltitude = minaltitude * minaltitude;
   shortest = minaltitude;
   longest = 0.0;
   smallestarea = minaltitude;
   biggestarea = 0.0;
   worstaspect = 0.0;
   smallestangle = 0.0;
   biggestangle = 2.0;
   acutebiggest = 1;

   traversalinit(&m->triangles);
   triangleloop.tri = triangletraverse(m);
   triangleloop.orient = 0;
   while (triangleloop.tri != (triangle *) NULL) {
      org(triangleloop, p[0]);
      dest(triangleloop, p[1]);
      apex(triangleloop, p[2]);
      trilongest2 = 0.0;

      for (i = 0; i < 3; i++) {
         j = plus1mod3[i];
         k = minus1mod3[i];
         dx[i] = p[j][0] - p[k][0];
         dy[i] = p[j][1] - p[k][1];
         edgelength[i] = dx[i] * dx[i] + dy[i] * dy[i];
         if (edgelength[i] > trilongest2) {
            trilongest2 = edgelength[i];
         }
         if (edgelength[i] > longest) {
            longest = edgelength[i];
         }
         if (edgelength[i] < shortest) {
            shortest = edgelength[i];
         }
      }

      triarea = counterclockwise(m, b, p[0], p[1], p[2]);
      if (triarea < smallestarea) {
         smallestarea = triarea;
      }
      if (triarea > biggestarea) {
         biggestarea = triarea;
      }
      triminaltitude2 = triarea * triarea / trilongest2;
      if (triminaltitude2 < minaltitude) {
         minaltitude = triminaltitude2;
      }
      triaspect2 = trilongest2 / triminaltitude2;
      if (triaspect2 > worstaspect) {
         worstaspect = triaspect2;
      }
      aspectindex = 0;
      while ((triaspect2 > ratiotable[aspectindex] * ratiotable[aspectindex]) && (aspectindex < 15)) {
         aspectindex++;
      }
      aspecttable[aspectindex]++;

      for (i = 0; i < 3; i++) {
         j = plus1mod3[i];
         k = minus1mod3[i];
         dotproduct = dx[j] * dx[k] + dy[j] * dy[k];
         cossquare = dotproduct * dotproduct / (edgelength[j] * edgelength[k]);
         tendegree = 8;
         for (ii = 7; ii >= 0; ii--) {
            if (cossquare > cossquaretable[ii]) {
               tendegree = ii;
            }
         }
         if (dotproduct <= 0.0) {
            angletable[tendegree]++;
            if (cossquare > smallestangle) {
               smallestangle = cossquare;
            }
            if (acutebiggest && (cossquare < biggestangle)) {
               biggestangle = cossquare;
            }
         }
         else {
            angletable[17 - tendegree]++;
            if (acutebiggest || (cossquare > biggestangle)) {
               biggestangle = cossquare;
               acutebiggest = 0;
            }
         }
      }
      triangleloop.tri = triangletraverse(m);
   }

   shortest = sqrt(shortest);
   longest = sqrt(longest);
   minaltitude = sqrt(minaltitude);
   worstaspect = sqrt(worstaspect);
   smallestarea *= 0.5;
   biggestarea *= 0.5;
   if (smallestangle >= 1.0) {
      smallestangle = 0.0;
   }
   else {
      smallestangle = degconst * acos(sqrt(smallestangle));
   }
   if (biggestangle >= 1.0) {
      biggestangle = 180.0;
   }
   else {
      if (acutebiggest) {
         biggestangle = degconst * acos(sqrt(biggestangle));
      }
      else {
         biggestangle = 180.0 - degconst * acos(sqrt(biggestangle));
      }
   }

   printf("  Smallest area: %16.5g   |  Largest area: %16.5g\n", smallestarea, biggestarea);
   printf("  Shortest edge: %16.5g   |  Longest edge: %16.5g\n", shortest, longest);
   printf(
         "  Shortest altitude: %12.5g   |  Largest aspect ratio: %8.5g\n\n", minaltitude, worstaspect);

   printf("  Triangle aspect ratio histogram:\n");
   printf(
         "  1.1547 - %-6.6g    :  %8d    | %6.6g - %-6.6g     :  %8d\n", ratiotable[0], aspecttable[0], ratiotable[7], ratiotable[8], aspecttable[8]);
   for (i = 1; i < 7; i++) {
      printf(
            "  %6.6g - %-6.6g    :  %8d    | %6.6g - %-6.6g     :  %8d\n", ratiotable[i - 1], ratiotable[i], aspecttable[i], ratiotable[i + 7], ratiotable[i + 8], aspecttable[i + 8]);
   }
   printf(
         "  %6.6g - %-6.6g    :  %8d    | %6.6g -            :  %8d\n", ratiotable[6], ratiotable[7], aspecttable[7], ratiotable[14], aspecttable[15]);
   printf("  (Aspect ratio is longest edge divided by shortest altitude)\n\n");

   printf("  Smallest angle: %15.5g   |  Largest angle: %15.5g\n\n", smallestangle, biggestangle);

   printf("  Angle histogram:\n");
   for (i = 0; i < 9; i++) {
      printf(
            "    %3d - %3d degrees:  %8d    |    %3d - %3d degrees:  %8d\n", i * 10, i * 10 + 10, angletable[i], i * 10 + 90, i * 10 + 100, angletable[i + 9]);
   }
   printf("\n");
}

/*****************************************************************************/
/*                                                                           */
/*  statistics()   Print all sorts of cool facts.                            */
/*                                                                           */
/*****************************************************************************/

void statistics(struct mesh *m, struct behavior *b) {
   printf("\nStatistics:\n\n");
   printf("  Input vertices: %d\n", m->invertices);
   if (b->refine) {
      printf("  Input triangles: %d\n", m->inelements);
   }
   if (b->poly) {
      printf("  Input segments: %d\n", m->insegments);
      if (!b->refine) {
         printf("  Input holes: %d\n", m->holes);
      }
   }

   printf("\n  Mesh vertices: %ld\n", m->vertices.items - m->undeads);
   printf("  Mesh triangles: %ld\n", m->triangles.items);
   printf("  Mesh edges: %ld\n", m->edges);
   printf("  Mesh exterior boundary edges: %ld\n", m->hullsize);
   if (b->poly || b->refine) {
      printf("  Mesh interior boundary edges: %ld\n", m->subsegs.items - m->hullsize);
      printf("  Mesh subsegments (constrained edges): %ld\n", m->subsegs.items);
   }
   printf("\n");

   if (b->verbose) {
      quality_statistics(m, b);
      printf("Memory allocation statistics:\n\n");
      printf("  Maximum number of vertices: %ld\n", m->vertices.maxitems);
      printf("  Maximum number of triangles: %ld\n", m->triangles.maxitems);
      if (m->subsegs.maxitems > 0) {
         printf("  Maximum number of subsegments: %ld\n", m->subsegs.maxitems);
      }
      if (m->viri.maxitems > 0) {
         printf("  Maximum number of viri: %ld\n", m->viri.maxitems);
      }
      if (m->badsubsegs.maxitems > 0) {
         printf("  Maximum number of encroached subsegments: %ld\n", m->badsubsegs.maxitems);
      }
      if (m->badtriangles.maxitems > 0) {
         printf("  Maximum number of bad triangles: %ld\n", m->badtriangles.maxitems);
      }
      if (m->flipstackers.maxitems > 0) {
         printf("  Maximum number of stacked triangle flips: %ld\n", m->flipstackers.maxitems);
      }
      if (m->splaynodes.maxitems > 0) {
         printf("  Maximum number of splay tree nodes: %ld\n", m->splaynodes.maxitems);
      }
      printf(
            "  Approximate heap memory use (bytes): %ld\n\n", m->vertices.maxitems * m->vertices.itembytes + m->triangles.maxitems * m->triangles.itembytes + m->subsegs.maxitems * m->subsegs.itembytes + m->viri.maxitems * m->viri.itembytes + m->badsubsegs.maxitems * m->badsubsegs.itembytes + m->badtriangles.maxitems * m->badtriangles.itembytes + m->flipstackers.maxitems * m->flipstackers.itembytes + m->splaynodes.maxitems * m->splaynodes.itembytes);

      printf("Algorithmic statistics:\n\n");
      if (!b->weighted) {
         printf("  Number of incircle tests: %ld\n", m->incirclecount);
      }
      else {
         printf("  Number of 3D orientation tests: %ld\n", m->orient3dcount);
      }
      printf("  Number of 2D orientation tests: %ld\n", m->counterclockcount);
      if (m->hyperbolacount > 0) {
         printf("  Number of right-of-hyperbola tests: %ld\n", m->hyperbolacount);
      }
      if (m->circletopcount > 0) {
         printf("  Number of circle top computations: %ld\n", m->circletopcount);
      }
      if (m->circumcentercount > 0) {
         printf("  Number of triangle circumcenter computations: %ld\n", m->circumcentercount);
      }
      printf("\n");
   }
}

/********* Debugging routines begin here                             *********/
/**                                                                         **/
/**                                                                         **/

/*****************************************************************************/
/*                                                                           */
/*  printtriangle()   Print out the details of an oriented triangle.         */
/*                                                                           */
/*  I originally wrote this procedure to simplify debugging; it can be       */
/*  called directly from the debugger, and presents information about an     */
/*  oriented triangle in digestible form.  It's also used when the           */
/*  highest level of verbosity (`-VVV') is specified.                        */
/*                                                                           */
/*****************************************************************************/

void printtriangle(struct mesh *m, struct behavior *b, struct otri *t) {
   struct otri printtri;
   struct osub printsh;
   vertex printvertex;

   printf("triangle x%lx with orientation %d:\n", (unsigned long) t->tri, t->orient);
   decode(t->tri[0], printtri);
   if (printtri.tri == m->dummytri) {
      printf("    [0] = Outer space\n");
   }
   else {
      printf("    [0] = x%lx  %d\n", (unsigned long) printtri.tri, printtri.orient);
   }
   decode(t->tri[1], printtri);
   if (printtri.tri == m->dummytri) {
      printf("    [1] = Outer space\n");
   }
   else {
      printf("    [1] = x%lx  %d\n", (unsigned long) printtri.tri, printtri.orient);
   }
   decode(t->tri[2], printtri);
   if (printtri.tri == m->dummytri) {
      printf("    [2] = Outer space\n");
   }
   else {
      printf("    [2] = x%lx  %d\n", (unsigned long) printtri.tri, printtri.orient);
   }

   org(*t, printvertex);
   if (printvertex == (vertex) NULL)
      printf("    Origin[%d] = NULL\n", (t->orient + 1) % 3 + 3);
   else
      printf(
            "    Origin[%d] = x%lx  (%.12g, %.12g)\n", (t->orient + 1) % 3 + 3, (unsigned long) printvertex, printvertex[0], printvertex[1]);
   dest(*t, printvertex);
   if (printvertex == (vertex) NULL)
      printf("    Dest  [%d] = NULL\n", (t->orient + 2) % 3 + 3);
   else
      printf(
            "    Dest  [%d] = x%lx  (%.12g, %.12g)\n", (t->orient + 2) % 3 + 3, (unsigned long) printvertex, printvertex[0], printvertex[1]);
   apex(*t, printvertex);
   if (printvertex == (vertex) NULL)
      printf("    Apex  [%d] = NULL\n", t->orient + 3);
   else
      printf(
            "    Apex  [%d] = x%lx  (%.12g, %.12g)\n", t->orient + 3, (unsigned long) printvertex, printvertex[0], printvertex[1]);

   if (b->usesegments) {
      sdecode(t->tri[6], printsh);
      if (printsh.ss != m->dummysub) {
         printf("    [6] = x%lx  %d\n", (unsigned long) printsh.ss, printsh.ssorient);
      }
      sdecode(t->tri[7], printsh);
      if (printsh.ss != m->dummysub) {
         printf("    [7] = x%lx  %d\n", (unsigned long) printsh.ss, printsh.ssorient);
      }
      sdecode(t->tri[8], printsh);
      if (printsh.ss != m->dummysub) {
         printf("    [8] = x%lx  %d\n", (unsigned long) printsh.ss, printsh.ssorient);
      }
   }

   if (b->vararea) {
      printf("    Area constraint:  %.4g\n", areabound(*t));
   }
}

/*****************************************************************************/
/*                                                                           */
/*  printsubseg()   Print out the details of an oriented subsegment.         */
/*                                                                           */
/*  I originally wrote this procedure to simplify debugging; it can be       */
/*  called directly from the debugger, and presents information about an     */
/*  oriented subsegment in digestible form.  It's also used when the highest */
/*  level of verbosity (`-VVV') is specified.                                */
/*                                                                           */
/*****************************************************************************/

void printsubseg(struct mesh *m, struct behavior *b, struct osub *s) {
   struct osub printsh;
   struct otri printtri;
   vertex printvertex;

   printf(
         "subsegment x%lx with orientation %d and mark %d:\n", (unsigned long) s->ss, s->ssorient, mark(*s));
   sdecode(s->ss[0], printsh);
   if (printsh.ss == m->dummysub) {
      printf("    [0] = No subsegment\n");
   }
   else {
      printf("    [0] = x%lx  %d\n", (unsigned long) printsh.ss, printsh.ssorient);
   }
   sdecode(s->ss[1], printsh);
   if (printsh.ss == m->dummysub) {
      printf("    [1] = No subsegment\n");
   }
   else {
      printf("    [1] = x%lx  %d\n", (unsigned long) printsh.ss, printsh.ssorient);
   }

   sorg(*s, printvertex);
   if (printvertex == (vertex) NULL)
      printf("    Origin[%d] = NULL\n", 2 + s->ssorient);
   else
      printf(
            "    Origin[%d] = x%lx  (%.12g, %.12g)\n", 2 + s->ssorient, (unsigned long) printvertex, printvertex[0], printvertex[1]);
   sdest(*s, printvertex);
   if (printvertex == (vertex) NULL)
      printf("    Dest  [%d] = NULL\n", 3 - s->ssorient);
   else
      printf(
            "    Dest  [%d] = x%lx  (%.12g, %.12g)\n", 3 - s->ssorient, (unsigned long) printvertex, printvertex[0], printvertex[1]);

   decode(s->ss[6], printtri);
   if (printtri.tri == m->dummytri) {
      printf("    [6] = Outer space\n");
   }
   else {
      printf("    [6] = x%lx  %d\n", (unsigned long) printtri.tri, printtri.orient);
   }
   decode(s->ss[7], printtri);
   if (printtri.tri == m->dummytri) {
      printf("    [7] = Outer space\n");
   }
   else {
      printf("    [7] = x%lx  %d\n", (unsigned long) printtri.tri, printtri.orient);
   }

   segorg(*s, printvertex);
   if (printvertex == (vertex) NULL)
      printf("    Segment origin[%d] = NULL\n", 4 + s->ssorient);
   else
      printf(
            "    Segment origin[%d] = x%lx  (%.12g, %.12g)\n", 4 + s->ssorient, (unsigned long) printvertex, printvertex[0], printvertex[1]);
   segdest(*s, printvertex);
   if (printvertex == (vertex) NULL)
      printf("    Segment dest  [%d] = NULL\n", 5 - s->ssorient);
   else
      printf(
            "    Segment dest  [%d] = x%lx  (%.12g, %.12g)\n", 5 - s->ssorient, (unsigned long) printvertex, printvertex[0], printvertex[1]);
}

/**                                                                         **/
/**                                                                         **/
/********* Debugging routines end here                               *********/
