#include <org.oscim.utils.tess.Tesselator.h>

//@line:103

	#include <tesselator.h>
	#include <string.h>
	 JNIEXPORT jlong JNICALL Java_org_oscim_utils_tess_Tesselator_newTess(JNIEnv* env, jclass clazz) {


//@line:107
 {
		return (long)tessNewTess(0);
	} 

}

JNIEXPORT void JNICALL Java_org_oscim_utils_tess_Tesselator_freeTess(JNIEnv* env, jclass clazz, jlong inst) {


//@line:111
 {
		tessDeleteTess((TESStesselator*) inst);
	} 

}

JNIEXPORT void JNICALL Java_org_oscim_utils_tess_Tesselator_addContour(JNIEnv* env, jclass clazz, jlong inst, jint size, jfloatArray obj_contour, jint stride, jint offset, jint count) {
	float* contour = (float*)env->GetPrimitiveArrayCritical(obj_contour, 0);


//@line:125
 {
		tessAddContour((TESStesselator*) inst, size, contour + (offset * stride), stride, count);
	} 
	env->ReleasePrimitiveArrayCritical(obj_contour, contour, 0);

}

JNIEXPORT void JNICALL Java_org_oscim_utils_tess_Tesselator_addMultiContour2D(JNIEnv* env, jclass clazz, jlong inst, jintArray obj_index, jfloatArray obj_contour, jint idxStart, jint idxCount) {
	int* index = (int*)env->GetPrimitiveArrayCritical(obj_index, 0);
	float* contour = (float*)env->GetPrimitiveArrayCritical(obj_contour, 0);


//@line:129
 {
		TESStesselator* tess = (TESStesselator*) inst;
		int offset = 0;
		
		for (int i = 0; i < idxStart + idxCount; i++){
			int len = index[i];
			
			if ((len % 2 != 0) || (len < 0))
				break;
				
			if (len < 6 || i < idxStart) {
				offset += len;
				continue;
			}
			
			tessAddContour(tess, 2, contour + offset, 8, len >> 1);
			
			offset += len;
		}
	} 
	env->ReleasePrimitiveArrayCritical(obj_index, index, 0);
	env->ReleasePrimitiveArrayCritical(obj_contour, contour, 0);

}

JNIEXPORT jint JNICALL Java_org_oscim_utils_tess_Tesselator_tessContour2D(JNIEnv* env, jclass clazz, jlong inst, jint windingRule, jint elementType, jint polySize, jint vertexSize) {


//@line:161
{
		return tessTesselate((TESStesselator*) inst, windingRule, elementType, polySize, vertexSize, 0);
	} 

}

JNIEXPORT jint JNICALL Java_org_oscim_utils_tess_Tesselator_getVertexCount(JNIEnv* env, jclass clazz, jlong inst) {


//@line:165
{
		return tessGetVertexCount((TESStesselator*) inst);
	}

}

static inline jboolean wrapped_Java_org_oscim_utils_tess_Tesselator_getVertices
(JNIEnv* env, jclass clazz, jlong inst, jfloatArray obj_out, jint offset, jint length, float* out) {

//@line:172
{
		const TESSIOreal* vertices = tessGetVertices((TESStesselator*) inst);
		
		//const TESSreal* vertices = tessGetVertices((TESStesselator*) inst);
		
		if (!vertices)
			return 0;
			
		memcpy(out, vertices + offset, length * sizeof(TESSIOreal));
		
		//memcpy(out, vertices + offset, length * sizeof(TESSreal));
		
		return 1;
	}
}

JNIEXPORT jboolean JNICALL Java_org_oscim_utils_tess_Tesselator_getVertices(JNIEnv* env, jclass clazz, jlong inst, jfloatArray obj_out, jint offset, jint length) {
	float* out = (float*)env->GetPrimitiveArrayCritical(obj_out, 0);

	jboolean JNI_returnValue = wrapped_Java_org_oscim_utils_tess_Tesselator_getVertices(env, clazz, inst, obj_out, offset, length, out);

	env->ReleasePrimitiveArrayCritical(obj_out, out, 0);

	return JNI_returnValue;
}

