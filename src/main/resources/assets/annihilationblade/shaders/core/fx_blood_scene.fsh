#version 150

#moj_import <annihilationblade:fx_common.glsl>
#moj_import <annihilationblade:fx_blood.glsl>

// 血狱领域 · 场景通道：
//  · 领域内的世界被染成血色（暗部沉为深红，亮部保留猩红）；
//  · 地表浮现随脉冲明灭的血脉纹路；
//  · 身处领域中时，天空化为血色并悬挂一轮血月。
uniform sampler2D Sampler0; // 场景颜色
uniform sampler2D Sampler1; // 场景原始深度
uniform mat4 InvProj;
uniform mat4 InvView;
uniform vec4 Zone;
uniform vec4 ZoneExtra;
uniform float FxTime;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    float depth = texture(Sampler1, uv).r;
    vec3 rd = fxViewRay(uv, InvProj, InvView);
    float sceneT = fxSceneDistance(depth, uv, InvProj);
    vec3 color = texture(Sampler0, uv).rgb;
    float strength = ZoneExtra.y;

    if (strength > 0.0) {
        if (depth < 1.0) {
            vec3 local = rd * sceneT - Zone.xyz;
            float field = fxBloodMask(local, Zone, ZoneExtra) * strength;
            if (field > 0.0) {
                float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
                vec3 blood = mix(BLOOD_DEEP, BLOOD_BRIGHT, smoothstep(0.05, 0.85, luma)) * (0.35 + luma * 1.1);
                blood += vec3(color.r * 0.25, 0.0, 0.0);
                color = mix(color, blood, 0.75 * field);
                float veins = fxBloodVeins(local, ZoneExtra.z, 0.06);
                float beat = 0.55 + 0.45 * sin(FxTime * 5.0 - length(local.xz) * 0.6);
                float pulse = fxBloodPulse(local, ZoneExtra.w);
                color = mix(color, BLOOD_DEEP * 0.4, veins * 0.5 * field);
                color += BLOOD_BRIGHT * veins * (0.18 * beat + pulse * 0.8) * field;
            }
        } else {
            float inside = fxBloodMask(-Zone.xyz, Zone, ZoneExtra) * strength;
            if (inside > 0.0) {
                float height = clamp(rd.y * 0.5 + 0.5, 0.0, 1.0);
                vec3 sky = mix(vec3(0.35, 0.02, 0.03), vec3(0.04, 0.0, 0.01), height);
                sky += vec3(0.18, 0.0, 0.02) * fxFbm3(rd * 4.0 + vec3(FxTime * 0.05, 0.0, 0.0));
                float moon = dot(rd, normalize(BLOOD_MOON_DIR));
                float disk = smoothstep(0.9975, 0.9985, moon);
                float spots = fxFbm3(rd * 120.0) * 0.5 + 0.5;
                sky = mix(sky, vec3(0.95, 0.12, 0.1) * spots, disk);
                sky += vec3(0.6, 0.03, 0.04) * pow(max(moon, 0.0), 220.0) * 0.8;
                color = mix(color, sky, 0.9 * inside);
            }
        }
    }

    fragColor = vec4(color, 1.0);
}
