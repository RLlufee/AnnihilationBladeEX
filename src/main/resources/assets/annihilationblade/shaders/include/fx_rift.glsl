// 天穹裂隙（依赖 fx_common.glsl）：位于水平面上的狭长撕裂口，边缘带随时间抖动的锯齿。
//
// rift  : xyz = 裂隙中心（相对相机），w = 张开程度 0..1
// shape : xy = 裂隙长轴方向（水平单位向量 x,z），z = 半长，w = 最大半宽
// 返回 vec4(t = 视线击中裂隙平面的距离, edge = 到裂口边缘的有符号距离（>0 在裂口内）, a = 沿长轴坐标, valid)
vec4 fxRift(vec3 rd, float sceneT, vec4 rift, vec4 shape, float time) {
    if (rift.w <= 0.0 || abs(rd.y) < 1.0e-4) {
        return vec4(-1.0, -1.0e3, 0.0, 0.0);
    }
    float t = rift.y / rd.y;
    if (t <= 0.0 || t > sceneT) {
        return vec4(-1.0, -1.0e3, 0.0, 0.0);
    }
    vec2 h = (rd * t).xz - rift.xz;
    vec2 u = shape.xy;
    vec2 v = vec2(-u.y, u.x);
    float a = dot(h, u);
    float b = dot(h, v);
    float halfLength = shape.z * (0.35 + 0.65 * rift.w);
    float s = clamp(a / max(halfLength, 1.0e-3), -1.0, 1.0);
    float profile = pow(max(1.0 - s * s, 0.0), 0.8);
    float jag = (fxNoise1(a * 0.35 + time * 0.8) - 0.5) * 0.5 + (fxNoise1(a * 1.3 - time * 1.7) - 0.5) * 0.25;
    float halfWidth = shape.w * rift.w * profile * (1.0 + jag);
    float edge = abs(a) < halfLength ? halfWidth - abs(b) : -(abs(a) - halfLength) - abs(b);
    return vec4(t, edge, a, 1.0);
}
