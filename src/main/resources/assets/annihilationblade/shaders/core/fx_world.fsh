#version 150

#moj_import <annihilationblade:fx_common.glsl>

// 网格特效统一着色器：
// 支持 16 种程序化 + 流场贴图驱动着色风格，兼容 HDR 加法发光层（FxBlendMode = 0）
// 与预乘 Alpha 实体/暗部衬底层（FxBlendMode = 1）。
uniform sampler2D Sampler0; // 场景原始深度（R32F）
uniform sampler2D Sampler1; // 256x256 无缝四通道流场噪声贴图（R=fBm云雾, G=Worley脊线, BA=旋度偏移）
uniform mat4 ProjMat;
uniform float FxStyle;
uniform float FxEmission;
uniform float FxSoft;
uniform float FxTime;
uniform vec4 FxParams;
uniform vec4 FxOccluder;  // xyz = 遮挡球心（相对相机），w = 半径；w <= 0 表示无遮挡
uniform vec4 FxGeoCenter; // xyz = 球面/几何参考中心（相对相机），w > 0.5 启用平滑解析球面法线
uniform float FxBlendMode; // 0 = HDR 加法发光层，1 = 预乘 Alpha 本体/暗部层

in vec4 vertexColor;
in vec2 texCoord0;
in float viewDistance;
in vec3 relativePos;

out vec4 fragColor;

// 所有导数须在 discard 之前求取（见 main），否则部分驱动下结果未定义。
vec3 gFaceNormal = vec3(0.0, 1.0, 0.0);
float gRadialWidth = 0.002;
float gCurtainWidth = 0.01;

float linearDepth(float depth) {
    float ndc = depth * 2.0 - 1.0;
    return ProjMat[3][2] / (ndc + ProjMat[2][2]);
}

