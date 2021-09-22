attribute vec4 a_position;
attribute vec3 a_normal;

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
    }
}
