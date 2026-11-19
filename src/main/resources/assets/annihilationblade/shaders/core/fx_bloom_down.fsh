#version 150

// 泛光降采样（13 taps，双线性加权）。
// 首级合并网格特效层与体积层，执行 Soft-Knee 亮度阈值与 Karis Average，
// 防止低亮底图把画面糊成一团雾，同时消除 1~2px 高能细线移动时的闪烁。
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 TexelSize;
uniform float Prefilter;
uniform float VolumeWeight;

in vec2 texCoord;

out vec4 fragColor;

float peakLuma(vec3 c) {
    return max(max(c.r, c.g), c.b);
}

vec3 prefilterColor(vec3 color) {
    float luma = peakLuma(color);
    // 截顶防极端萤火虫噪点
    color *= min(1.0, 28.0 / max(luma, 1.0e-4));
    luma = min(luma, 28.0);
    // Soft-Knee 阈值：低亮底色只保留少量辉光，高能核心全量进入泛光金字塔
    const float threshold = 0.55;
    const float knee = 0.45;
    float soft = clamp(luma - threshold + knee, 0.0, 2.0 * knee);
    soft = (soft * soft) / (4.0 * knee + 1.0e-4);
    float contrib = max(soft, luma - threshold) / max(luma, 1.0e-4);
    // 保留 12% 底色微光，确保中等亮度的法阵与雾气仍有柔和晕染而不糊屏
    return color * max(contrib, 0.12);
}

vec3 tap(vec2 offset) {
    vec2 uv = texCoord + offset * TexelSize;
    vec3 color = texture(Sampler0, uv).rgb;
    if (VolumeWeight > 0.5) {
        color += texture(Sampler1, uv).rgb;
    }
    if (Prefilter > 0.5) {
        color = prefilterColor(color);
    }
    return color;
}

float karisWeight(vec3 c) {
    return 1.0 / (1.0 + dot(c, vec3(0.2126, 0.7152, 0.0722)));
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

    vec3 color;
    if (Prefilter > 0.5) {
        // Karis Average：按 5 个子组亮度倒数加权平均，极大抑制高频细线闪烁
        vec3 g0 = (a + b + d + e) * 0.25;
        vec3 g1 = (b + c + e + f) * 0.25;
        vec3 g2 = (d + e + g + h) * 0.25;
        vec3 g3 = (e + f + h + i) * 0.25;
        vec3 g4 = (j + k + l + m) * 0.25;
        float w0 = karisWeight(g0) * 0.125;
        float w1 = karisWeight(g1) * 0.125;
        float w2 = karisWeight(g2) * 0.125;
        float w3 = karisWeight(g3) * 0.125;
        float w4 = karisWeight(g4) * 0.5;
        color = (g0 * w0 + g1 * w1 + g2 * w2 + g3 * w3 + g4 * w4) / max(w0 + w1 + w2 + w3 + w4, 1.0e-4);
    } else {
        color = e * 0.125 + (a + c + g + i) * 0.03125 + (b + d + f + h) * 0.0625 + (j + k + l + m) * 0.125;
    }
    fragColor = vec4(color, 1.0);
}