// 软深度：与场景几何相交处平滑淡出；FxSoft <= 0 时为带少量偏移的硬遮挡。
float depthMask() {
    float depth = texelFetch(Sampler0, ivec2(gl_FragCoord.xy), 0).r;
    float scene = depth >= 0.9999 ? 1.0e6 : linearDepth(depth);
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

// 解析几何法线：当启用 FxGeoCenter 时用球面解析法线消除 Flat Shading 多边形棱面。
vec3 surfaceNormal() {
    if (FxGeoCenter.w > 0.5) {
        vec3 delta = relativePos - FxGeoCenter.xyz;
        float len = length(delta);
        if (len > 1.0e-5) {
            return delta / len;
        }
    }
    return gFaceNormal;
}

// 三速无缝流场偏移采样（uv.x 倍率取整数，确保圆柱与球体在 u=0/1 接缝处完全连续）。
vec2 sampleFlowOffset(vec2 uv, float speed) {
    float t = FxTime * speed;
    vec4 fastSample = texture(Sampler1, fract(vec2(uv.x * 2.0 + t * 0.42, uv.y * 0.85 - t * 0.65)));
    vec4 slowSample = texture(Sampler1, fract(vec2(uv.x * 1.0 - t * 0.18, uv.y * 1.45 - t * 0.28)));
    vec2 v1 = fastSample.ba * 2.0 - 1.0;
    vec2 v2 = slowSample.ba * 2.0 - 1.0;
    return v1 * 0.085 + v2 * 0.055;
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

float dot2(vec2 v) {
    return dot(v, v);
}

// 爱心 SDF（Inigo Quilez），心形底尖在原点，高约 1.06。
float sdHeart(vec2 p) {
    p.x = abs(p.x);
    if (p.y + p.x > 1.0) {
        return sqrt(dot2(p - vec2(0.25, 0.75))) - 0.35355339;
    }
    return sqrt(min(dot2(p - vec2(0.0, 1.0)), dot2(p - 0.5 * max(p.x + p.y, 0.0)))) * sign(p.x - p.y);
}

// 五角星 SDF（Inigo Quilez），r = 外径，rf = 内凹比例。
float sdStar5(vec2 p, float r, float rf) {
    const vec2 k1 = vec2(0.809016994375, -0.587785252292);
    const vec2 k2 = vec2(-0.809016994375, -0.587785252292);
    p.x = abs(p.x);
    p -= 2.0 * max(dot(k1, p), 0.0) * k1;
    p -= 2.0 * max(dot(k2, p), 0.0) * k2;
    p.x = abs(p.x);
    p.y -= r;
    vec2 ba = rf * vec2(-k1.y, k1.x) - vec2(0.0, 1.0);
    float h = clamp(dot(p, ba) / dot(ba, ba), 0.0, r);
    return length(p - ba * h) * sign(p.y * ba.x - p.x * ba.y);
}

// 正 N 边形边界距离（外接圆半径 r），通过极坐标折叠到单个扇区。
float sdPolygon(vec2 p, float r, float n, float phase) {
    float a = atan(p.y, p.x) + phase;
    float sector = FX_TAU / n;
    float local = mod(a + sector * 0.5, sector) - sector * 0.5;
    float apothem = r * cos(FX_PI / n);
    return length(p) * cos(local) - apothem;
}

// 能量细丝波形：锐利核心 + 柔和光边。
float filamentWave(float coord, float width, float feather) {
    float d = abs(fract(coord) - 0.5);
    float core = 1.0 - smoothstep(width, width + feather, d);
    float halo = 1.0 - smoothstep(width * 2.4, width * 2.4 + feather * 2.2, d);
    return max(core, halo * 0.36);
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

vec4 styleGlow(vec2 uv) {
    float r = length(uv);
    float core = exp(-r * r * FxParams.x * 4.0);
    float halo = exp(-r * FxParams.y * 2.0);
    float streak = FxParams.z * exp(-abs(uv.y) * 28.0) * (1.0 - smoothstep(0.0, 1.0, abs(uv.x)));
    float edge = 1.0 - smoothstep(0.7, 1.0, r);
    float intensity = (core * 1.2 + halo * 0.55) * edge + streak * 0.6;
    vec3 col = vertexColor.rgb * intensity + vec3(core * core * 0.8 * edge);
    float alpha = clamp(core * edge + halo * 0.5 * edge, 0.0, 1.0);
    return vec4(col, alpha);
}

vec4 styleRibbon(vec2 uv) {
    float y = abs(uv.y);
    float core = exp(-y * FxParams.x);
    float glow = exp(-y * 2.2) * FxParams.w;
    float flicker = FxParams.y > 0.0 ? 0.7 + 0.6 * fxNoise1(uv.x * FxParams.y - FxTime * FxParams.z) : 1.0;
    float edge = 1.0 - smoothstep(0.82, 1.0, y);
    float intensity = (core + glow) * flicker * edge;
    vec3 col = vertexColor.rgb * intensity + vec3(core * core * core * 0.9 * edge);
    float alpha = clamp((core + glow * 0.6) * edge, 0.0, 1.0);
    return vec4(col, alpha);
}

vec4 styleRune(vec2 uv) {
    float r = length(uv);
    if (r > 1.0) {
        return vec4(0.0);
    }
    float aa = max(gRadialWidth * 1.2, 0.0015);
    float rotation = FxParams.x;
    float seed = FxParams.y;
    float grand = step(0.5, FxParams.z);
    float reveal = FxParams.w;
    float baseAngle = atan(uv.y, uv.x);

    float lines = 0.0;
    float detail = 0.0;
    if (FxParams.z > 1.5) {
        lines += aaLine(r - 0.975, 0.007, aa) + aaLine(r - 0.91, 0.003, aa) + aaLine(r - 0.6, 0.003, aa);
        vec2 hp = rotate2(uv, sin(rotation * 0.5) * 0.15) * 2.1 + vec2(0.0, 0.62);
        lines += aaLine(sdHeart(hp) / 2.1, 0.006, aa) * 1.2;
        detail += (1.0 - smoothstep(-0.02, 0.0, sdHeart(hp) / 2.1)) * 0.12;
        for (int i = 0; i < 6; i++) {
            float a = float(i) * FX_PI / 3.0 + rotation;
            vec2 c = vec2(cos(a), sin(a)) * 0.755;
            vec2 sp = (uv - c) * 9.0 + vec2(0.0, 0.5);
            lines += aaLine(sdHeart(sp) / 9.0, 0.004, aa);
        }
        float a2 = mod(baseAngle - rotation * 0.6 + FX_TAU, FX_TAU);
        float tick = abs(fract(a2 / FX_TAU * 40.0) - 0.5);
        detail += (1.0 - smoothstep(0.1, 0.1 + aa * 25.0, tick)) * step(0.915, r) * step(r, 0.965);
        detail += runeBand(mod(baseAngle + rotation * 0.8 + FX_TAU, FX_TAU), r, 0.42, 0.56, 22.0, seed, aa) * 0.8;
    } else if (grand > 0.5) {
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
    vec3 col = vertexColor.rgb * shape + vec3(clamp(lines, 0.0, 1.0) * 0.35 * visible);
    return vec4(col, clamp(shape, 0.0, 1.0));
}

vec4 styleRing(vec2 uv) {
    float r = length(uv);
    if (r > 1.0) {
        return vec4(0.0);
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
    vec3 col = vertexColor.rgb * ring + vec3(exp(-(d * d) / (width * width * 0.2)) * 0.5);
    return vec4(col, clamp(ring, 0.0, 1.0));
}

vec4 styleBeam(vec2 uv) {
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
    vec3 col = vertexColor.rgb * intensity + vec3(hot * 1.2 * edge);
    return vec4(col, clamp(intensity, 0.0, 1.0));
}

vec4 styleCurtain(vec2 uv) {
    float h = uv.y * 0.5 + 0.5;
    float rise = FxParams.z;
    float topFade = 1.0 - smoothstep(rise - 0.1, rise, h);
    if (topFade <= 0.0) {
        return vec4(0.0);
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
    float vline = (1.0 - smoothstep(0.02, 0.02 + gCurtainWidth * 1.5, gx)) * 0.25 * FxParams.y;
    float ends = 1.0 - smoothstep(0.96, 1.0, abs(x));
    float dissolve = FxParams.w > 0.0 ? smoothstep(FxParams.w - 0.15, FxParams.w + 0.15, fxNoise3(vec3(x * 9.0, h * 6.0, seed + 9.0)) * 0.8 + h * 0.2) : 1.0;
    float intensity = (streak * (1.0 - h * 0.6) + base + crest + scan + vline) * topFade * ends * dissolve;
    vec3 col = mix(vertexColor.rgb, vec3(0.75, 0.6, 1.0), h * 0.7) * intensity;
    return vec4(col, clamp(intensity, 0.0, 1.0));
}

vec4 styleDisk(vec2 uv) {
    float r = length(uv);
    float inner = FxParams.x;
    if (r > 1.0 || r < inner * 0.8) {
        return vec4(0.0);
    }
    vec2 q = rotate2(uv, FxTime * FxParams.y / max(r, 0.15));
    float bands = 0.55 + 0.45 * fxNoise3(vec3(q * 7.0, r * 12.0 + FxParams.z));
    float fine = fxNoise3(vec3(q * 21.0, r * 30.0 + FxParams.z));
    float profile = smoothstep(inner * 0.9, inner * 1.15, r) * pow(1.0 - smoothstep(inner, 1.0, r), 1.5);
    float doppler = 1.0 + FxParams.w * sin(atan(uv.y, uv.x));
    float hot = exp(-(r - inner) * 14.0);
    vec3 inside = vec3(1.0, 0.85, 0.6);
    vec3 tint = mix(inside, vertexColor.rgb, smoothstep(inner, 0.8, r));
    vec3 col = (tint * (bands * 0.8 + fine * 0.4) + vec3(1.0, 0.95, 0.9) * hot * 2.0) * profile * max(doppler, 0.1);
    return vec4(col, clamp(profile, 0.0, 1.0));
}

// 升级版龙鳞能量屏障（配合 sphere + FxGeoCenter 解析平滑法线）：
// 流场扰动能量丝线 + 龙鳞纹 + 双重菲涅尔硬边 + 逐片龟裂点亮，支持 Body（半透明能量膜）与 FX（高光丝线/硬边）双模式。
// params: x = 鳞片/丝线密度, y = 龟裂比例 0..1, z = 闪烁相位, w = 边缘光强度。
vec4 styleShield(vec2 uv) {
    vec3 normal = surfaceNormal();
    vec3 viewDir = normalize(-relativePos);
    float ndv = clamp(abs(dot(normal, viewDir)), 0.0, 1.0);
    float rim = pow(1.0 - ndv, 1.75);
    float hardRim = pow(1.0 - ndv, 4.8);

    float rows = max(FxParams.x, 4.0);
    vec2 flow = sampleFlowOffset(vec2(uv.x * 2.0, uv.y * 1.5), 0.18);
    float cloud = texture(Sampler1, fract(uv * vec2(3.5, 2.2) + vec2(FxTime * 0.06, -FxTime * 0.08))).r;

    // 交织能量丝线
    float latLine = filamentWave((uv.y + flow.y * 0.35) * rows * 1.1, 0.032, 0.024);
    float diagA = filamentWave((uv.x * 1.8 + uv.y * 0.85 + flow.x * 0.25) * rows * 0.65 + FxTime * 0.04, 0.028, 0.024);
    float diagB = filamentWave((uv.x * 1.8 - uv.y * 0.85 - flow.y * 0.25) * rows * 0.65 - FxTime * 0.035, 0.026, 0.022);
    float threads = max(latLine * 0.7, max(diagA, diagB));

    // 错位龙鳞纹 + 龟裂
    vec2 q = vec2(uv.x * rows * 2.0, uv.y * rows);
    float row = floor(q.y);
    q.x += mod(row, 2.0) * 0.5;
    vec2 cell = floor(q);
    vec2 f = fract(q);
    float d = length(vec2(f.x - 0.5, f.y * 1.15)) - 0.62;
    float scaleEdge = exp(-abs(d) * 18.0);
    float h = fxHash12(cell + 7.0);
    float shimmer = 0.5 + 0.5 * sin(FxParams.z * 3.0 + h * 20.0 + uv.y * 12.0);
    float crack = 1.0 - smoothstep(FxParams.y - 0.06, FxParams.y, h);

    if (FxBlendMode > 0.5) {
        // 本体半透明能量膜：深色底膜 + 柔和流云 + 边缘实体轮廓
        float membrane = (0.16 + cloud * 0.18 + rim * 0.45 + threads * 0.14 + scaleEdge * 0.12);
        vec3 baseCol = mix(vertexColor.rgb * 0.28, vertexColor.rgb * 0.72, cloud * 0.5 + rim * 0.5);
        baseCol += mix(vertexColor.rgb, vec3(1.0, 0.9, 1.0), 0.45) * (hardRim * 0.65 + crack * 0.35);
        return vec4(baseCol, clamp(membrane, 0.0, 0.88));
    }

    float energy = rim * (0.55 + FxParams.w * 0.85) + hardRim * (1.2 + FxParams.w * 0.8)
        + threads * (0.28 + cloud * 0.45) * (0.45 + rim)
        + scaleEdge * (0.22 + 0.35 * shimmer) * (0.35 + rim)
        + crack * (0.65 + scaleEdge * 0.7);
    vec3 col = vertexColor.rgb * energy + vec3(hardRim * 0.85 + crack * scaleEdge * 0.45 + threads * hardRim * 0.4);
    return vec4(col, clamp(energy, 0.0, 1.0));
}

vec4 styleSprite(vec2 uv) {
    float shape = FxParams.x;
    float d;
    if (shape < 0.5) {
        d = sdHeart(uv * 0.95 + vec2(0.0, 0.5)) / 0.95;
    } else if (shape < 1.5) {
        vec2 a = abs(uv);
        d = (pow(a.x, 0.5) + pow(a.y, 0.5)) - 0.75;
    } else {
        d = sdStar5(uv * 1.05, 0.8, 0.45) / 1.05;
    }
    float fill = 1.0 - smoothstep(-0.03, 0.02, d);
    float edge = exp(-abs(d) * 28.0);
    float glow = exp(-max(d, 0.0) * 5.0) * 0.45 * (1.0 - smoothstep(0.85, 1.0, length(uv)));
    vec3 col = vertexColor.rgb * (fill * 0.65 + glow) + vec3(edge * 0.9 + fill * 0.15);
    return vec4(col, clamp(fill + glow, 0.0, 1.0));
}

// 9. 3D 圆柱/锥台多层等离子光柱（配合 cylinder）：
// 三速流场贴图扭曲 + 烟沟（暗凹）/烟脊（亮脊）分层 + 轴向冲击波前 + 视角边缘软淡出。
// params: x = 纹理缩放, y = 流动速度（>0 向下灌注，<0 向上喷射）, z = 坍缩/消散进度(0..1), w = 波前位置(0..1)
vec4 stylePlasmaBeam(vec2 uv) {
    vec3 normal = gFaceNormal;
    vec3 viewDir = normalize(-relativePos);
    float ndv = clamp(abs(dot(normal, viewDir)), 0.0, 1.0);
    // 圆柱侧面在掠射角柔和淡出，消除硬几何边界感
    float softEdge = smoothstep(0.05, 0.48, ndv);
    float rim = pow(1.0 - ndv, 2.2) * softEdge;

    float scale = max(floor(FxParams.x + 0.5), 1.0);
    float speed = FxParams.y;
    float collapse = clamp(FxParams.z, 0.0, 1.0);
    float wavePos = FxParams.w;

    vec2 baseUv = vec2(uv.x * 2.0 * scale, uv.y * 2.6 * scale);
    vec2 offset = sampleFlowOffset(vec2(uv.x * scale, uv.y * 2.2 * scale), abs(speed) * 0.6 + 0.4);
    float t = FxTime * speed;
    vec4 s1 = texture(Sampler1, fract(vec2(uv.x * 3.0 * scale + offset.x, uv.y * 3.2 * scale + t * 0.48 + offset.y)));
    vec4 s2 = texture(Sampler1, fract(vec2(uv.x * 5.0 * scale - t * 0.22 - offset.y, uv.y * 6.0 * scale + t * 0.85)));

    float smokeField = clamp(s1.r * 0.62 + s2.g * 0.44 + abs(offset.x) * 0.55, 0.0, 1.0);
    float smokeGroove = smoothstep(0.56, 0.90, smokeField);
    float smokeRidge = smoothstep(0.28, 0.66, smokeField) * (1.0 - smoothstep(0.74, 0.96, smokeField));

    float streakA = smoothstep(0.65, 1.0, 0.5 + 0.5 * sin(uv.y * 20.0 * scale + t * 7.5 + (uv.x + offset.x) * FX_TAU * 3.0));
    float streakB = smoothstep(0.76, 1.0, 0.5 + 0.5 * sin(uv.y * 13.0 * scale + t * 5.2 - (uv.x - offset.y) * FX_TAU * 2.0));
    float streaks = (streakA + streakB * 0.72 + smokeRidge * 0.95) * (1.0 - smokeGroove * 0.55);

    float waveFront = exp(-pow((uv.y - wavePos) * 5.5, 2.0));
    float endFade = smoothstep(0.0, 0.06, uv.y) * (1.0 - smoothstep(0.92, 1.0, uv.y));
    float collapseFade = pow(1.0 - collapse, 1.3 + (1.0 - uv.y) * 0.4);

    if (FxBlendMode > 0.5) {
        // 本体暗烟壳：保留深色暗沟实体感，把光柱的「厚度与威压」立起来
        float bodyAlpha = (0.32 + smokeField * 0.42 + smokeRidge * 0.26) * softEdge * endFade * collapseFade;
        vec3 darkShell = mix(vertexColor.rgb * 0.14, vertexColor.rgb * 0.58, smokeRidge * 0.7 + streaks * 0.3);
        darkShell *= 1.0 - smokeGroove * 0.45;
        return vec4(darkShell, clamp(bodyAlpha, 0.0, 0.92));
    }

    float energy = (0.18 + streaks * 0.62 + rim * 0.48 + waveFront * 0.55) * softEdge * endFade * collapseFade;
    float whiteHot = clamp(pow(ndv, 2.4) * 0.45 + waveFront * 0.35 + smokeRidge * 0.25, 0.0, 1.0);
    vec3 col = mix(vertexColor.rgb, vec3(1.0, 0.96, 0.90), whiteHot * 0.65) * energy;
    col *= 1.0 - smokeGroove * 0.32;
    return vec4(col, clamp(energy, 0.0, 1.0));
}

// 10. 3D 大气再入燃烧锥壳 / 沸腾能量火球（配合 cylinder 锥台 或 sphere）：
// uv.y = 0 为迎风烧灼前端，uv.y = 1 为拖曳火舌尾部。
// params: x = 前端聚焦锐度, y = 噪声密度, z = 流动速度, w = 激波环脉动(0..1)
vec4 styleReentry(vec2 uv) {
    vec3 normal = surfaceNormal();
    vec3 viewDir = normalize(-relativePos);
    float ndv = clamp(abs(dot(normal, viewDir)), 0.0, 1.0);
    float rim = pow(1.0 - ndv, 1.55);

    float front = clamp(1.0 - uv.y, 0.0, 1.0);
    float frontPower = max(FxParams.x, 0.4);
    float nScale = max(floor(FxParams.y + 0.5), 1.0);
    float speed = FxParams.z;
    float pulse = FxParams.w;

    float t = FxTime * speed;
    vec2 flow = sampleFlowOffset(vec2(uv.x * 2.0 * nScale, uv.y * 1.8 * nScale), speed * 0.5);
    float coarse = texture(Sampler1, fract(vec2(uv.x * 2.0 * nScale + flow.x, uv.y * 1.4 * nScale - t * 0.55))).r;
    float tongues = texture(Sampler1, fract(vec2(uv.x * 5.0 * nScale - flow.y, uv.y * 3.0 * nScale - t * 1.15))).g;
    float sharp = texture(Sampler1, fract(vec2(uv.x * 9.0 * nScale, uv.y * 5.2 * nScale - t * 1.75))).g;

    float turb = coarse * 0.46 + tongues * 0.36 + sharp * 0.18;
    float frontMask = pow(front, frontPower);
    float tailFade = smoothstep(0.0, 0.32, front);
    float lick = smoothstep(0.32, 0.88, turb + front * 0.28 + rim * 0.16);
    float shockRing = exp(-pow((front - (0.76 + pulse * 0.08)) * 12.0, 2.0));
    float noseHot = pow(front, 3.0) * (0.72 + turb * 0.48);

    if (FxBlendMode > 0.5) {
        // 本体焦灼暗焰层：给流星/恒星壳体压出高对比暗紫/深绯底层
        float a = clamp((frontMask * (0.28 + lick * 0.65) + rim * tailFade * 0.42), 0.0, 0.90);
        vec3 ember = mix(vertexColor.rgb * 0.12, vertexColor.rgb * 0.55, turb * 0.8 + front * 0.2);
        return vec4(ember, a);
    }

    float energy = frontMask * (0.22 + lick * 0.72 + noseHot * 0.55) + rim * tailFade * (0.35 + lick * 0.75) + shockRing * 0.55;
    vec3 deep = vertexColor.rgb * 0.45;
    vec3 mid = vertexColor.rgb;
    vec3 hot = vec3(1.0, 0.94, 0.78);
    vec3 col = mix(deep, mid, clamp(turb * 1.15 + front * 0.25, 0.0, 1.0));
    col = mix(col, hot, clamp(noseHot * 0.65 + shockRing * 0.45, 0.0, 1.0));
    return vec4(col * energy, clamp(energy, 0.0, 1.0));
}

// 11. 3D 定向曲面刀光弧（配合 slashArc）：
// uv.x 沿斩击弧从尾(0)到头(1)，uv.y 从内缘(0)到外刃(1)。
// Body 模式画出锐利的黑紫/墨黑撕裂实心刀弧，FX 模式叠加高能锋刃与流场残影。
// params: x = 锋刃锐度, y = 流场噪声强度, z = 沿弧推进进度(0..1), w = 尾部撕裂程度(0..1)
vec4 styleSlashArc(vec2 uv) {
    float headReveal = clamp(FxParams.z, 0.0, 1.0);
    if (uv.x > headReveal) {
        return vec4(0.0);
    }
    float tailClip = clamp(FxParams.w, 0.0, 0.95);
    float along = clamp((uv.x - tailClip) / max(headReveal - tailClip, 0.05), 0.0, 1.0);
    if (uv.x < tailClip) {
        return vec4(0.0);
    }

    vec4 n1 = texture(Sampler1, fract(vec2(uv.x * 2.4 - FxTime * 1.8, uv.y * 1.6)));
    vec4 n2 = texture(Sampler1, fract(vec2(uv.x * 6.0 - FxTime * 3.1, uv.y * 3.5 + n1.b * 0.2)));
    float noise = mix(1.0, n1.r * 0.55 + n2.g * 0.55, clamp(FxParams.y, 0.0, 1.0));

    // 刀弧横截面形状：靠近外刃(uv.y ~ 0.82)最锋利，向内缘流场撕裂淡出
    float outerCut = 1.0 - smoothstep(0.86, 1.0, uv.y);
    float innerFade = smoothstep(0.05, 0.55 + (1.0 - along) * 0.35, uv.y + (noise - 0.5) * 0.35);
    float tipFade = smoothstep(0.0, 0.12, along) * (1.0 - smoothstep(0.92, 1.0, along) * 0.35);
    float bladeBody = outerCut * innerFade * tipFade;

    float sharpness = max(FxParams.x, 2.0);
    float edgeLine = exp(-pow(abs(uv.y - 0.84) * sharpness, 2.0)) * tipFade;
    float streak = smoothstep(0.45, 0.92, n2.g) * bladeBody * (0.4 + 0.6 * along);

    if (FxBlendMode > 0.5) {
        // 暗黑空间切口衬底：刀身核心压黑/深紫，形成极强视觉反差
        float bodyMask = clamp(bladeBody * (0.55 + 0.45 * noise), 0.0, 0.94);
        vec3 darkCol = vertexColor.rgb * (0.18 + edgeLine * 0.45);
        return vec4(darkCol, bodyMask);
    }

    float energy = (edgeLine * 1.35 + bladeBody * (0.35 + streak * 0.85)) * (0.45 + 0.55 * along);
    vec3 col = vertexColor.rgb * energy + vec3(edgeLine * edgeLine * 1.15);
    return vec4(col, clamp(energy, 0.0, 1.0));
}

// 12. 3D 喇叭形多层冲击波锥（配合 cylinder 锥台）：
// 三波段气浪沿轴向(uv.y 0..1)高速外推 + 流场噪声撕裂边缘 + 喉部核心爆发。
// params: x = 扩散进度(0..1), y = 速度, z = 边缘撕裂强度, w = 喉部亮核强度
vec4 styleShockCone(vec2 uv) {
    vec3 normal = gFaceNormal;
    vec3 viewDir = normalize(-relativePos);
    float ndv = clamp(abs(dot(normal, viewDir)), 0.0, 1.0);
    float softEdge = smoothstep(0.03, 0.35, ndv);

    float progress = clamp(FxParams.x, 0.0, 1.0);
    float speed = FxParams.y;
    float tear = FxParams.z;
    float throat = FxParams.w;

    float nA = texture(Sampler1, fract(vec2(uv.x * 4.0 + FxTime * 0.35, uv.y * 2.2 - FxTime * speed * 0.5))).r;
    float nB = texture(Sampler1, fract(vec2(uv.x * 8.0 - FxTime * 0.5, uv.y * 4.0 - FxTime * speed * 0.85))).g;
    float shiftedY = fract(uv.y - FxTime * speed * 0.45 + (nA - 0.5) * 0.16 * tear);

    float w1 = 1.0 - smoothstep(0.03, 0.13, abs(shiftedY - 0.22));
    float w2 = (1.0 - smoothstep(0.03, 0.12, abs(fract(shiftedY + 0.34) - 0.22))) * 0.78;
    float w3 = (1.0 - smoothstep(0.02, 0.10, abs(fract(shiftedY + 0.67) - 0.22))) * 0.52;
    float bands = (w1 + w2 + w3) * (0.55 + 0.65 * nB);

    float frontWave = exp(-pow((uv.y - progress) * 7.5, 2.0));
    float endFade = smoothstep(0.0, 0.08, uv.y) * (1.0 - smoothstep(0.75, 1.0, uv.y));
    float throatGlow = exp(-uv.y * 6.0) * throat;

    float energy = (bands * 0.72 + frontWave * 1.15 + throatGlow * 0.85) * endFade * softEdge;
    if (FxBlendMode > 0.5) {
        return vec4(vertexColor.rgb * 0.25, clamp(energy * 0.65, 0.0, 0.82));
    }
    vec3 col = mix(vertexColor.rgb, vec3(1.0, 0.95, 0.88), clamp(frontWave * 0.55 + throatGlow * 0.45, 0.0, 1.0)) * energy;
    return vec4(col, clamp(energy, 0.0, 1.0));
}

// 13. 360° 三平面无缝沸腾日冕 / 日蚀球壳（配合 deformedSphere + geoCenter）：
// 从任意俯仰视角（包括地面仰视）看都完美无极点拉伸与接缝，支持暗黑日蚀内核与金紫耀斑外层。
// params: x = 边缘日冕锐度, y = 湍流对比度, z = 流动速度, w = 脉动强度
vec4 styleCorona(vec2 uv) {
    vec3 normal = surfaceNormal();
    vec3 viewDir = normalize(-relativePos);
    float ndv = clamp(abs(dot(normal, viewDir)), 0.0, 1.0);
    float rimPower = max(FxParams.x, 1.0);
    float contrast = max(FxParams.y, 0.5);
    float t = FxTime * FxParams.z;
    float pulse = FxParams.w;

    // 3D 三平面采样保证 360° 零接缝、零极点挤压
    vec3 w = pow(abs(normal), vec3(3.0));
    w /= max(w.x + w.y + w.z, 1.0e-4);
    vec4 sXY = texture(Sampler1, fract(normal.xy * 0.85 + vec2(t * 0.16, -t * 0.11)));
    vec4 sYZ = texture(Sampler1, fract(normal.yz * 0.85 + vec2(-t * 0.13, t * 0.19)));
    vec4 sXZ = texture(Sampler1, fract(normal.xz * 0.85 + vec2(t * 0.11, t * 0.15)));
    vec4 baseSample = sXY * w.z + sYZ * w.x + sXZ * w.y;

    vec4 fXY = texture(Sampler1, fract(normal.xy * 1.9 + (baseSample.ba - 0.5) * 0.32 - vec2(t * 0.24, t * 0.18)));
    vec4 fYZ = texture(Sampler1, fract(normal.yz * 1.9 + (baseSample.ba - 0.5) * 0.32 + vec2(t * 0.20, -t * 0.22)));
    vec4 fXZ = texture(Sampler1, fract(normal.xz * 1.9 + (baseSample.ba - 0.5) * 0.32 - vec2(-t * 0.17, t * 0.26)));
    vec4 fineSample = fXY * w.z + fYZ * w.x + fXZ * w.y;

    float plasma = clamp(baseSample.r * 0.55 + fineSample.g * 0.55, 0.0, 1.0);
    float veins = smoothstep(0.45, 0.92, fineSample.g);
    float rim = pow(1.0 - ndv, rimPower);
    float hardRim = pow(1.0 - ndv, rimPower * 2.4);

    if (FxBlendMode > 0.5) {
        // 实心日蚀内核：中心极深黑紫压暗背景，表面浮现微弱沸腾裂纹与深渊边缘
        float alpha = clamp(0.84 + (1.0 - plasma) * 0.14 + rim * 0.20, 0.0, 0.98);
        vec3 darkCore = vertexColor.rgb * (0.20 + veins * 0.45 * pulse + rim * 0.55);
        return vec4(darkCore, alpha);
    }

    float energy = rim * (0.65 + plasma * contrast * 0.85) + hardRim * (1.2 + pulse * 0.8) + veins * (0.25 + 0.45 * pulse) * (0.35 + rim);
    vec3 hotEdge = vec3(1.0, 0.95, 0.84);
    vec3 col = mix(vertexColor.rgb, hotEdge, clamp(hardRim * 0.65 + veins * pulse * 0.35, 0.0, 1.0)) * energy;
    return vec4(col, clamp(energy, 0.0, 1.0));
}

// 断空全息残影：模型逐面绘制（rawQuad，uv 为每个面的 [-1,1]），棱边发光 + 菲涅尔轮廓 + 流动扫描线；
// w 为溶解进度，按流场噪声逐片烧蚀并在烧蚀前沿留下白热边。
// 本体层输出半透明暗色剪影，让残影在明亮天空下依然有体积。
vec4 styleAfterimage(vec2 uv) {
    vec2 a = abs(uv);
    float edge = max(a.x, a.y);
    float lineWidth = max(gRadialWidth * 2.5, 0.05) * max(FxParams.x, 0.1);
    float edgeLine = smoothstep(1.0 - lineWidth * 2.2, 1.0 - lineWidth * 0.3, edge);

    vec3 viewDir = normalize(-relativePos);
    float ndv = clamp(abs(dot(gFaceNormal, viewDir)), 0.0, 1.0);
    float rim = pow(1.0 - ndv, 2.2);

    float scan = 0.5 + 0.5 * sin(relativePos.y * FxParams.z * 6.2831 - FxTime * 9.0);
    scan = smoothstep(0.55, 1.0, scan);

    float noise = texture(Sampler1, fract(uv * 0.23 + vec2(relativePos.x + relativePos.z, relativePos.y) * 0.17)).g;
    float dissolve = FxParams.w;
    float alive = smoothstep(dissolve - 0.02, dissolve + 0.02, noise);
    if (alive <= 0.001) {
        return vec4(0.0);
    }
    float burn = dissolve > 0.001 ? (1.0 - smoothstep(dissolve, dissolve + 0.12, noise)) : 0.0;

    if (FxBlendMode > 0.5) {
        float fill = (0.22 + rim * 0.3) * alive;
        return vec4(vertexColor.rgb * 0.25, fill);
    }
    float intensity = edgeLine * 1.5 + rim * 0.8 + FxParams.y + scan * 0.22;
    vec3 col = vertexColor.rgb * intensity * alive;
    col += vec3(1.0) * (edgeLine * edgeLine * 0.55 + burn * 2.2) * alive;
    return vec4(col, clamp(intensity * alive, 0.0, 1.0));
}

// 虚空裂口半宽轮廓（uv.y 沿裂口方向 [-1,1]，返回 uv.x 方向半宽）。
float slitHalfWidth(float y, float open, float jag, float variant) {
    float ay = abs(y);
    float profile = variant < 0.5 ? pow(max(1.0 - ay * ay, 0.0), 0.75) : (1.0 - smoothstep(0.82, 1.0, ay));
    float ridge = texture(Sampler1, fract(vec2(0.13 + variant * 0.37, y * 0.45 + FxTime * 0.07))).g;
    float fine = fxNoise1(y * 23.0 + FxTime * 3.0);
    float tear = (ridge - 0.5) * 0.7 + (fine - 0.5) * 0.3;
    return max(0.0, 0.5 * open * profile + tear * jag * 0.18 * open * sqrt(profile));
}

// 虚空裂口（quad：uv.x 横跨裂口，uv.y 沿裂口方向）。
// params: x=张开度, y=边缘撕裂强度, z=变体(0 眼形伤口 / 1 直刃裂缝), w=内部细节（眼形：因果之眼睁开程度；直刃：星点密度）
// 本体层画深紫→纯黑的暗腔；发光层画锯齿刃口、内侧白热边、外侧晕光、内部暗星与因果之眼。
vec4 styleVoidSlit(vec2 uv) {
    float open = FxParams.x;
    float variant = FxParams.z;
    float w = slitHalfWidth(uv.y, open, FxParams.y, variant);
    float d = abs(uv.x) - w;
    float aa = max(gRadialWidth * 1.5, 0.004);
    float inside = 1.0 - smoothstep(-aa, aa, d);
    float endFade = 1.0 - smoothstep(0.9, 1.0, abs(uv.y));

    if (FxBlendMode > 0.5) {
        float depth01 = w > 1.0e-4 ? clamp(1.0 - abs(uv.x) / w, 0.0, 1.0) : 0.0;
        vec3 cavity = mix(vec3(0.16, 0.02, 0.2), vec3(0.008, 0.0, 0.02), sqrt(depth01));
        return vec4(cavity, inside * 0.97 * endFade);
    }

    float rimWidth = 0.022 + 0.02 * FxParams.y;
    float rim = exp(-abs(d) / rimWidth);
    float innerHot = exp(-max(-d, 0.0) / 0.012) * inside;
    float halo = exp(-max(d, 0.0) / 0.18) * (1.0 - inside) * 0.35;
    vec3 col = vertexColor.rgb * (rim * 1.4 + halo) + vec3(1.0, 0.75, 0.95) * innerHot * 0.9;
    float alpha = rim + halo + innerHot;

    if (inside > 0.0) {
        vec2 grid = vec2(uv.x * 14.0, uv.y * 9.0 + FxTime * 0.15);
        vec2 cell = floor(grid);
        vec2 f = fract(grid) - 0.5;
        float h = fxHash12(cell);
        float threshold = variant < 0.5 ? 0.82 : mix(0.95, 0.72, clamp(FxParams.w, 0.0, 1.0));
        float star = step(threshold, h) * exp(-dot(f, f) * 60.0) * (0.5 + 0.5 * sin(FxTime * 5.0 + h * 40.0));
        col += vec3(0.9, 0.6, 1.0) * star * inside * 0.8;
        alpha += star * inside * 0.5;
        if (variant < 0.5 && FxParams.w > 0.0) {
            float eye = FxParams.w;
            vec2 e = vec2(uv.x / max(w, 0.05), uv.y / 0.38);
            float r = length(e);
            float iris = exp(-abs(r - 0.62 * eye) / 0.05) * eye;
            float pupil = (1.0 - smoothstep(0.035 * eye, 0.055 * eye, abs(uv.x))) * (1.0 - smoothstep(0.24 * eye, 0.3 * eye, abs(uv.y)));
            vec3 gold = vec3(1.0, 0.76, 0.36);
            col += gold * (iris * 1.2 + pupil * 2.0) * inside;
            alpha += (iris + pupil) * inside;
        }
    }
    return vec4(col * endFade, clamp(alpha * endFade, 0.0, 1.0));
}

void main() {
    gFaceNormal = normalize(cross(dFdx(relativePos), dFdy(relativePos)) + vec3(0.0, 1.0e-7, 0.0));
    gRadialWidth = fwidth(length(texCoord0));
    gCurtainWidth = fwidth(texCoord0.x * 16.0);
    float mask = depthMask() * occluderMask();
    if (mask <= 0.0 || vertexColor.a <= 0.0) {
        discard;
    }
    vec4 res;
    if (FxStyle < 0.5) {
        res = styleGlow(texCoord0);
    } else if (FxStyle < 1.5) {
        res = styleRibbon(texCoord0);
    } else if (FxStyle < 2.5) {
        res = styleRune(texCoord0);
    } else if (FxStyle < 3.5) {
        res = styleRing(texCoord0);
    } else if (FxStyle < 4.5) {
        res = styleBeam(texCoord0);
    } else if (FxStyle < 5.5) {
        res = styleCurtain(texCoord0);
    } else if (FxStyle < 6.5) {
        res = styleDisk(texCoord0);
    } else if (FxStyle < 7.5) {
        res = styleShield(texCoord0);
    } else if (FxStyle < 8.5) {
        res = styleSprite(texCoord0);
    } else if (FxStyle < 9.5) {
        res = stylePlasmaBeam(texCoord0);
    } else if (FxStyle < 10.5) {
        res = styleReentry(texCoord0);
    } else if (FxStyle < 11.5) {
        res = styleSlashArc(texCoord0);
    } else if (FxStyle < 12.5) {
        res = styleShockCone(texCoord0);
    } else if (FxStyle < 13.5) {
        res = styleCorona(texCoord0);
    } else if (FxStyle < 14.5) {
        res = styleAfterimage(texCoord0);
    } else {
        res = styleVoidSlit(texCoord0);
    }

    if (FxBlendMode > 0.5) {
        float outAlpha = clamp(res.a * vertexColor.a * mask, 0.0, 1.0);
        if (outAlpha <= 0.002) {
            discard;
        }
        // 预乘 Alpha 输出：支持纯黑/暗紫暗部层 + 正常半透明烟壳叠加
        fragColor = vec4(max(res.rgb, vec3(0.0)) * FxEmission * outAlpha, outAlpha);
    } else {
        fragColor = vec4(max(res.rgb, vec3(0.0)) * vertexColor.a * FxEmission * mask, 1.0);
    }
}
