#version 150

// 裂界 · 场景通道「空间被吸入伤口」：以伤口为中心的竖向椭圆区域内，画面向中心收缩并略微旋转，
// 越靠近伤口越强；三个颜色通道收缩量不同，边缘泛起品红色散，中心略微压暗。
uniform sampler2D Sampler0; // 场景颜色
uniform vec2 OutSize;
uniform float FxTime;
uniform vec4 Wound; // xy = 伤口中心（屏幕 UV），z = 影响半径（屏幕高度比例），w = 收缩强度
uniform vec4 Shape; // x = 竖向拉伸, y = 旋转量（弧度）, z = 色散强度, w = 中心压暗

in vec2 texCoord;

out vec4 fragColor;

vec2 rotate2(vec2 v, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec2(c * v.x - s * v.y, s * v.x + c * v.y);
}

void main() {
    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 p = vec2(texCoord.x * aspect, texCoord.y);
    vec2 c = vec2(Wound.x * aspect, Wound.y);
    vec2 rel = p - c;
    vec2 ellipse = vec2(rel.x, rel.y / max(Shape.x, 0.1));
    float r = length(ellipse) / max(Wound.z, 1.0e-4);
    if (r >= 1.0 || Wound.w <= 0.0) {
        fragColor = vec4(texture(Sampler0, texCoord).rgb, 1.0);
        return;
    }
    float fall = 1.0 - r;
    fall = fall * fall * (3.0 - 2.0 * fall);
    float wobble = 1.0 + 0.08 * sin(FxTime * 11.0 + r * 9.0);
    float k = Wound.w * fall * wobble;
    vec2 twisted = rotate2(rel, Shape.y * fall);

    vec2 qR = c + twisted * (1.0 + k * (1.0 + Shape.z));
    vec2 qG = c + twisted * (1.0 + k);
    vec2 qB = c + twisted * (1.0 + k * (1.0 - Shape.z));
    vec3 color = vec3(
        texture(Sampler0, clamp(vec2(qR.x / aspect, qR.y), vec2(0.001), vec2(0.999))).r,
        texture(Sampler0, clamp(vec2(qG.x / aspect, qG.y), vec2(0.001), vec2(0.999))).g,
        texture(Sampler0, clamp(vec2(qB.x / aspect, qB.y), vec2(0.001), vec2(0.999))).b
    );
    color *= 1.0 - Shape.w * fall * 0.5;
    color += vec3(0.5, 0.08, 0.6) * Shape.z * fall * fall * 0.6;
    fragColor = vec4(color, 1.0);
}
