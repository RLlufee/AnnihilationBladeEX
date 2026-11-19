#version 150

#moj_import <annihilationblade:fx_common.glsl>
#moj_import <annihilationblade:fx_space.glsl>

// 空间破碎 · 场景通道：碎片折射、虚空碎片、裂缝暗隙、黑洞透镜与事件视界、冲击波折射。
uniform sampler2D Sampler0; // 场景颜色
uniform sampler2D Sampler1; // 场景原始深度
uniform mat4 InvProj;
uniform mat4 InvView;
uniform mat4 ViewProj;
uniform vec4 Sphere; // xyz = 中心（相对相机），w = 半径
uniform vec4 Phase;  // x = 展开进度, y = 坍缩进度, z = 整体淡出, w = 种子
uniform vec4 Shock;  // x = 冲击波半径, y = 强度, z = 厚度, w = 黑洞半径
uniform vec4 Extra;  // x = 核心闪光, y = 虚空碎片比例, z = 碎片尺寸（格）
uniform float FxTime;

in vec2 texCoord;

out vec4 fragColor;

vec2 projectUv(vec3 p, out float w) {
    vec4 clip = ViewProj * vec4(p, 1.0);
    w = clip.w;
    return clip.xy / max(clip.w, 1.0e-4) * 0.5 + 0.5;
}

void main() {
    vec2 uv = texCoord;
    float depth = texture(Sampler1, uv).r;
    vec3 rd = fxViewRay(uv, InvProj, InvView);
    float sceneT = fxSceneDistance(depth, uv, InvProj);

    vec3 center = Sphere.xyz;
    float radius = Sphere.w;
    float reveal = Phase.x;
    float collapse = Phase.y;
    float fade = Phase.z;
    float seed = Phase.w;

    float tc = dot(center, rd);
    float dRay = length(center - rd * tc);
    float cw;
    vec2 cuv = projectUv(center, cw);
    bool centerAhead = cw > 0.0 && tc > 0.0;
    vec2 offset = vec2(0.0);

    // 冲击波：沿球壳轮廓的径向折射
    if (Shock.y > 0.0 && centerAhead) {
        float ring = exp(-pow((dRay - Shock.x) / max(Shock.z, 0.1), 2.0));
        float frontT = tc - sqrt(max(Shock.x * Shock.x - dRay * dRay, 0.0));
        vec2 dir = uv - cuv;
        float len = length(dir);
        if (frontT < sceneT && len > 1.0e-4) {
            offset += dir / len * ring * Shock.y * 0.035;
        }
    }

    // 黑洞引力透镜：越靠近事件视界，光线越向中心弯折
    float coreR = Shock.w;
    if (coreR > 0.0 && centerAhead && tc - radius < sceneT) {
        offset += fxLensOffset(uv, cuv, dRay, coreR);
    }

    // 碎裂场：射线进入球体处的 3D Voronoi 单元即「碎片」
    float fieldMask = 0.0;
    vec3 cell = vec3(0.0);
    vec3 vor = vec3(9.0, 9.0, 1.0);
    vec2 hit = fxSphere(rd, center, radius);
    if (fade > 0.0 && hit.y > 0.0) {
        float tEntry = max(hit.x, 0.0);
        float tExit = min(hit.y, sceneT);
        if (tEntry < tExit) {
            float tSample = hit.x > 0.0 ? tEntry : min(tExit, radius * 0.6);
            vec3 p = rd * tSample - center;
            float rel = length(p) / radius;
            float front = 1.0 - smoothstep(reveal - 0.12, reveal, rel);
            float boundary = 1.0 - smoothstep(0.82, 1.0, rel);
            fieldMask = front * boundary * fade;
            vor = fxVoronoi3(p / max(Extra.z, 0.25) + seed, cell);
            vec3 h = fxHash33(cell + 17.0);
            vec2 shard = (h.xy - 0.5) * (0.018 + 0.05 * collapse);
            shard += (cuv - uv) * collapse * 0.12 * h.z;
            offset += shard * fieldMask;
        }
    }

    vec3 color = texture(Sampler0, clamp(uv + offset, vec2(0.001), vec2(0.999))).rgb;

    if (fieldMask > 0.0) {
        color = mix(color, color * vec3(0.78, 0.72, 1.05) + vec3(0.02, 0.0, 0.04), 0.45 * fieldMask);
        float isVoid = 1.0 - smoothstep(Extra.y - 0.05, Extra.y, vor.z);
        color = mix(color, fxVoidSky(rd, cell, FxTime), isVoid * fieldMask);
        float crack = 1.0 - smoothstep(0.0, 0.07, vor.y);
        color *= 1.0 - crack * 0.8 * fieldMask;
        color *= 1.0 + (fxHash13(cell + 5.0) - 0.5) * 0.25 * fieldMask;
    }

    // 事件视界：纯黑核心与外围暗晕
    if (coreR > 0.0 && centerAhead) {
        color = fxEventHorizon(color, rd, center, coreR, dRay, tc, sceneT);
    }

    fragColor = vec4(color, 1.0);
}
