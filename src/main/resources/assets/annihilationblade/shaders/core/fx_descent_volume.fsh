#version 150

#moj_import <annihilationblade:fx_common.glsl>
#moj_import <annihilationblade:fx_rift.glsl>

// 神陨·宇宙夜陨降临 · 体积通道（HDR，加法累积）：
//  · 裂隙金边与向外溢出的辉光；
//  · 暗星周围的旋臂尘埃盘（对数螺线 + 噪声，内金外紫）；
//  · 光子环与外晕、坍缩闪光、冲击波光边。
uniform sampler2D Sampler1; // 场景原始深度
uniform mat4 InvProj;
uniform mat4 InvView;
uniform vec4 Star;
uniform vec4 Rift;
uniform vec4 RiftShape;
uniform vec4 Vortex;
uniform vec4 Shock;
uniform float FxTime;
uniform float Steps;

in vec2 texCoord;

out vec4 fragColor;

const vec3 GOLD = vec3(1.0, 0.72, 0.28);
const vec3 VIOLET = vec3(0.62, 0.18, 1.0);

void main() {
    vec2 uv = texCoord;
    float depth = texture(Sampler1, uv).r;
    vec3 rd = fxViewRay(uv, InvProj, InvView);
    float sceneT = fxSceneDistance(depth, uv, InvProj);
    vec3 acc = vec3(0.0);

    // 1. 裂隙边缘
    vec4 rift = fxRift(rd, sceneT, Rift, RiftShape, FxTime);
    if (rift.w > 0.0) {
        float rim = exp(-abs(rift.y) / 0.45);
        float spill = exp(-max(-rift.y, 0.0) / 6.0) * step(rift.y, 0.0);
        float inner = smoothstep(0.0, RiftShape.w, rift.y) * 0.12;
        float flicker = 0.8 + 0.2 * fxNoise1(rift.z * 0.5 + FxTime * 6.0);
        acc += (mix(GOLD, vec3(1.0, 0.95, 0.85), 0.4) * rim * 2.2 + VIOLET * spill * 0.35 + VIOLET * inner) * Rift.w * flicker;
    }

    // 2. 暗星旋臂尘埃盘
    vec3 star = Star.xyz;
    float radius = Vortex.y;
    if (Vortex.x > 0.0 && radius > 0.0) {
        float thickness = max(radius * 0.08, 1.5);
        vec2 hit = fxSphere(rd, star, radius);
        float t0 = max(hit.x, 0.0);
        float t1 = min(hit.y, sceneT);
        if (hit.y > 0.0 && t0 < t1) {
            int steps = int(clamp(Steps * 0.75, 4.0, 36.0));
            float dt = (t1 - t0) / float(steps);
            float jitter = fxIGN(gl_FragCoord.xy, FxTime);
            vec3 dust = vec3(0.0);
            for (int i = 0; i < 36; i++) {
                if (i >= steps) {
                    break;
                }
                vec3 p = rd * (t0 + (float(i) + jitter) * dt) - star;
                float r = length(p.xz);
                float vertical = exp(-abs(p.y) / thickness);
                if (vertical < 0.02 || r > radius) {
                    continue;
                }
                float angle = atan(p.z, p.x);
                float arms = 0.5 + 0.5 * sin(angle * 3.0 + log(r + 1.0) * 6.0 - FxTime * Vortex.z);
                arms = smoothstep(0.35, 1.0, arms);
                float noise = 0.6 + 0.4 * fxNoise3(p * 0.25 + vec3(0.0, FxTime * 0.5, 0.0));
                float radial = (1.0 - smoothstep(radius * 0.6, radius, r)) * smoothstep(Star.w * 1.2, Star.w * 2.6, r);
                float k = clamp(r / radius, 0.0, 1.0);
                dust += mix(GOLD, VIOLET, k) * arms * noise * vertical * radial * (1.0 + 2.0 * (1.0 - k));
            }
            acc += dust * dt * 0.036 * Vortex.x;
        }
    }

    // 3. 光子环、外晕、闪光
    float tc = dot(star, rd);
    float dStar = length(star - rd * tc);
    if (Star.w > 0.0 && tc > 0.0 && tc - Star.w < sceneT) {
        float ring = exp(-abs(dStar - Star.w * 1.35) / (Star.w * 0.18));
        acc += mix(GOLD, vec3(1.0), 0.3) * ring * 2.4;
        acc += VIOLET * exp(-max(dStar - Star.w, 0.0) / (Star.w * 1.6)) * step(Star.w, dStar) * 0.8;
    }
    float flashRadius = max(Shock.z, 0.5) * 8.0;
    if (Shock.w > 0.0 && tc > 0.0 && tc - flashRadius < sceneT) {
        acc += vec3(1.0, 0.9, 0.75) * exp(-pow(dStar / flashRadius, 2.0)) * Shock.w * 3.5;
    }

    // 4. 冲击波光边
    if (Shock.y > 0.0 && tc > 0.0) {
        float frontT = tc - sqrt(max(Shock.x * Shock.x - dStar * dStar, 0.0));
        if (frontT < sceneT) {
            acc += mix(GOLD, VIOLET, 0.35) * exp(-pow((dStar - Shock.x) / max(Shock.z, 0.1), 2.0)) * Shock.y * 1.8;
        }
    }

    fragColor = vec4(acc, 1.0);
}
