attribute vec4 a_position;

uniform int u_renderType;
uniform mat4 u_proj;
uniform mat4 u_trans;

uniform vec3 u_camPos;
uniform vec2 u_scl;

void main(){
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
