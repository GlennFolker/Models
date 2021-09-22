attribute vec4 a_position;

attribute vec4 a_normal;
uniform mat4 u_normalMatrix;
varying vec3 v_normal;

uniform int u_renderType;
uniform mat4 u_proj;
uniform mat4 u_trans;

uniform vec3 u_camPos;
uniform vec2 u_res;
uniform vec2 u_scl;

#if defined(diffuseTextureFlag) || defined(emissiveTextureFlag)
attribute vec2 a_texCoord0;
#endif

#ifdef diffuseTextureFlag
uniform vec4 u_diffuseUV;
varying vec2 v_diffuseUV;
#endif

#ifdef emissiveTextureFlag
uniform vec4 u_emissiveUV;
varying vec2 v_emissiveUV;
#endif

#if numDirLights > 0
#define lightingFlag
#endif

#ifdef lightingFlag
varying vec4 v_lightDiffuse;
#if numDirLights > 0
struct DirLights{
    vec4 color;
    vec3 dir;
};
uniform int u_dirLightsSize;
uniform DirLights u_dirLights[numDirLights];
#endif
#endif

void main(){
    #ifdef diffuseTextureFlag
    v_diffuseUV = u_diffuseUV.xy + a_texCoord0 * u_diffuseUV.zw;
    #endif

    #ifdef emissiveTextureFlag
    v_emissiveUV = u_emissiveUV.xy + a_texCoord0 * u_emissiveUV.zw;
    #endif

    if(u_renderType == 0){
        gl_Position = u_proj * u_trans * a_position;
    }else if(u_renderType == 1){
        mat4 view = u_trans;
        vec4 translation = vec4(view[3][0], view[3][1], view[3][2], 0.0);

        view[3][0] = u_camPos.x;
        view[3][1] = u_camPos.y;

        vec2 diff = u_camPos.xy - translation.xy;
        vec4 pos = u_proj * view * a_position;
        pos.xy *= u_scl;

        pos -= vec4(diff * pos.z * 2.0415 / u_res, 0.0, 0.0);
        gl_Position = pos;
    };

    v_normal = normalize(u_normalMatrix * a_normal).xyz;

    #ifdef lightingFlag
    #if numDirLights > 0
    for(int i = 0; i < u_dirLightsSize; i++){
        vec3 lightDir = -u_dirLights[i].dir;
        float NdotL = clamp(dot(v_normal, lightDir), 0.0, 1.0);
        vec4 value = u_dirLights[i].color * NdotL;

        v_lightDiffuse += value;
    }
    #endif
    #endif
}
