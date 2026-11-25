#version 150

in vec3 Position;
in vec4 Color;
in vec2 ShapePoint;
in vec2 ShapeSize;
in vec4 BorderColor;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 shapePoint;
flat out vec2 shapeSize;
out vec4 fillColor;
flat out vec4 borderColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    shapePoint = ShapePoint;
    shapeSize = ShapeSize;
    fillColor = Color;
    borderColor = BorderColor;
}
