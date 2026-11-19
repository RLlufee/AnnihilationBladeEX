#version 150

#moj_import <annihilationblade:fx_common.glsl>

// 网格特效统一着色器：按 FxStyle 程序化生成辉光、能量条带、法阵、环带，输出 HDR 发光（加法混合）。
uniform sampler2D Sampler0; // 场景原始深度（R32F）
uniform mat4 ProjMat;
uniform float FxStyle;
uniform float FxEmission;
uniform float FxSoft;
uniform float FxTime;
uniform vec4 FxParams;
uniform vec4 FxOccluder; // xyz = 遮挡球心（相对相机），w = 半径；w <= 0 表示无遮挡

in vec4 vertexColor;
in vec2 texCoord0;
in float viewDistance;
in vec3 relativePos;

out vec4 fragColor;

float linearDepth(float depth) {
    float ndc = depth * 2.0 - 1.0;
    return ProjMat[3][2] / (ndc + ProjMat[2][2]);
}

// 软深度：与场景几何相交处平滑淡出；FxSoft <= 0 时为带少量偏移的硬遮挡。
float depthMask() {
    float depth = texelFetch(Sampler0, ivec2(gl_FragCoord.xy), 0).r;
    float scene = depth >= 1.0 ? 1.0e6 : linearDepth(depth);
    float diff = scene - viewDistance;
    if (FxSoft <= 0.0) {
        return diff > -(0.03 + viewDistance * 0.002) ? 1.0 : 0.0;
    }
    return clamp(diff / FxSoft, 0.0, 1.0);
}

// 遮挡球（黑洞事件视界）：位于球体背后的片元按视线与球心的最近距离平滑剔除。
float occluderMask() {
    if (FxOccluder.w <= 0.0) {
        return 1.0;
    }
    float fragDist = length(relativePos);
    vec3 rd = relativePos / max(fragDist, 1.0e-4);
    float tc = dot(FxOccluder.xyz, rd);
    if (tc <= 0.0) {
        return 1.0;
    }
    float dRay = length(FxOccluder.xyz - rd * tc);
    float r = FxOccluder.w;
    float entry = tc - sqrt(max(r * r - dRay * dRay, 0.0));
    if (fragDist <= entry) {
        return 1.0;
    }
    return smoothstep(r * 0.96, r * 1.03, dRay);
}

vec2 rotate2(vec2 v, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec2(c * v.x - s * v.y, s * v.x + c * v.y);
}

float aaLine(float distance, float width, float aa) {
    return 1.0 - smoothstep(width, width + aa, abs(distance));
}

float sdSegment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

// 正 N 边形边界距离（外接圆半径 r），通过极坐标折叠到单个扇区。
float sdPolygon(vec2 p, float r, float n, float phase) {
    float a = atan(p.y, p.x) + phase;
    float sector = FX_TAU / n;
    float local = mod(a + sector * 0.5, sector) - sector * 0.5;
    float apothem = r * cos(FX_PI / n);
    return length(p) * cos(local) - apothem;
}

// 扇区符文：在 (角向, 径向) 局部盒内用哈希挑选 3 笔划。
float runeGlyph(vec2 local, float h, float aa) {
    float strokes = 0.0;
    vec2 grid[9] = vec2[9](vec2(-1.0, -1.0), vec2(0.0, -1.0), vec2(1.0, -1.0), vec2(-1.0, 0.0), vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(-1.0, 1.0), vec2(0.0, 1.0), vec2(1.0, 1.0));
    for (int i = 0; i < 3; i++) {
        float ha = fxHash11(h * 91.7 + float(i) * 13.1);
        float hb = fxHash11(h * 37.3 + float(i) * 7.9 + 1.0);
        int ia = int(ha * 8.999);
        int ib = int(hb * 8.999);
        if (ia == ib) {
            ib = 8 - ia;
        }
        float d = sdSegment(local, grid[ia], grid[ib]);
        strokes = max(strokes, aaLine(d, 0.16, aa * 6.0));
    }
    return strokes;
}

