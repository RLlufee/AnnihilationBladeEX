// 血狱领域公共函数（依赖 fx_common.glsl）。
//
// Zone      : xyz = 领域中心（地面，相对相机），w = 水平半边长
// ZoneExtra : x = 领域高度, y = 强度 0..1, z = 种子, w = 脉冲环半径（<0 表示无脉冲）

const vec3 BLOOD_DEEP = vec3(0.32, 0.0, 0.03);
const vec3 BLOOD_BRIGHT = vec3(1.0, 0.12, 0.16);
const vec3 BLOOD_MOON_DIR = vec3(0.32, 0.56, -0.76);

// 领域遮罩（正方形软边，高度 [-2, height]），local 为相对领域中心的坐标。
float fxBloodMask(vec3 local, vec4 zone, vec4 extra) {
    float halfSize = zone.w;
    float ex = 1.0 - smoothstep(halfSize - 0.8, halfSize + 0.2, abs(local.x));
    float ez = 1.0 - smoothstep(halfSize - 0.8, halfSize + 0.2, abs(local.z));
    float ey = smoothstep(-2.5, -1.5, local.y) * (1.0 - smoothstep(extra.x - 1.0, extra.x, local.y));
    return ex * ez * ey;
}

// 地表血脉：3D Voronoi 单元边界，返回 0..1（越接近边界越亮）。
float fxBloodVeins(vec3 local, float seed, float width) {
    vec3 cell;
    vec3 v = fxVoronoi3(local * 0.65 + seed, cell);
    return 1.0 - smoothstep(0.0, width, v.y);
}

// 向外扩散的脉冲环（按水平距离），返回 0..1。
float fxBloodPulse(vec3 local, float pulseRadius) {
    if (pulseRadius < 0.0) {
        return 0.0;
    }
    float d = length(local.xz);
    return exp(-pow((d - pulseRadius) / 0.9, 2.0));
}
