#version 150

#moj_import <annihilationblade:fx_common.glsl>
#moj_import <annihilationblade:fx_space.glsl>
#moj_import <annihilationblade:fx_blood.glsl>

// 血狱领域 · 体积通道（HDR，加法累积）：贴地流动的血雾、血脉辉光、脉冲光环、血月光晕。
uniform sampler2D Sampler1; // 场景原始深度
uniform mat4 InvProj;
uniform mat4 InvView;
uniform vec4 Zone;
uniform vec4 ZoneExtra;
uniform float FxTime;
uniform float Steps;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    float depth = texture(Sampler1, uv).r;
    vec3 rd = fxViewRay(uv, InvProj, InvView);
    float sceneT = fxSceneDistance(depth, uv, InvProj);
    float strength = ZoneExtra.y;
    vec3 acc = vec3(0.0);

    if (strength > 0.0) {
        // 1. 贴地血雾
        vec3 boxMin = Zone.xyz - vec3(Zone.w, 2.0, Zone.w);
        vec3 boxMax = Zone.xyz + vec3(Zone.w, ZoneExtra.x, Zone.w);
        vec2 box = fxBox(rd, boxMin, boxMax);
        float t0 = max(box.x, 0.0);
        float t1 = min(box.y, sceneT);
        if (t0 < t1) {
            int steps = int(clamp(Steps * 0.6, 4.0, 24.0));
            float dt = (t1 - t0) / float(steps);
            float jitter = fxHash12(gl_FragCoord.xy + fract(FxTime * 7.0) * 100.0);
            vec3 mist = vec3(0.0);
            for (int i = 0; i < 24; i++) {
                if (i >= steps) {
                    break;
                }
                vec3 local = rd * (t0 + (float(i) + jitter) * dt) - Zone.xyz;
                float m = fxBloodMask(local, Zone, ZoneExtra);
                if (m <= 0.001) {
                    continue;
                }
                float ground = exp(-max(local.y, 0.0) / 1.6);
                float n = fxNoise3(local * 0.35 + vec3(FxTime * 0.4, -FxTime * 0.15, 0.0)) * 0.7 + fxNoise3(local * 0.9 - FxTime * 0.6) * 0.3;
                mist += mix(BLOOD_DEEP, BLOOD_BRIGHT, n) * smoothstep(0.3, 0.8, n) * ground * m;
            }
            acc += mist * dt * 0.09 * strength;
        }

        // 2. 地表血脉辉光与脉冲光环
        if (depth < 1.0) {
            vec3 local = rd * sceneT - Zone.xyz;
            float field = fxBloodMask(local, Zone, ZoneExtra) * strength;
            if (field > 0.0) {
                float veins = fxBloodVeins(local, ZoneExtra.z, 0.16);
                float beat = 0.55 + 0.45 * sin(FxTime * 5.0 - length(local.xz) * 0.6);
                float pulse = fxBloodPulse(local, ZoneExtra.w);
                acc += BLOOD_BRIGHT * (veins * (0.25 * beat + pulse * 1.2) + pulse * 0.35) * field;
            }
        } else {
            // 3. 血月光晕（身处领域内）
            float inside = fxBloodMask(-Zone.xyz, Zone, ZoneExtra) * strength;
            if (inside > 0.0) {
                float moon = max(dot(rd, normalize(BLOOD_MOON_DIR)), 0.0);
                acc += vec3(0.9, 0.05, 0.06) * (pow(moon, 600.0) * 2.5 + pow(moon, 60.0) * 0.25) * inside;
            }
        }
    }

    fragColor = vec4(acc, 1.0);
}