float runeBand(float angle, float r, float inner, float outer, float count, float seed, float aa) {
    if (r < inner || r > outer) {
        return 0.0;
    }
    float t = angle / FX_TAU * count;
    float sector = floor(t);
    float sa = fract(t) * 2.0 - 1.0;
    float rb = (r - inner) / (outer - inner) * 2.0 - 1.0;
    float h = fxHash12(vec2(sector, seed));
    if (h < 0.12) {
        return 0.0;
    }
    vec2 local = vec2(sa * 1.45, rb * 1.45);
    if (abs(local.x) > 1.15 || abs(local.y) > 1.15) {
        return 0.0;
    }
    return runeGlyph(local, h, aa);
}

vec3 styleGlow(vec2 uv) {
    float r = length(uv);
    float core = exp(-r * r * FxParams.x * 4.0);
    float halo = exp(-r * FxParams.y * 2.0);
    float streak = FxParams.z * exp(-abs(uv.y) * 28.0) * (1.0 - smoothstep(0.0, 1.0, abs(uv.x)));
    float edge = 1.0 - smoothstep(0.7, 1.0, r);
    float intensity = (core * 1.2 + halo * 0.55) * edge + streak * 0.6;
    return vertexColor.rgb * intensity + vec3(core * core * 0.8 * edge);
}

vec3 styleRibbon(vec2 uv) {
    float y = abs(uv.y);
    float core = exp(-y * FxParams.x);
    float glow = exp(-y * 2.2) * FxParams.w;
    float flicker = FxParams.y > 0.0 ? 0.7 + 0.6 * fxNoise1(uv.x * FxParams.y - FxTime * FxParams.z) : 1.0;
    float edge = 1.0 - smoothstep(0.82, 1.0, y);
    float intensity = (core + glow) * flicker * edge;
    return vertexColor.rgb * intensity + vec3(core * core * core * 0.9 * edge);
}

vec3 styleRune(vec2 uv) {
    float r = length(uv);
    if (r > 1.0) {
        return vec3(0.0);
    }
    float aa = max(fwidth(r) * 1.2, 0.0015);
    float rotation = FxParams.x;
    float seed = FxParams.y;
    float grand = step(0.5, FxParams.z);
    float reveal = FxParams.w;
    float baseAngle = atan(uv.y, uv.x);

    float lines = 0.0;
    float detail = 0.0;
    if (grand > 0.5) {
        lines += aaLine(r - 0.985, 0.006, aa) + aaLine(r - 0.95, 0.003, aa);
        lines += aaLine(r - 0.80, 0.004, aa) + aaLine(r - 0.74, 0.003, aa);
        lines += aaLine(r - 0.42, 0.004, aa) + aaLine(r - 0.38, 0.0025, aa);
        float a1 = mod(baseAngle + rotation + FX_TAU, FX_TAU);
        detail += runeBand(a1, r, 0.81, 0.94, 48.0, seed, aa);
        float a2 = mod(baseAngle - rotation * 0.5 + FX_TAU, FX_TAU);
        float tick = abs(fract(a2 / FX_TAU * 144.0) - 0.5);
        float major = step(0.5, fract(a2 / FX_TAU * 14.4));
        detail += (1.0 - smoothstep(0.06, 0.06 + aa * 40.0, tick)) * step(0.745, r) * step(r, 0.745 + mix(0.03, 0.05, major));
        vec2 star = rotate2(uv, -rotation * 0.7);
        float hex = min(abs(sdPolygon(star, 0.735, 3.0, 0.0)), abs(sdPolygon(star, 0.735, 3.0, FX_PI / 3.0)));
        lines += aaLine(hex, 0.0035, aa);
        for (int i = 0; i < 6; i++) {
            float a = float(i) * FX_PI / 3.0 + FX_PI / 6.0 - rotation * 0.7;
            vec2 c = vec2(cos(a), sin(a)) * 0.58;
            lines += aaLine(length(uv - c) - 0.06, 0.003, aa) * 0.8;
         }
        float a3 = mod(baseAngle - rotation * 1.6 + FX_TAU, FX_TAU);
        detail += runeBand(a3, r, 0.22, 0.34, 16.0, seed + 11.0, aa) * 0.9;
        lines += aaLine(r - 0.2, 0.003, aa);
    } else {
        lines += aaLine(r - 0.97, 0.008, aa) + aaLine(r - 0.88, 0.004, aa);
        vec2 star = rotate2(uv, rotation);
        float hex = min(abs(sdPolygon(star, 0.875, 3.0, 0.0)), abs(sdPolygon(star, 0.875, 3.0, FX_PI / 3.0)));
        lines += aaLine(hex, 0.006, aa);
        lines += aaLine(r - 0.44, 0.004, aa) + aaLine(r - 0.12, 0.005, aa);
        float a1 = mod(baseAngle - rotation * 0.8 + FX_TAU, FX_TAU);
        float tick = abs(fract(a1 / FX_TAU * 72.0) - 0.5);
        detail += (1.0 - smoothstep(0.08, 0.08 + aa * 30.0, tick)) * step(0.885, r) * step(r, 0.955);
        detail += runeBand(mod(baseAngle + rotation * 1.3 + FX_TAU, FX_TAU), r, 0.5, 0.62, 18.0, seed, aa) * 0.8;
    }

    float sweep = (baseAngle + FX_PI) / FX_TAU;
    float visible = reveal >= 1.0 ? 1.0 : smoothstep(reveal, reveal - 0.02, sweep);
    float leading = reveal >= 1.0 ? 0.0 : exp(-abs(sweep - reveal) * 60.0) * step(0.3, r);
    float shimmer = 0.85 + 0.15 * sin(FxTime * 7.0 + baseAngle * 3.0 + r * 9.0);
    float fill = (exp(-abs(r - 0.985) * 40.0) * 0.45 + (1.0 - r) * 0.06) * visible;
    float shape = clamp(lines + detail * 0.85, 0.0, 1.6) * visible * shimmer + fill + leading * 1.5;
    return vertexColor.rgb * shape + vec3(clamp(lines, 0.0, 1.0) * 0.35 * visible);
}

