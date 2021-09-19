#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

void main(){
    #ifdef diffuseColorFlag
    vec4 color = u_diffuseColor;
    #else
    vec4 color = vec4(1.0);
    #endif

    gl_FragColor = color;
}
