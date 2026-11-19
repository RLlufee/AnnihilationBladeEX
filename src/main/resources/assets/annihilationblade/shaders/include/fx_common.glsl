// AnnihilationBlade FX 公共函数库：哈希、噪声、3D Voronoi、深度重建、射线-球求交。

#define FX_PI 3.14159265
#define FX_TAU 6.28318531

float fxHash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float fxHash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float fxHash13(vec3 p3) {
    p3 = fract(p3 * 0.1031);
    p3 += dot(p3, p3.zyx + 31.32);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 fxHash33(vec3 p3) {
    p3 = fract(p3 * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yxz + 33.33);
    return fract((p3.xxy + p3.yxx) * p3.zyx);
}

float fxNoise1(float x) {
    float i = floor(x);
    float f = fract(x);
    float u = f * f * (3.0 - 2.0 * f);
    return mix(fxHash11(i), fxHash11(i + 1.0), u);
}

float fxNoise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    float n000 = fxHash13(i);
    float n100 = fxHash13(i + vec3(1.0, 0.0, 0.0));
    float n010 = fxHash13(i + vec3(0.0, 1.0, 0.0));
    float n110 = fxHash13(i + vec3(1.0, 1.0, 0.0));
    float n001 = fxHash13(i + vec3(0.0, 0.0, 1.0));
    float n101 = fxHash13(i + vec3(1.0, 0.0, 1.0));
    float n011 = fxHash13(i + vec3(0.0, 1.0, 1.0));
    float n111 = fxHash13(i + vec3(1.0, 1.0, 1.0));
    return mix(mix(mix(n000, n100, u.x), mix(n010, n110, u.x), u.y), mix(mix(n001, n101, u.x), mix(n011, n111, u.x), u.y), u.z);
}

float fxFbm3(vec3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += amplitude * fxNoise3(p);
        p = p * 2.03 + vec3(1.7, 9.2, 3.1);
        amplitude *= 0.5;
    }
    return value;
}

// 3D Voronoi（27 邻域，精确）。返回 vec3(F1, F2-F1 近似边界距离, 单元哈希)，cellId 输出单元整数坐标。
// 单元特征点抖动范围限制在 [0.15, 0.85]，与 fxVoronoi3Fast 保持同一套单元划分。
vec3 fxVoronoi3(vec3 p, out vec3 cellId) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    float f1 = 8.0;
    float f2 = 8.0;
    vec3 best = i;
    for (int z = -1; z <= 1; z++) {
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                vec3 g = vec3(float(x), float(y), float(z));
                vec3 o = 0.15 + 0.7 * fxHash33(i + g);
                vec3 r = g + o - f;
                float d = dot(r, r);
                if (d < f1) {
                    f2 = f1;
                    f1 = d;
                    best = i + g;
                } else if (d < f2) {
                    f2 = d;
                }
            }
        }
    }
    cellId = best;
    float d1 = sqrt(f1);
    return vec3(d1, sqrt(f2) - d1, fxHash13(best));
}

// 3D Voronoi（2x2x2 近邻，近似）。用于体积光线步进的大量采样，代价约为精确版的 30%。
vec3 fxVoronoi3Fast(vec3 p, out vec3 cellId) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 s = step(0.5, f) - 1.0;
    float f1 = 8.0;
    float f2 = 8.0;
    vec3 best = i;
    for (int z = 0; z <= 1; z++) {
        for (int y = 0; y <= 1; y++) {
            for (int x = 0; x <= 1; x++) {
                vec3 g = s + vec3(float(x), float(y), float(z));
                vec3 o = 0.15 + 0.7 * fxHash33(i + g);
                vec3 r = g + o - f;
                float d = dot(r, r);
                if (d < f1) {
                    f2 = f1;
                    f1 = d;
                    best = i + g;
                } else if (d < f2) {
                    f2 = d;
                }
            }
        }
    }
    cellId = best;
    float d1 = sqrt(f1);
    return vec3(d1, sqrt(f2) - d1, fxHash13(best));
}

// 屏幕 UV → 相对相机的世界空间视线方向（InvView 为纯旋转的逆）。
vec3 fxViewRay(vec2 uv, mat4 invProj, mat4 invView) {
    vec4 v = invProj * vec4(uv * 2.0 - 1.0, 1.0, 1.0);
    v.xyz /= v.w;
    return normalize((invView * vec4(v.xyz, 0.0)).xyz);
}

// 原始深度 → 相机到场景表面的欧氏距离；天空返回极大值（兼容光影包 0.9999 深度）。
float fxSceneDistance(float depth, vec2 uv, mat4 invProj) {
    if (depth >= 0.9999) {
        return 1.0e6;
    }
    vec4 v = invProj * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return length(v.xyz / v.w);
}

// Interleaved Gradient Noise：用于体积光线步进抖动，比白噪声噪点细腻一个量级。
float fxIGN(vec2 pixel, float time) {
    vec2 p = pixel + vec2(fract(time * 13.37) * 59.0, fract(time * 7.19) * 37.0);
    return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715))));
}

// 8x8 Bayer 有序抖动：消除 8-bit 输出在大面积渐变处的色带断层。
float fxBayer2(vec2 p) {
    p = floor(p);
    return fract(p.x * 0.5 + p.y * p.y * 0.75);
}

float fxBayer4(vec2 p) {
    return fxBayer2(p * 0.5) * 0.25 + fxBayer2(p);
}

float fxBayer8(vec2 p) {
    return fxBayer4(p * 0.5) * 0.25 + fxBayer2(p);
}

vec3 fxDither8(vec3 color, vec2 fragCoord) {
    float threshold = fxBayer8(fragCoord) - 0.5;
    return clamp(color + vec3(threshold / 255.0), vec3(0.0), vec3(1.0));
}

// 带前景深度防穿帮校验的折射偏移：避免近处前景方块被吸进远处黑洞或冲击波。
vec2 fxSafeRefractUv(vec2 baseUv, vec2 offset, float effectT, sampler2D depthTex, mat4 invProj) {
    if (dot(offset, offset) < 1.0e-8) {
        return baseUv;
    }
    vec2 target = clamp(baseUv + offset, vec2(0.001), vec2(0.999));
    float targetDepth = texture(depthTex, target).r;
    float targetT = fxSceneDistance(targetDepth, target, invProj);
    if (targetT < effectT - 0.5) {
        return baseUv;
    }
    return target;
}

// 射线（起点为相机原点）与球求交，返回 (tNear, tFar)；未命中返回 (-1, -1)。
vec2 fxSphere(vec3 rd, vec3 center, float radius) {
    float b = dot(center, rd);
    float c = dot(center, center) - radius * radius;
    float h = b * b - c;
    if (h < 0.0) {
        return vec2(-1.0);
    }
    h = sqrt(h);
    return vec2(b - h, b + h);
}
