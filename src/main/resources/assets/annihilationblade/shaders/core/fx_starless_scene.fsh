#version 150

// 星寂裁决 · 场景通道：
//  1. 天幕被劈开：竖直裂缝两侧的画面被向外推开（越靠近裂缝推得越远），裂缝本体由网格层绘制；
//  2. 星寂：全屏天空像素（深度 >= 0.9999，兼容光影包）逐渐熄灭为近黑的冷色，地表也随之略微压暗。
uniform sampler2D Sampler0; // 场景颜色
uniform sampler2D Sampler1; // 场景原始深度
uniform vec2 OutSize;
uniform vec4 Slit;   // x = 裂缝中心 u, y = 底端 v, z = 顶端 v, w = 裂缝半宽（u）
uniform vec4 Params; // x = 推开强度, y = 天空熄灭程度, z = 地表压暗, w = 未用

in vec2 texCoord;

out vec4 fragColor;

void main() {
    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 uv = texCoord;

    float halfWidth = max(Slit.w, 1.0e-4);
    float dx = (uv.x - Slit.x) * aspect;
    float influence = halfWidth * aspect * 5.0;
    float vertical = smoothstep(Slit.y - 0.05, Slit.y + 0.02, uv.y) * (1.0 - smoothstep(Slit.z - 0.08, Slit.z + 0.04, uv.y));
    float fall = 1.0 - smoothstep(0.0, influence, abs(dx));
    float shift = Params.x * halfWidth * fall * fall * vertical;
    vec2 sampleUv = vec2(uv.x - sign(dx) * shift, uv.y);
    sampleUv = clamp(sampleUv, vec2(0.001), vec2(0.999));

    vec3 color = texture(Sampler0, sampleUv).rgb;
    float depth = texture(Sampler1, sampleUv).r;
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    if (depth >= 0.9999) {
        vec3 cold = vec3(luma * 0.35, luma * 0.42, luma * 0.6) + vec3(0.004, 0.006, 0.014);
        color = mix(color, cold, clamp(Params.y, 0.0, 1.0) * 0.92);
    } else {
        color *= 1.0 - Params.z * 0.35;
    }
    fragColor = vec4(color, 1.0);
}
