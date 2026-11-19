#version 150

// 泛光升采样：对低一级做 3x3 帐篷滤波后与当前级相加，逐级扩散出宽而柔和的辉光。
uniform sampler2D Sampler0; // 低一级（更模糊）
uniform sampler2D Sampler1; // 当前级降采样结果
uniform vec2 TexelSize;
uniform float Radius;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 d = TexelSize * Radius;
    vec3 up = texture(Sampler0, texCoord + vec2(-d.x, d.y)).rgb
        + texture(Sampler0, texCoord + vec2(0.0, d.y)).rgb * 2.0
        + texture(Sampler0, texCoord + vec2(d.x, d.y)).rgb
        + texture(Sampler0, texCoord + vec2(-d.x, 0.0)).rgb * 2.0
        + texture(Sampler0, texCoord).rgb * 4.0
        + texture(Sampler0, texCoord + vec2(d.x, 0.0)).rgb * 2.0
        + texture(Sampler0, texCoord + vec2(-d.x, -d.y)).rgb
        + texture(Sampler0, texCoord + vec2(0.0, -d.y)).rgb * 2.0
        + texture(Sampler0, texCoord + vec2(d.x, -d.y)).rgb;
    up /= 16.0;
    fragColor = vec4(texture(Sampler1, texCoord).rgb + up * 0.85, 1.0);
}
