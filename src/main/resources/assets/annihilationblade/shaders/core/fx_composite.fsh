#version 150

// 最终合成：场景（含径向模糊与色差）+ HDR 特效层 + 体积层 + 泛光，再叠加去色、暗角、闪白。
// 发光部分先做指数色调映射再以「滤色」方式叠加到场景上，亮部不会硬截断成一片死白。
uniform sampler2D Sampler0; // 场景
uniform sampler2D Sampler1; // 网格特效层
uniform sampler2D Sampler2; // 体积层（半分辨率）
uniform sampler2D Sampler3; // 泛光
uniform vec2 OutSize;
uniform float VolumeWeight;
uniform float BloomStrength;
uniform vec4 Flash;
uniform float Aberration;
uniform float ZoomBlur;
uniform vec2 Center;
uniform vec4 Vignette;
uniform float Desaturate;

in vec2 texCoord;

out vec4 fragColor;

vec3 sampleScene(vec2 uv, vec2 dir) {
    if (Aberration <= 0.001) {
        return texture(Sampler0, uv).rgb;
    }
    vec2 shift = dir * Aberration * 0.012;
    return vec3(
        texture(Sampler0, clamp(uv + shift, vec2(0.0), vec2(1.0))).r,
        texture(Sampler0, uv).g,
        texture(Sampler0, clamp(uv - shift, vec2(0.0), vec2(1.0))).b
    );
}

void main() {
    vec2 uv = texCoord;
    vec2 dir = uv - Center;

    vec3 scene;
    if (ZoomBlur > 0.001) {
        vec3 sum = vec3(0.0);
        float weight = 0.0;
        for (int i = 0; i < 12; i++) {
            float k = float(i) / 11.0;
            float w = 1.0 - k * 0.6;
            sum += sampleScene(uv - dir * ZoomBlur * 0.12 * k, dir) * w;
            weight += w;
        }
        scene = sum / weight;
    } else {
        scene = sampleScene(uv, dir);
    }

    vec3 glow = texture(Sampler1, uv).rgb;
    glow += texture(Sampler2, uv).rgb * VolumeWeight;
    glow += texture(Sampler3, uv).rgb * BloomStrength * 0.16;

    vec3 mapped = 1.0 - exp(-glow);
    vec3 color = 1.0 - (1.0 - scene) * (1.0 - mapped);
    color += max(glow - 1.0, vec3(0.0)) * 0.08;

    if (Desaturate > 0.001) {
        float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
        color = mix(color, vec3(luma) * vec3(0.92, 0.9, 1.0), Desaturate);
    }

    if (Vignette.a > 0.001) {
        float aspect = OutSize.x / max(OutSize.y, 1.0);
        float v = smoothstep(0.25, 1.0, length((uv - 0.5) * vec2(aspect, 1.0)) * 1.1);
        color = mix(color, Vignette.rgb, v * Vignette.a);
    }

    if (Flash.a > 0.001) {
        color = mix(color, Flash.rgb, Flash.a * 0.85);
    }

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
