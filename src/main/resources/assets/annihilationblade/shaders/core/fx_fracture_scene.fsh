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
            float cut = clamp(Extra.w, 0.0, 1.5);
            vec3 h = fxHash33(cell + 17.0);
            vec2 shard = (h.xy - 0.5) * (0.022 + 0.055 * collapse + 0.028 * cut);
            shard += (cuv - uv) * (collapse * 0.12 + cut * 0.05) * h.z;
            // 单块碎片的独立棱镜倍镜折射，呈现玻璃碎裂与错位放大感
            float facetZoom = 1.0 + (0.14 + 0.34 * cut) * (0.35 + 0.65 * h.z);
            vec2 facetPivot = clamp(cuv + (h.xy - 0.5) * 0.24, 0.05, 0.95);
            vec2 prismZoom = clamp((facetPivot - uv) * (1.0 - 1.0 / facetZoom), vec2(-0.08), vec2(0.08));
            offset += (shard + prismZoom) * fieldMask;
        }
    }

    float refractT = max(tc - radius, 0.0);
    vec2 sampleUv = fxSafeRefractUv(uv, offset, refractT, Sampler1, InvProj);
    vec3 color = texture(Sampler0, sampleUv).rgb;

    if (fieldMask > 0.0) {
        float cut = clamp(Extra.w, 0.0, 1.5);
        vec3 hEdge = fxHash33(cell + 17.0);
        float isVoid = 1.0 - smoothstep(Extra.y - 0.05, Extra.y, vor.z);
        float crack = 1.0 - smoothstep(0.0, 0.075, vor.y);
        float bevel = exp(-max(vor.y - 0.015, 0.0) * 58.0) * (0.25 + 0.95 * cut);

        float tintStrength = (0.45 + 0.22 * cut) * fieldMask;
        vec3 shardColor = mix(color, color * vec3(0.76, 0.74, 1.08) + vec3(0.02, 0.005, 0.045), tintStrength);
        shardColor = mix(shardColor, fxVoidSky(rd, cell, FxTime), isVoid * fieldMask);
        shardColor *= 1.0 + (fxHash13(cell + 5.0) - 0.5) * (0.28 + 0.24 * cut) * fieldMask;
        shardColor = mix(shardColor, clamp((shardColor - 0.5) * (1.12 + 0.14 * cut) + 0.5, 0.0, 2.5), cut * 0.45 * fieldMask * (1.0 - isVoid));
        shardColor *= 1.0 - crack * 0.85 * fieldMask;
        vec3 bevelCol = mix(vec3(0.72, 0.48, 1.0), vec3(0.85, 0.96, 1.0), hEdge.z);
        color = shardColor + bevelCol * bevel * (1.0 - crack * 0.65) * fieldMask;
    }

    // 事件视界：纯黑核心与外围暗晕
    if (coreR > 0.0 && centerAhead) {
        color = fxEventHorizon(color, rd, center, coreR, dRay, tc, sceneT);
    }

    fragColor = vec4(color, 1.0);
}
