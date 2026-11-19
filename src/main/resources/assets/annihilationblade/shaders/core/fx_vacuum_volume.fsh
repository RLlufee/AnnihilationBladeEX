#version 150

#moj_import <annihilationblade:fx_common.glsl>
#moj_import <annihilationblade:fx_space.glsl>

// 真空衰变 · 体积通道（HDR，加法累积）：
//  · 真空泡壁：薄壳按掠射角增亮（limb brightening），带流动的虹彩色；
//  · 结界内的稀薄虚空雾，被奇点照亮；
//  · 地表网格的柔和辉光（与场景通道的锐利网格叠加形成泛光）；
//  · 奇点光子环与外晕、终结闪光与冲击波光边。
uniform sampler2D Sampler1; // 场景原始深度
uniform mat4 InvProj;
uniform mat4 InvView;
uniform vec4 Zone;
uniform vec4 ZoneExtra;
uniform vec4 Bubble;
uniform vec4 Core;
uniform vec4 Shock;
uniform float FxTime;
uniform float Steps;

in vec2 texCoord;

out vec4 fragColor;

float zoneMask(vec3 local, float bubbleRadius) {
    float halfSize = Zone.w;
    float ex = 1.0 - smoothstep(halfSize - 2.0, halfSize, abs(local.x));
    float ez = 1.0 - smoothstep(halfSize - 2.0, halfSize, abs(local.z));
    float ey = smoothstep(-ZoneExtra.y - 2.0, -ZoneExtra.y, local.y) * (1.0 - smoothstep(ZoneExtra.x - 2.0, ZoneExtra.x, local.y));
    float eb = 1.0 - smoothstep(bubbleRadius - 4.0, bubbleRadius, length(local));
    return ex * ez * ey * eb;
}

vec3 iridescence(float t) {
    return 0.5 + 0.5 * cos(FX_TAU * (vec3(0.0, 0.33, 0.67) + t));
}

void main() {
    vec2 uv = texCoord;
    float depth = texture(Sampler1, uv).r;
    vec3 rd = fxViewRay(uv, InvProj, InvView);
    float sceneT = fxSceneDistance(depth, uv, InvProj);
    float strength = ZoneExtra.z * (1.0 - Bubble.w);
    vec3 zc = Zone.xyz;
    vec3 acc = vec3(0.0);
    // 导数必须在一致控制流中求取，因此在任何分支之前计算表面点与法线
    vec3 surface = rd * min(sceneT, 4096.0);
    vec3 surfaceNormal = normalize(cross(dFdx(surface), dFdy(surface)) + vec3(0.0, 1.0e-6, 0.0));
    float glowMajor = fxSurfaceGrid(surface - Zone.xyz, surfaceNormal, 16.0, 0.06);
    float glowMinor = fxSurfaceGrid(surface - Zone.xyz, surfaceNormal, 4.0, 0.08);

    // 1. 真空泡壁
    if (Bubble.y > 0.0) {
        vec2 hit = fxSphere(rd, zc, Bubble.x);
        for (int i = 0; i < 2; i++) {
            float t = i == 0 ? hit.x : hit.y;
            if (t > 0.0 && t < sceneT) {
                vec3 local = rd * t - zc;
                vec3 n = local / max(Bubble.x, 1.0e-3);
                float limb = min(Bubble.z / max(abs(dot(n, rd)), 0.06), 14.0);
                float band = atan(local.z, local.x) * 0.48 + local.y * 0.02 + FxTime * 0.6;
                vec3 tint = mix(vec3(0.4, 0.85, 1.0), iridescence(band), 0.55);
                float boxFade = 1.0 - smoothstep(Zone.w * 1.05, Zone.w * 1.4, max(abs(local.x), abs(local.z)));
                acc += tint * limb * Bubble.y * 0.3 * (0.4 + 0.6 * boxFade);
            }
        }
    }

    // 2. 结界内的虚空雾
    if (strength > 0.0) {
        vec3 boxMin = zc - vec3(Zone.w, ZoneExtra.y, Zone.w);
        vec3 boxMax = zc + vec3(Zone.w, ZoneExtra.x, Zone.w);
        vec2 box = fxBox(rd, boxMin, boxMax);
        float t0 = max(box.x, 0.0);
        float t1 = min(box.y, min(sceneT, 400.0));
        if (t0 < t1) {
            int steps = int(clamp(Steps, 4.0, 48.0));
            float dt = (t1 - t0) / float(steps);
            float jitter = fxHash12(gl_FragCoord.xy + fract(FxTime * 7.0) * 100.0);
            vec3 fog = vec3(0.0);
            for (int i = 0; i < 48; i++) {
                if (i >= steps) {
                    break;
                }
                float t = t0 + (float(i) + jitter) * dt;
                vec3 p = rd * t;
                vec3 local = p - zc;
                float m = zoneMask(local, Bubble.x);
                if (m <= 0.001) {
                    continue;
                }
                float n = fxNoise3(local * 0.045 + vec3(0.0, -FxTime * 0.4, FxTime * 0.15)) * 0.7 + fxNoise3(local * 0.11 + FxTime * 0.3) * 0.3;
                float density = smoothstep(0.45, 0.85, n);
                float dc = length(p - Core.xyz);
                float light = 1.0 / (1.0 + dc * dc * 0.004);
                fog += mix(vec3(0.3, 0.15, 0.6), vec3(0.4, 0.85, 1.0), light) * density * (0.25 + light * 1.5) * m;
            }
            acc += fog * dt * 0.02 * strength;
        }

        // 3. 地表网格辉光
        if (depth < 1.0) {
            vec3 local = surface - zc;
            float field = zoneMask(local, Bubble.x) * strength;
            if (field > 0.0) {
                float wave = fract(length(local.xz) / 22.0 - FxTime * 0.45);
                float pulse = pow(wave, 10.0);
                acc += vec3(0.3, 0.75, 1.0) * (glowMajor * 0.35 + glowMinor * pulse * 0.6) * field;
            }
        }
    }

    // 4. 奇点：光子环、外晕、闪光
    vec3 core = Core.xyz;
    float tc = dot(core, rd);
    float dCore = length(core - rd * tc);
    if (Core.w > 0.0 && tc > 0.0 && tc - Core.w < sceneT) {
        float ring = exp(-abs(dCore - Core.w * 1.35) / (Core.w * 0.18));
        acc += vec3(0.78, 0.5, 0.95) * ring * 2.5;
        acc += vec3(0.45, 0.35, 1.0) * exp(-max(dCore - Core.w, 0.0) / (Core.w * 1.8)) * step(Core.w, dCore) * 0.7;
    }
    // 湮灭闪光半径与冲击波厚度挂钩（Shock.z × 8），随结界尺寸缩放
    float flashRadius = max(Shock.z, 0.5) * 8.0;
    if (Shock.w > 0.0 && tc > 0.0 && tc - flashRadius < sceneT) {
        acc += vec3(0.85, 0.95, 1.0) * exp(-pow(dCore / flashRadius, 2.0)) * Shock.w * 3.5;
    }

    // 5. 冲击波光边
    if (Shock.y > 0.0 && tc > 0.0) {
        float frontT = tc - sqrt(max(Shock.x * Shock.x - dCore * dCore, 0.0));
        if (frontT < sceneT) {
            acc += vec3(0.6, 0.85, 1.0) * exp(-pow((dCore - Shock.x) / max(Shock.z, 0.1), 2.0)) * Shock.y * 1.6;
        }
    }

    fragColor = vec4(acc, 1.0);
}
