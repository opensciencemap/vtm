tessellate = (function() {

    Module.TOTAL_MEMORY = 1024 * 1024;

    var c_tessellate = Module.cwrap('tessellate', 'void', [ 'number', 'number',
            'number', 'number', 'number', 'number' ]);

    // special tessellator for extrusion layer - only returns triangle indices
    var tessellate = function(vertices, v_start, v_end, boundaries, b_start,
            b_end, mode) {
        var i;

        var v_len = (v_end - v_start);
        var b_len = (b_end - b_start);

        var p = Module._malloc(v_len * 8);

        for (i = 0; i < v_len; ++i)
            Module.setValue(p + i * 8, vertices[v_start + i], 'double');

        var contours = Module._malloc((b_len + 1) * 4);

        // pointer to first contour
        Module.setValue(contours + 0, p + 0, 'i32');
        var offset = p;

        // pointer to further contours + end
        for (i = 0; i < b_len; ++i) {
            offset += 8 * boundaries[b_start + i];
            Module.setValue(contours + 4 * (i + 1), offset, 'i32');
        }

        var ppcoordinates_out = Module._malloc(4);
        var pptris_out = Module._malloc(4);
        var pnverts = Module._malloc(4);
        var pntris = Module._malloc(4);

        c_tessellate(ppcoordinates_out, pnverts, pptris_out, pntris, contours,
                contours + 4 * (b_len + 1));

        var pcoordinates_out = Module.getValue(ppcoordinates_out, 'i32');
        var ptris_out = Module.getValue(pptris_out, 'i32');

        var nverts = Module.getValue(pnverts, 'i32');
        var ntris = Module.getValue(pntris, 'i32');

        var result_triangles = null;
        var result_vertices = null;

        if (mode){
            result_triangles = new Int32Array(ntris * 3);
            for (i = 0; i < 3 * ntris; ++i)
                result_triangles[i] = Module.getValue(ptris_out + i * 4, 'i32');
            
            result_vertices = new Float32Array(nverts * 2);
            for (i = 0; i < 2 * nverts; ++i)
                result_vertices[i] = Module.getValue(pcoordinates_out + i * 8, 'double');
        
        } else {
            if (nverts * 2 == v_len) {
                result_triangles = new Int32Array(ntris * 3);
                
                for (i = 0; i < 3 * ntris; ++i) {
                    result_triangles[i] = Module.getValue(ptris_out + i * 4, 'i32') * 2;
                }
                // when a ring has an odd number of points one (or rather two)
                // additional vertices will be added. so the following rings
                // needs extra offset...
                var start = 0;
                for ( var j = 0, m = b_len - 1; j < m; j++) {
                    start += boundaries[b_start + j];

                    // even number of points?
                    if (!((boundaries[b_start + j] >> 1) & 1))
                        continue;

                    for ( var n = ntris * 3, tri = 0; tri < n; tri++)
                        if (result_triangles[tri] >= start)
                            result_triangles[tri] += 2;

                    start += 2;
                }
            }    
        }
        
        Module._free(pnverts);
        Module._free(pntris);
        
        Module._free(ppcoordinates_out);
        Module._free(pcoordinates_out);
        
        Module._free(pptris_out);
        Module._free(ptris_out);

        Module._free(p);
        Module._free(contours);
        
        if (mode)
                 return { vertices: result_vertices, triangles: result_triangles };
            else
                return result_triangles;
        
    };

    return tessellate;

})();
