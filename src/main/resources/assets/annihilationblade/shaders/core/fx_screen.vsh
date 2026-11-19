#version 150

// 全屏通道公用顶点着色器：输入即 NDC 坐标，不使用 ModelView/Proj 矩阵。
in vec3 Position;

out vec2 texCoord;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    texCoord = Position.xy * 0.5 + 0.5;
}
