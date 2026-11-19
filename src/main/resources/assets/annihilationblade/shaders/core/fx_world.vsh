#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 texCoord0;
out float viewDistance;
out vec3 relativePos;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;
    vertexColor = Color;
    texCoord0 = UV0;
    viewDistance = -viewPos.z;
    // 顶点本身就是相对相机的世界坐标（ModelViewMat 只含视图旋转）
    relativePos = Position;
}
