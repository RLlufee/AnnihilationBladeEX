#version 150

// 泛光降采样（13 taps，双线性加权），首级合并网格特效层与体积层并抑制过亮像素以避免闪烁。
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 TexelSize;
uniform float Prefilter;
uniform float VolumeWeight;

in vec2 texCoord;

out vec4 fragColor;

vec3 tap(vec2 offset) {
    vec2 uv = texCoord + offset * TexelSize;
    vec3 color = texture(Sampler0, uv).rgb;
    if (VolumeWeight > 0.5) {
        color += texture(Sampler1, uv).rgb;
    }
    if (Prefilter > 0.5) {
        float luma = max(max(color.r, color.g), color.b);
        color *= min(1.0, 24.0 / max(luma, 1.0e-4));
    }
    return color;
}

void main() {
    vec3 a = tap(vec2(-2.0, 2.0));
    vec3 b = tap(vec2(0.0, 2.0));
    vec3 c = tap(vec2(2.0, 2.0));
    vec3 d = tap(vec2(-2.0, 0.0));
    vec3 e = tap(vec2(0.0, 0.0));
    vec3 f = tap(vec2(2.0, 0.0));
    vec3 g = tap(vec2(-2.0, -2.0));
    vec3 h = tap(vec2(0.0, -2.0));
    vec3 i = tap(vec2(2.0, -2.0));
    vec3 j = tap(vec2(-1.0, 1.0));
    vec3 k = tap(vec2(1.0, 1.0));
    vec3 l = tap(vec2(-1.0, -1.0));
    vec3 m = tap(vec2(1.0, -1.0));
    vec3 color = e * 0.125 + (a + c + g + i) * 0.03125 + (b + d + f + h) * 0.0625 + (j + k + l + m) * 0.125;
    fragColor = vec4(color, 1.0);
}