JNIEXPORT void JNICALL Java_org_oscim_utils_tess_Tesselator_getVerticesS(JNIEnv* env, jclass clazz, jlong inst, jshortArray obj_out, jint offset, jint length, jfloat scale) {
	short* out = (short*)env->GetPrimitiveArrayCritical(obj_out, 0);


//@line:190
{
		const TESSIOreal* vertices = tessGetVertices((TESStesselator*) inst);
		
		//const TESSreal* vertices = tessGetVertices((TESStesselator*) inst);
		
		for(int i = 0; i < length; i++)
			out[i] = (short)(vertices[offset++] * scale + 0.5f);
	}
	env->ReleasePrimitiveArrayCritical(obj_out, out, 0);

}

static inline jboolean wrapped_Java_org_oscim_utils_tess_Tesselator_getVertexIndices
(JNIEnv* env, jclass clazz, jlong inst, jintArray obj_out, jint offset, jint length, int* out) {

//@line:206
 {
		const TESSindex* indices = tessGetVertexIndices((TESStesselator*) inst);
		if (!indices)
			return 0;
			
		memcpy(out, indices + offset, length * sizeof(TESSindex));
		return 1;
	} 
}

JNIEXPORT jboolean JNICALL Java_org_oscim_utils_tess_Tesselator_getVertexIndices(JNIEnv* env, jclass clazz, jlong inst, jintArray obj_out, jint offset, jint length) {
	int* out = (int*)env->GetPrimitiveArrayCritical(obj_out, 0);

	jboolean JNI_returnValue = wrapped_Java_org_oscim_utils_tess_Tesselator_getVertexIndices(env, clazz, inst, obj_out, offset, length, out);

	env->ReleasePrimitiveArrayCritical(obj_out, out, 0);

	return JNI_returnValue;
}

JNIEXPORT jint JNICALL Java_org_oscim_utils_tess_Tesselator_getElementCount(JNIEnv* env, jclass clazz, jlong inst) {


//@line:218
{
		return tessGetElementCount((TESStesselator*) inst);
	}

}

static inline jboolean wrapped_Java_org_oscim_utils_tess_Tesselator_getElements
(JNIEnv* env, jclass clazz, jlong inst, jintArray obj_out, jint offset, jint length, int* out) {

//@line:225
{
		const TESSindex* elements = tessGetElements((TESStesselator*) inst);
		if (!elements)
			return 0;
			
		memcpy(out, elements + offset, length * sizeof(TESSindex));
		return 1;
	}
}

JNIEXPORT jboolean JNICALL Java_org_oscim_utils_tess_Tesselator_getElements(JNIEnv* env, jclass clazz, jlong inst, jintArray obj_out, jint offset, jint length) {
	int* out = (int*)env->GetPrimitiveArrayCritical(obj_out, 0);

	jboolean JNI_returnValue = wrapped_Java_org_oscim_utils_tess_Tesselator_getElements(env, clazz, inst, obj_out, offset, length, out);

	env->ReleasePrimitiveArrayCritical(obj_out, out, 0);

	return JNI_returnValue;
}

JNIEXPORT void JNICALL Java_org_oscim_utils_tess_Tesselator_getElementsS(JNIEnv* env, jclass clazz, jlong inst, jshortArray obj_out, jint offset, jint length) {
	short* out = (short*)env->GetPrimitiveArrayCritical(obj_out, 0);


//@line:238
{
		const TESSindex* elements = tessGetElements((TESStesselator*) inst);
		for(int i = 0; i < length; i++)
			out[i] = (short)elements[offset++];
	}
	env->ReleasePrimitiveArrayCritical(obj_out, out, 0);

}

JNIEXPORT void JNICALL Java_org_oscim_utils_tess_Tesselator_getElementsWithInputVertexIds(JNIEnv* env, jclass clazz, jlong inst, jshortArray obj_out, jint dstOffset, jint offset, jint length) {
	short* out = (short*)env->GetPrimitiveArrayCritical(obj_out, 0);


//@line:247
{
		const TESSindex* elements = tessGetElements((TESStesselator*) inst);
		const TESSindex* indices = tessGetVertexIndices((TESStesselator*) inst);
		
		for(int i = 0; i < length; i++)
			out[dstOffset++] = (short)indices[elements[offset++]];
	}
	env->ReleasePrimitiveArrayCritical(obj_out, out, 0);

}

