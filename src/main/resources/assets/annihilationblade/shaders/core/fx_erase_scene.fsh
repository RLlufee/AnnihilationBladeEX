#version 150

#moj_import <annihilationblade:fx_common.glsl>

// 虚无权域 · 场景通道「格式化」：领域穹顶内的世界被还原为数据。
//  - 马赛克：穹顶内的画面按像素块量化，块大小随时间由粗变细；
//  - 去色：穹顶内只剩灰阶；
//  - 坐标网格：穹顶内所有表面浮现与方块边缘对齐的白色网格（按表面法线剔除垂直于表面的那一轴）；
//  - 抹除前沿：以中心为球心扩张的一层球壳，球壳经过处画面反相并带白色刃光；
//  - 整体反相脉冲：「万象湮灭」时整个穹顶瞬间反相；
//  - 穹顶轮廓：视线掠过球面处的一圈白色轮廓光。
// 视线穿过穹顶但落在穹顶之外的像素（天空、远景）按穿过的厚度部分生效，让穹顶呈现为一团灰色的体积。
uniform sampler2D Sampler0; // 场景颜色
uniform sampler2D Sampler1; // 场景原始深度
uniform mat4 InvProj;
uniform mat4 InvView;
uniform vec2 OutSize;
uniform vec4 Dome;    // xyz = 穹顶中心（相对相机），w = 半径
uniform vec4 Erase;    // x = 整体强度, y = 马赛克块大小（像素）, z = 抹除前沿半径, w = 前沿厚度
uniform vec4 Grid;     // x = 网格强度, y = 去色, z = 整体反相脉冲, w = 轮廓光
uniform vec4 CamFract; // xyz = 相机世界坐标的小数部分（让网格对齐方块边缘）

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 rd = fxViewRay(texCoord, InvProj, InvView);
    float depth = texture(Sampler1, texCoord).r;
    float sceneT = fxSceneDistance(depth, texCoord, InvProj);
    vec3 p = rd * min(sceneT, 1.0e4);
    vec3 normal = normalize(cross(dFdx(p), dFdy(p)) + vec3(0.0, 1.0e-6, 0.0));
    vec3 base = texture(Sampler0, texCoord).rgb;

    vec2 hit = fxSphere(rd, Dome.xyz, Dome.w);
    if (Erase.x <= 0.001 || hit.y <= 0.0) {
        fragColor = vec4(base, 1.0);
        return;
    }

    float distToCenter = length(p - Dome.xyz);
    float surfaceInside = (sceneT < 1.0e5 && distToCenter < Dome.w) ? 1.0 : 0.0;
    float through = 0.0;
    if (surfaceInside < 0.5 && max(hit.x, 0.0) < sceneT) {
        through = clamp((min(hit.y, sceneT) - max(hit.x, 0.0)) / (Dome.w * 2.0), 0.0, 1.0) * 0.7;
    }
    float inside = max(surfaceInside, through) * Erase.x;

    // 马赛克
    float block = max(Erase.y, 1.0);
    vec2 cell = (floor(texCoord * OutSize / block) + 0.5) * block / OutSize;
    vec3 color = mix(base, texture(Sampler0, cell).rgb, step(1.5, block) * inside);

    // 去色
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(color, vec3(luma), Grid.y * inside);

    // 坐标网格（只在穹顶内的实体表面上）
    if (surfaceInside > 0.5) {
        vec3 world = p + CamFract.xyz;
        vec3 edge = 0.5 - abs(fract(world) - 0.5);
        vec3 mask = step(abs(normal), vec3(0.5));
        float nearest = min(min(mix(1.0, edge.x, mask.x), mix(1.0, edge.y, mask.y)), mix(1.0, edge.z, mask.z));
        float width = 0.025 + sceneT * 0.0015;
        float line = 1.0 - smoothstep(width, width * 2.0, nearest);
        float fade = 1.0 - smoothstep(Dome.w * 0.6, Dome.w, distToCenter);
        color += vec3(line) * Grid.x * fade * Erase.x * 0.8;

        // 抹除前沿：球壳经过处反相 + 刃光
        float shell = 1.0 - smoothstep(0.0, max(Erase.w, 0.01), abs(distToCenter - Erase.z));
        if (Erase.z > 0.01) {
            color = mix(color, vec3(1.0) - color, shell * 0.85 * Erase.x);
            color += vec3(1.0) * exp(-abs(distToCenter - Erase.z) / 0.15) * 0.6 * Erase.x;
        }
    }

    // 整体反相脉冲
    color = mix(color, vec3(1.0) - color, Grid.z * inside);

    // 穹顶轮廓光：视线到球心的最近距离接近半径处
    float tc = dot(Dome.xyz, rd);
    if (tc > 0.0 && max(hit.x, 0.0) < sceneT) {
        float closest = length(Dome.xyz - rd * tc);
        float rim = exp(-abs(closest - Dome.w) / max(Dome.w * 0.015, 0.05));
        color += vec3(1.0) * rim * Grid.w * Erase.x;
    }

    fragColor = vec4(color, 1.0);
}
