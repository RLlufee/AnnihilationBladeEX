#version 150

#moj_import <annihilationblade:fx_common.glsl>

// 最终合成：
//  1. 场景 + 预乘 Alpha 本体/暗部层（Sampler4）
//  2. HDR 发光层（网格 + 体积 + 泛光）在线性空间能量合成，带高光自然白热去饱和（原版无特效处零色偏）
//  3. 同步作用于「场景 + 特效」的径向模糊、边缘速度线模糊（中心清晰四周拉丝）与非对称色差
//  4. 去色、暗角、带中心过曝与轻微颗粒的爆闪、黑白定格冲击帧
//  5. 8x8 Bayer 有序抖动消除 8-bit 色带
uniform sampler2D Sampler0; // 场景
uniform sampler2D Sampler1; // 网格 HDR 特效层
uniform sampler2D Sampler2; // 体积层
uniform sampler2D Sampler3; // 泛光
uniform sampler2D Sampler4; // 预乘 Alpha 本体/暗部层
uniform vec2 OutSize;
uniform float FxTime;
uniform float BodyWeight;
uniform float VolumeWeight;
uniform float BloomStrength;
uniform vec4 Flash;       // rgb = 颜色, a = 强度
uniform vec2 FlashExtra;  // x = 过曝/颗粒增强系数, y = 保留
uniform float Aberration;
uniform float ZoomBlur;
uniform float DashBlur;   // 中心清晰、边缘强向心拉丝速度线强度
uniform vec2 Center;
uniform vec4 Vignette;
uniform float Desaturate;
uniform vec2 ImpactFrame; // x = 冲击帧强度, y = 是否反相 (>0.5)

in vec2 texCoord;

out vec4 fragColor;

// 采样单点的合成色彩（场景 + 本体暗部层 + 线性 HDR 发光），保证模糊/色差对背景与特效同步生效。
vec3 sampleComposite(vec2 uv) {
    vec2 suv = clamp(uv, vec2(0.0005), vec2(0.9995));
    vec3 base = texture(Sampler0, suv).rgb;
    if (BodyWeight > 0.5) {
        vec4 body = texture(Sampler4, suv);
        // 预乘 Alpha 合成：先压暗/遮盖背景，再叠加本体可见色彩
        base = base * clamp(1.0 - body.a, 0.0, 1.0) + body.rgb;
    }

    vec3 glow = texture(Sampler1, suv).rgb;
    if (VolumeWeight > 0.5) {
        glow += texture(Sampler2, suv).rgb * VolumeWeight;
    }
    if (BloomStrength > 0.001) {
        glow += texture(Sampler3, suv).rgb * BloomStrength * 0.22;
    }
    glow = max(glow, vec3(0.0));
    float glowPeak = max(max(glow.r, glow.g), glow.b);
    if (glowPeak <= 1.0e-5) {
        return clamp(base, vec3(0.0), vec3(1.0));
    }

    // 在近似线性空间进行物理能量叠加，再通过高光自然去饱和曲线平滑过渡到白热核心
    vec3 baseLin = pow(clamp(base, vec3(0.0), vec3(1.0)), vec3(2.2));
    // 高能量段按亮度平滑向白热收拢，避免纯通道 exp 导致紫/青偏色
    float luma = dot(glow, vec3(0.2126, 0.7152, 0.0722));
    float desat = 1.0 - exp(-max(luma - 0.85, 0.0) * 0.32);
    vec3 shapedGlow = mix(glow, vec3(luma * 1.06), desat * 0.62);

    // 透射式 HDR 合成（暗处饱满、亮处不过曝死白，且 glow -> 0 时严格收敛回 baseLin）
    vec3 lin = baseLin + shapedGlow;
    vec3 compressed = 1.0 - exp(-lin);
    // 仅按发光占比平滑混入压缩曲线，保证未发光背景与原版画面 100% 一致
    float blendMask = 1.0 - exp(-glowPeak * 1.45);
    vec3 outLin = mix(baseLin, compressed, blendMask);
    // 保留高能核心微弱的线性白热冲顶，拉出刀光/爆发的锋利对比
    outLin += max(shapedGlow - 1.6, vec3(0.0)) * 0.035;
    return pow(clamp(outLin, vec3(0.0), vec3(1.0)), vec3(1.0 / 2.2));
}