vec3 styleRing(vec2 uv) {
    float r = length(uv);
    if (r > 1.0) {
        return vec3(0.0);
    }
    float width = max(FxParams.y, 1.0e-4);
    float d = abs(r - FxParams.x);
    float ring = exp(-(d * d) / (width * width)) + 0.35 * exp(-d / (width * 4.0));
    if (FxParams.z > 0.0) {
        float a = (atan(uv.y, uv.x) + FX_PI) / FX_TAU;
        float f = fract(a * FxParams.z + FxTime * FxParams.w);
        float dash = smoothstep(0.0, 0.08, f) * (1.0 - smoothstep(0.55, 0.63, f));
        ring *= mix(0.2, 1.0, dash);
    }
    return vertexColor.rgb * ring + vec3(exp(-(d * d) / (width * width * 0.2)) * 0.5);
}

// 湍流光束：横截面 |uv.y| 视作半径，用两层流动噪声扰动半径，得到翻滚的烟壳与稳定的白热核心。
vec3 styleBeam(vec2 uv) {
    float y = abs(uv.y);
    float flow = FxTime * FxParams.y;
    float n1 = fxNoise3(vec3(uv.x * FxParams.x - flow, uv.y * 2.0, 1.7));
    float n2 = fxNoise3(vec3(uv.x * FxParams.x * 2.3 - flow * 1.7 + 7.0, uv.y * 4.0, 4.3));
    float turbulence = n1 * 0.65 + n2 * 0.35;
    float radial = clamp(y + (turbulence - 0.5) * 0.45 * FxParams.z, 0.0, 1.0);
    float core = pow(1.0 - radial, 6.0);
    float hot = pow(1.0 - radial, 16.0);
    float shell = pow(1.0 - radial, 1.6) * smoothstep(0.3, 0.8, turbulence);
    float rim = exp(-abs(radial - 0.7) * 12.0) * FxParams.w;
    float edge = 1.0 - smoothstep(0.86, 1.0, y);
    float intensity = (core * 0.9 + shell * 0.6 + rim * 0.5) * edge;
    return vertexColor.rgb * intensity + vec3(hot * 1.2 * edge);
}

