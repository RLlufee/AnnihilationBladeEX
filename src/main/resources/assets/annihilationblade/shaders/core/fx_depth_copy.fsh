#version 150

// 把主深度（可能为 DEPTH32F_STENCIL8 等格式）复制到 R32F 颜色纹理，供后续通道采样。
uniform sampler2D Sampler0;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    fragColor = vec4(texelFetch(Sampler0, ivec2(gl_FragCoord.xy), 0).r, 0.0, 0.0, 1.0);
}
