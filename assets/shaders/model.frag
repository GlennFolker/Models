#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
varying vec2 v_diffuseUV;
#endif

#ifdef emissiveTextureFlag
uniform sampler2D u_emissiveTexture;
varying vec2 v_emissiveUV;
#endif

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef emissiveColorFlag
uniform vec4 u_emissiveColor;
#endif

#if numDirLights > 0
#define lightingFlag
#endif

#ifdef lightingFlag
varying vec4 v_lightDiffuse;
#endif

void main(){
    #if defined(diffuseTextureFlag) && defined(diffuseColorFlag)
    vec4 color = texture2D(u_diffuseTexture, v_diffuseUV) * u_diffuseColor;
    #elif defined(diffuseTextureFlag)
    vec4 color = texture2D(u_diffuseTexture, v_diffuseUV);
    #elif defined(diffuseColorFlag)
    vec4 color = u_diffuseColor;
    #else
    vec4 color = vec4(1.0);
    #endif

    color *= color.a;

    #if defined(emissiveTextureFlag) && defined(emissiveColorFlag)
    vec4 emit = texture2D(u_emissiveTexture, v_emissiveUV) * u_emissiveColor;
    #elif defined(emissiveTextureFlag)
    vec4 emit = texture2D(u_emissiveTexture, v_emissiveUV);
    #elif defined(emissiveColorFlag)
    vec4 emit = u_emissiveColor;
    #else
    vec4 emit = vec4(0.0);
    #endif

    emit *= emit.a;

    #if !defined(lightingFlag)
    gl_FragColor = color + emit;
    #else
    gl_FragColor = (color * v_lightDiffuse) + emit;
    #endif
}