// 极光帷幕：竖直光带自底部涌起，顶部随升起进度柔和截断；叠加横向扫描线与竖向网格。
vec3 styleCurtain(vec2 uv) {
    float h = uv.y * 0.5 + 0.5;
    float rise = FxParams.z;
    float topFade = 1.0 - smoothstep(rise - 0.1, rise, h);
    if (topFade <= 0.0) {
        return vec3(0.0);
    }
    float x = uv.x;
    float seed = FxParams.x;
    float bands = fxNoise3(vec3(x * 18.0, h * 1.5 - FxTime * 0.6, seed));
    float bands2 = fxNoise3(vec3(x * 47.0 + 3.0, h * 3.0 - FxTime * 1.1, seed + 5.0));
    float streak = pow(bands * 0.6 + bands2 * 0.4, 3.0) * 2.2;
    float base = exp(-h * 5.0) * 1.2;
    float crest = exp(-abs(h - rise) * 30.0) * 0.8 * step(rise, 0.995);
    float scan = pow(0.5 + 0.5 * sin(h * 120.0 - FxTime * 8.0), 18.0) * 0.35 * FxParams.y;
    float gx = abs(fract(x * 16.0) - 0.5);
    float vline = (1.0 - smoothstep(0.02, 0.02 + fwidth(x * 16.0) * 1.5, gx)) * 0.25 * FxParams.y;
    float ends = 1.0 - smoothstep(0.96, 1.0, abs(x));
    float dissolve = FxParams.w > 0.0 ? smoothstep(FxParams.w - 0.15, FxParams.w + 0.15, fxNoise3(vec3(x * 9.0, h * 6.0, seed + 9.0)) * 0.8 + h * 0.2) : 1.0;
    float intensity = (streak * (1.0 - h * 0.6) + base + crest + scan + vline) * topFade * ends * dissolve;
    return mix(vertexColor.rgb, vec3(0.75, 0.6, 1.0), h * 0.7) * intensity;
}

// 吸积盘：按半径差速旋转的噪声光带，内缘白热，多普勒效应让一侧更亮。
vec3 styleDisk(vec2 uv) {
    float r = length(uv);
    float inner = FxParams.x;
    if (r > 1.0 || r < inner * 0.8) {
        return vec3(0.0);
    }
    vec2 q = rotate2(uv, FxTime * FxParams.y / max(r, 0.15));
    float bands = 0.55 + 0.45 * fxNoise3(vec3(q * 7.0, r * 12.0 + FxParams.z));
    float fine = fxNoise3(vec3(q * 21.0, r * 30.0 + FxParams.z));
    float profile = smoothstep(inner * 0.9, inner * 1.15, r) * pow(1.0 - smoothstep(inner, 1.0, r), 1.5);
    float doppler = 1.0 + FxParams.w * sin(atan(uv.y, uv.x));
    float hot = exp(-(r - inner) * 14.0);
    vec3 inside = vec3(1.0, 0.85, 0.6);
    vec3 tint = mix(inside, vertexColor.rgb, smoothstep(inner, 0.8, r));
    return (tint * (bands * 0.8 + fine * 0.4) + vec3(1.0, 0.95, 0.9) * hot * 2.0) * profile * max(doppler, 0.1);
}

void main() {
    float mask = depthMask() * occluderMask();
    if (mask <= 0.0 || vertexColor.a <= 0.0) {
        discard;
    }
    vec3 color;
    if (FxStyle < 0.5) {
        color = styleGlow(texCoord0);
    } else if (FxStyle < 1.5) {
        color = styleRibbon(texCoord0);
    } else if (FxStyle < 2.5) {
        color = styleRune(texCoord0);
    } else if (FxStyle < 3.5) {
        color = styleRing(texCoord0);
    } else if (FxStyle < 4.5) {
        color = styleBeam(texCoord0);
    } else if (FxStyle < 5.5) {
        color = styleCurtain(texCoord0);
    } else {
        color = styleDisk(texCoord0);
    }
    fragColor = vec4(max(color, vec3(0.0)) * vertexColor.a * FxEmission * mask, 1.0);
}
