#version 150

in vec2 shapePoint;
flat in vec2 shapeSize;
in vec4 fillColor;
flat in vec4 borderColor;

uniform vec4 ColorModulator;

out vec4 fragColor;

float coverage(vec2 pixelMin, vec2 pixelMax, vec2 pixelSize, float inset) {
    vec2 overlap = min(pixelMax, shapeSize - inset) - max(pixelMin, vec2(inset));
    vec2 covered = clamp(overlap / pixelSize, 0.0, 1.0);
    return covered.x * covered.y;
}

void main() {
    vec2 pixelSize = max(fwidth(shapePoint), vec2(1e-5));
    vec2 pixelMin = shapePoint - 0.5 * pixelSize;
    vec2 pixelMax = shapePoint + 0.5 * pixelSize;
    float outer = coverage(pixelMin, pixelMax, pixelSize, 0.0);
    float border = borderColor.a > 0.0
            ? (outer - coverage(pixelMin, pixelMax, pixelSize, 1.0)) * borderColor.a
            : 0.0;
    float fill = (outer - border) * fillColor.a;
    float alpha = fill + border;
    if (alpha <= 0.0) {
        discard;
    }
    vec3 rgb = (fillColor.rgb * fill + borderColor.rgb * border) / alpha;
    fragColor = vec4(rgb, alpha) * ColorModulator;
}
