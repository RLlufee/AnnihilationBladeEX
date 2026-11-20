#version 150

// 断空 · 场景通道「世界被切开」：最多 8 条屏幕空间切线。
// 每条切线两侧的画面沿切线方向反向错位（像被居合斩开后滑移），切缝处露出虚空暗隙，缝口带冷白刃光。
// 有限切线（mode = 0）只在线段附近的窄带内轻微错位，用于每一闪的局部空间扰动；
// 无限切线（mode = 1）把整个半平面推开，用于納刀时横贯画面的齐斩。
uniform sampler2D Sampler0; // 场景颜色
uniform vec2 OutSize;
uniform float FxTime;
// 每个 mat4 打包两条切线：列 0/2 = (a.x, a.y, b.x, b.y)（屏幕 UV），列 1/3 = (错位量, 缝口宽度, 模式, 刃光强度)
uniform mat4 CutsA;
uniform mat4 CutsB;
uniform mat4 CutsC;
uniform mat4 CutsD;
uniform vec4 Slice; // x = 切线数量, y = 暗隙强度, z = 刃光强度, w = 未用

in vec2 texCoord;

out vec4 fragColor;

void applyCut(vec4 seg, vec4 prm, vec2 p, float aspect, inout vec2 offset, inout float seam, inout float gap) {
    vec2 a = vec2(seg.x * aspect, seg.y);
    vec2 b = vec2(seg.z * aspect, seg.w);
    vec2 ab = b - a;
    float len = length(ab);
    if (len < 1.0e-4) {
        return;
    }
    vec2 dir = ab / len;
    vec2 n = vec2(-dir.y, dir.x);
    vec2 q = p - a;
    float d = dot(q, n);
    float t = dot(q, dir) / len;
    bool infinite = prm.z > 0.5;
    float along = infinite ? 1.0 : smoothstep(-0.3, 0.0, t) * (1.0 - smoothstep(1.0, 1.3, t));
    float band = infinite ? 1.0 : exp(-abs(d) / 0.05);
    float side = d >= 0.0 ? 1.0 : -1.0;
    vec2 o = dir * (side * prm.x * along * band);
    offset += vec2(o.x / aspect, o.y);
    float width = max(prm.y, 1.0e-4);
    float flicker = 0.85 + 0.15 * sin(FxTime * 47.0 + t * 31.0);
    seam += prm.w * along * exp(-abs(d) / width) * flicker;
    // 暗隙宽度随错位量张开
    float gapWidth = abs(prm.x) * 0.6;
    if (gapWidth > 1.0e-5) {
        gap = max(gap, along * (1.0 - smoothstep(gapWidth * 0.5, gapWidth, abs(d))));
    }
}

void main() {
    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 p = vec2(texCoord.x * aspect, texCoord.y);
    int count = int(Slice.x + 0.5);
    vec2 offset = vec2(0.0);
    float seam = 0.0;
    float gap = 0.0;
    if (count > 0) applyCut(CutsA[0], CutsA[1], p, aspect, offset, seam, gap);
    if (count > 1) applyCut(CutsA[2], CutsA[3], p, aspect, offset, seam, gap);
    if (count > 2) applyCut(CutsB[0], CutsB[1], p, aspect, offset, seam, gap);
    if (count > 3) applyCut(CutsB[2], CutsB[3], p, aspect, offset, seam, gap);
    if (count > 4) applyCut(CutsC[0], CutsC[1], p, aspect, offset, seam, gap);
    if (count > 5) applyCut(CutsC[2], CutsC[3], p, aspect, offset, seam, gap);
    if (count > 6) applyCut(CutsD[0], CutsD[1], p, aspect, offset, seam, gap);
    if (count > 7) applyCut(CutsD[2], CutsD[3], p, aspect, offset, seam, gap);

    // 错位方向上做轻微色散：红通道走得更远、蓝通道更近
    vec2 uvR = clamp(texCoord + offset * 1.08, vec2(0.001), vec2(0.999));
    vec2 uvG = clamp(texCoord + offset, vec2(0.001), vec2(0.999));
    vec2 uvB = clamp(texCoord + offset * 0.92, vec2(0.001), vec2(0.999));
    vec3 color = vec3(texture(Sampler0, uvR).r, texture(Sampler0, uvG).g, texture(Sampler0, uvB).b);

    vec3 voidColor = vec3(0.012, 0.02, 0.05);
    color = mix(color, voidColor, clamp(gap * Slice.y, 0.0, 1.0));
    seam = clamp(seam, 0.0, 3.0);
    vec3 edgeColor = mix(vec3(0.55, 0.92, 1.0), vec3(1.0), clamp(seam * 0.5, 0.0, 1.0));
    color += edgeColor * seam * Slice.z;
    fragColor = vec4(color, 1.0);
}
