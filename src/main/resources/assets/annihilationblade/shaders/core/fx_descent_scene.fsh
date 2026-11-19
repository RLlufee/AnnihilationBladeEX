#version 150

#moj_import <annihilationblade:fx_common.glsl>
#moj_import <annihilationblade:fx_space.glsl>
#moj_import <annihilationblade:fx_rift.glsl>

// 神陨·宇宙夜陨降临 · 场景通道：
//  · 天穹裂隙：撕开的天幕后露出虚空星海；
//  · 暗星：引力漩涡把周围景象卷向中心（屏幕空间旋涡），叠加引力透镜与事件视界；
//  · 坍缩冲击波的折射。
uniform sampler2D Sampler0; // 场景颜色
uniform sampler2D Sampler1; // 场景原始深度
uniform mat4 InvProj;
uniform mat4 InvView;
uniform mat4 ViewProj;
uniform vec4 Star;      // xyz = 暗星中心（相对相机），w = 事件视界半径
uniform vec4 Rift;      // xyz = 裂隙中心（相对相机），w = 张开程度
uniform vec4 RiftShape; // xy = 长轴方向, z = 半长, w = 最大半宽
uniform vec4 Vortex;    // x = 漩涡强度, y = 漩涡半径, z = 旋转速度, w = 种子
uniform vec4 Shock;     // x = 冲击波半径, y = 强度, z = 厚度, w = 闪光
uniform vec4 Night;     // x = 世界压暗强度, y = 天空替换为虚空星海的强度
uniform float FxTime;

in vec2 texCoord;

out vec4 fragColor;

vec2 projectUv(vec3 p, out float w) {
    vec4 clip = ViewProj * vec4(p, 1.0);
    w = clip.w;
    return clip.xy / max(clip.w, 1.0e-4) * 0.5 + 0.5;
}

vec2 rotateAround(vec2 p, vec2 pivot, float angle) {
    vec2 d = p - pivot;
    float c = cos(angle);
    float s = sin(angle);
    return pivot + vec2(c * d.x - s * d.y, s * d.x + c * d.y);
}

void main() {
    vec2 uv = texCoord;
    float depth = texture(Sampler1, uv).r;
    vec3 rd = fxViewRay(uv, InvProj, InvView);
    float sceneT = fxSceneDistance(depth, uv, InvProj);

    vec3 star = Star.xyz;
    float tc = dot(star, rd);
    float dStar = length(star - rd * tc);
    float sw;
    vec2 suv = projectUv(star, sw);
    bool starAhead = sw > 0.0 && tc > 0.0;
    vec2 sampleUv = uv;

    // 引力漩涡：越靠近暗星旋转越强（只扭曲暗星前后可见的区域）
    if (Vortex.x > 0.0 && starAhead && tc - Vortex.y < sceneT) {
        float swirl = Vortex.x * 0.9 * exp(-dStar / max(Vortex.y * 0.22, 0.5));
        sampleUv = rotateAround(sampleUv, suv, swirl);
    }
    if (Star.w > 0.0 && starAhead && tc - Star.w < sceneT) {
        sampleUv += fxLensOffset(uv, suv, dStar, Star.w);
    }
    if (Shock.y > 0.0 && starAhead) {
        float ring = exp(-pow((dStar - Shock.x) / max(Shock.z, 0.1), 2.0));
        float frontT = tc - sqrt(max(Shock.x * Shock.x - dStar * dStar, 0.0));
        vec2 dir = uv - suv;
        float len = length(dir);
        if (frontT < sceneT && len > 1.0e-4) {
            sampleUv += dir / len * ring * Shock.y * 0.04;
        }
    }

    vec3 color = texture(Sampler0, clamp(sampleUv, vec2(0.001), vec2(0.999))).rgb;

    // 夜幕降临：整片天空化为虚空星海，大地压暗并染上紫色，让发光特效成为画面唯一的光源
    if (depth >= 1.0) {
        if (Night.y > 0.0) {
            vec3 night = fxVoidSky(rd, vec3(Vortex.w + 3.0), FxTime) * 1.2 + vec3(0.02, 0.0, 0.04);
            color = mix(color, night, Night.y);
        }
    } else if (Night.x > 0.0) {
        color = mix(color, color * vec3(0.42, 0.34, 0.66), Night.x);
    }

    // 天穹裂隙：裂口内为虚空星海，边缘略微压暗
    vec4 rift = fxRift(rd, sceneT, Rift, RiftShape, FxTime);
    if (rift.w > 0.0) {
        float inside = smoothstep(0.0, 0.6, rift.y);
        vec3 space = fxVoidSky(rd, vec3(Vortex.w), FxTime) * 1.3 + vec3(0.06, 0.0, 0.1);
        color = mix(color, space, inside);
        float shadow = exp(-max(-rift.y, 0.0) / 3.0) * (1.0 - inside);
        color *= 1.0 - shadow * 0.35 * Rift.w;
    }

    if (Star.w > 0.0 && starAhead) {
        color = fxEventHorizon(color, rd, star, Star.w, dStar, tc, sceneT);
    }

    fragColor = vec4(color, 1.0);
}