void main() {
    vec2 uv = texCoord;
    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 fromCenter = uv - Center;
    float radialDist = length(fromCenter * vec2(aspect, 1.0));

    vec3 color;
    if (ZoomBlur > 0.001) {
        vec3 sum = vec3(0.0);
        float weight = 0.0;
        for (int i = 0; i < 10; i++) {
            float k = float(i) / 9.0;
            float w = 1.0 - k * 0.55;
            sum += sampleComposite(uv - fromCenter * ZoomBlur * 0.11 * k) * w;
            weight += w;
        }
        color = sum / weight;
    } else {
        color = sampleComposite(uv);
    }

    // 边缘向心速度线模糊：中心区域保持锐利清晰，四周产生高速突进/斩裂拉丝与轻微压暗
    if (DashBlur > 0.001) {
        float edge = smoothstep(0.18, 0.92, radialDist * 1.25);
        float edgePower = edge * clamp(DashBlur, 0.0, 1.5);
        if (edgePower > 0.002) {
            vec2 inward = -normalize(fromCenter + vec2(1.0e-5));
            float span = (0.008 + edge * 0.030) * edgePower;
            vec3 dashSum = color * 0.25;
            float wSum = 0.25;
            for (int i = 1; i <= 7; i++) {
                float k = float(i) / 7.0;
                float w = mix(0.16, 0.04, k);
                dashSum += sampleComposite(uv + inward * span * k * (1.0 + k * 1.6)) * w;
                wSum += w;
            }
            color = mix(color, dashSum / wSum, clamp(edgePower * 0.85, 0.0, 1.0));
            // 中心微提亮 + 边缘轻微压暗，聚焦视觉焦点
            float centerLift = (1.0 - smoothstep(0.0, 0.5, radialDist)) * min(DashBlur, 1.0) * 0.06;
            color = color * (1.0 - edgePower * 0.15) + vec3(centerLift);
        }
    }

    // 径向非对称 RGB 色差（同步作用于背景与特效，避免贴纸割裂感）
    float totalAb = Aberration + DashBlur * 0.55 * smoothstep(0.18, 0.92, radialDist * 1.25);
    if (totalAb > 0.001) {
        vec2 shift = fromCenter * totalAb * 0.011;
        vec3 shifted;
        shifted.r = sampleComposite(uv + shift * 1.05).r;
        shifted.g = color.g;
        shifted.b = sampleComposite(uv - shift * 0.92).b;
        color = mix(color, shifted, clamp(totalAb * 0.85, 0.0, 1.0));
    }

    if (Desaturate > 0.001) {
        float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
        color = mix(color, vec3(luma) * vec3(0.92, 0.90, 1.0), Desaturate);
    }

    if (Vignette.a > 0.001) {
        float v = smoothstep(0.25, 1.0, length((uv - 0.5) * vec2(aspect, 1.0)) * 1.1);
        color = mix(color, Vignette.rgb, v * Vignette.a);
    }

    // 爆闪：中心强过曝 + 柔和余晖 + 细腻胶片颗粒，告别平涂白屏
    if (Flash.a > 0.001) {
        float core = 1.0 - smoothstep(0.0, 0.95, radialDist * 1.15);
        float boost = FlashExtra.x;
        float glare = Flash.a * (0.78 + core * (0.45 + boost * 0.55));
        vec3 flashed = color + Flash.rgb * glare * (0.65 + boost * 0.65);
        flashed = mix(flashed, Flash.rgb, clamp(glare * 0.52, 0.0, 0.88));
        if (boost > 0.01) {
            vec2 grainCell = floor((uv + vec2(FxTime * 0.017, FxTime * 0.023)) * OutSize * 0.5);
            float grain = (fxHash12(grainCell) - 0.5) * Flash.a * boost * 0.055;
            flashed += vec3(grain);
            vec3 exposed = 1.0 - exp(-max(flashed, vec3(0.0)) * (1.0 + Flash.a * boost * 0.9));
            flashed = mix(flashed, exposed, clamp(Flash.a * 0.75, 0.0, 1.0));
        }
        color = flashed;
    }

    // 冲击帧：高对比黑白定格（带暗部反相与轻微暖色调）
    if (ImpactFrame.x > 0.001) {
        float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
        float edge = smoothstep(0.38, 0.50, luma);
        vec3 mono = vec3(ImpactFrame.y > 0.5 ? 1.0 - edge : edge) * vec3(1.0, 0.94, 0.85);
        color = mix(color, mono, ImpactFrame.x);
    }

    fragColor = vec4(fxDither8(color, gl_FragCoord.xy), 1.0);
}
