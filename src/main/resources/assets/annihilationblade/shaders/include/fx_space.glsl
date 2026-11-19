// AnnihilationBlade FX 宇宙/时空效果库（依赖 fx_common.glsl）：虚空星海、黑洞透镜、事件视界、射线-包围盒求交。

// 虚空：深紫星云 + 青色暗流 + 星点。cell 用于让不同碎片看到不同的星空。
vec3 fxVoidSky(vec3 rd, vec3 cell, float time) {
    float n = fxFbm3(rd * 3.0 + cell * 0.37 + vec3(0.0, 0.0, time * 0.03));
    vec3 nebula = mix(vec3(0.015, 0.0, 0.04), vec3(0.24, 0.05, 0.42), smoothstep(0.35, 0.85, n));
    nebula += vec3(0.04, 0.22, 0.38) * smoothstep(0.62, 0.95, fxFbm3(rd * 6.0 - cell));
    vec3 sp = rd * 180.0;
    float star = fxHash13(floor(sp));
    float starMask = smoothstep(0.985, 1.0, star) * (1.0 - smoothstep(0.0, 0.45, length(fract(sp) - 0.5)));
    return nebula + vec3(starMask) * 1.4;
}

// 黑洞引力透镜的屏幕空间偏移：越靠近事件视界，采样点越向黑洞中心（cuv）弯折。
vec2 fxLensOffset(vec2 uv, vec2 cuv, float dRay, float coreR) {
    float lens = coreR * coreR / max(dRay * dRay, coreR * coreR * 0.25);
    return (cuv - uv) * clamp(lens * 0.55, 0.0, 0.85);
}

// 事件视界：射线击中黑洞球体处为纯黑，外围有一圈暗晕。
vec3 fxEventHorizon(vec3 color, vec3 rd, vec3 center, float coreR, float dRay, float tc, float sceneT) {
    vec2 coreHit = fxSphere(rd, center, coreR);
    if (coreHit.y > 0.0 && max(coreHit.x, 0.0) < sceneT) {
        return color * smoothstep(coreR * 0.9, coreR * 1.02, dRay);
    }
    if (tc - coreR < sceneT) {
        float halo = exp(-(dRay - coreR) / (coreR * 0.6));
        return color * (1.0 - 0.6 * halo);
    }
    return color;
}

// 刻蚀在场景表面上的网格：按表面法线选择投影平面（地面用 xz，墙面用 zy/xy），返回 0..1 线条强度。
// local 为表面点相对网格原点的坐标，cell 为格子边长，width 为线宽（格子单位）。
float fxSurfaceGrid(vec3 local, vec3 normal, float cell, float width) {
    vec3 an = abs(normal);
    vec2 q = an.y > max(an.x, an.z) ? local.xz : (an.x > an.z ? local.zy : local.xy);
    q /= cell;
    vec2 fw = min(fwidth(q), vec2(0.5));
    vec2 d = 0.5 - abs(fract(q) - 0.5);
    vec2 line = 1.0 - smoothstep(vec2(width), vec2(width) + fw * 1.5, d);
    return max(line.x, line.y);
}

// 射线（起点为相机原点）与轴对齐包围盒求交，返回 (tNear, tFar)；tFar < tNear 表示未命中。
vec2 fxBox(vec3 rd, vec3 boxMin, vec3 boxMax) {
    vec3 inv = 1.0 / (sign(rd) * max(abs(rd), vec3(1.0e-6)));
    vec3 t0 = boxMin * inv;
    vec3 t1 = boxMax * inv;
    vec3 tMin = min(t0, t1);
    vec3 tMax = max(t0, t1);
    return vec2(max(max(tMin.x, tMin.y), tMin.z), min(min(tMax.x, tMax.y), tMax.z));
}
