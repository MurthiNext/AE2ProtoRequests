#version 150

uniform sampler2D RegionMask;
uniform vec2 GuiSize;
uniform float Time;
uniform float Seed;

in vec2 texCoord;
out vec4 fragColor;

const vec3 INK = vec3(0.035, 0.024, 0.067);
const vec3 VIOLET = vec3(0.584, 0.400, 0.910);
const vec3 CYAN = vec3(0.447, 0.906, 0.937);
const vec3 MAGENTA = vec3(0.918, 0.420, 0.686);
const vec3 PALE = vec3(0.933, 0.898, 1.0);

float hash(vec2 value) {
    vec3 p = fract(vec3(value.xyx) * 0.1031);
    p += dot(p, p.yzx + 33.33 + Seed);
    return fract((p.x + p.y) * p.z);
}

float maskAt(vec2 point) {
    vec2 uv = point / GuiSize;
    return texture(RegionMask, vec2(uv.x, 1.0 - uv.y)).r;
}

vec2 fracture(vec2 point) {
    float tick = floor(Time * 7.0);
    float row = floor(point.y / 3.0);
    float column = floor(point.x / 5.0);
    float burst = step(0.86, hash(vec2(floor(point.y / 9.0), floor(Time * 4.0))));
    float x = (hash(vec2(row, 17.0)) - 0.5) * 11.0;
    x += (hash(vec2(floor(point.y / 12.0), 29.0)) - 0.5) * 8.0;
    x += (hash(vec2(row, tick)) - 0.5) * (3.0 + burst * 22.0);
    float y = (hash(vec2(column, 43.0)) - 0.5) * 8.0;
    y += (hash(vec2(column, tick)) - 0.5) * 3.0;
    return point + floor(vec2(x, y));
}

float coverage(vec2 point) {
    return maskAt(fracture(point));
}

int glyph(int value) {
    if (value == 0) return 31599;
    if (value == 1) return 9362;
    if (value == 2) return 29671;
    if (value == 3) return 29647;
    if (value == 4) return 23497;
    if (value == 5) return 31183;
    if (value == 6) return 31215;
    if (value == 7) return 29257;
    if (value == 8) return 31727;
    if (value == 9) return 31695;
    if (value == 10) return 31725;
    if (value == 11) return 27502;
    if (value == 12) return 31015;
    if (value == 13) return 27566;
    if (value == 14) return 31207;
    return 31204;
}

float dataGlyph(vec2 point, float clock) {
    vec2 cell = floor(point / vec2(4.0, 8.0));
    vec2 pixel = floor(mod(point, vec2(4.0, 8.0)));
    if (pixel.x >= 3.0 || pixel.y >= 5.0 || mod(cell.x, 5.0) == 4.0) return 0.0;
    float address = 1.0 - step(3.0, mod(cell.x, 25.0));
    float epoch = floor(clock * (4.0 + hash(vec2(cell.y, 7.0)) * 6.0));
    float value = hash(cell + vec2(epoch * (1.0 - address), floor(Seed)));
    if (hash(vec2(floor(cell.x / 5.0), cell.y)) < 0.17) return 0.0;
    int bit = int(pixel.x + pixel.y * 3.0);
    return float((glyph(int(value * 16.0)) >> bit) & 1);
}

void main() {
    vec2 point = floor(texCoord * GuiSize) + 0.5;
    vec2 brokenPoint = fracture(point);
    float inside = maskAt(brokenPoint);
    float tick = floor(Time * 9.0);
    vec2 shardCell = floor(point / vec2(7.0, 3.0));
    float shardSeed = hash(shardCell + vec2(tick, 71.0));
    if (inside < 0.5) {
        vec2 reach = vec2(8.0 + shardSeed * 9.0, 4.0 + shardSeed * 5.0);
        float nearby = max(max(maskAt(brokenPoint + vec2(reach.x, 0.0)),
                maskAt(brokenPoint - vec2(reach.x, 0.0))),
                max(maskAt(brokenPoint + vec2(0.0, reach.y)),
                maskAt(brokenPoint - vec2(0.0, reach.y))));
        vec2 pixel = mod(point, vec2(7.0, 3.0));
        if (nearby < 0.5 || shardSeed < 0.76 || pixel.x > 2.0 + shardSeed * 3.0
                || pixel.y > 1.0 + step(0.94, shardSeed)) discard;
        vec3 shardColor = mix(VIOLET, CYAN, step(0.9, shardSeed));
        fragColor = vec4(shardColor * (0.38 + shardSeed * 0.45), 1.0);
        return;
    }

    float band = floor(point.y / 8.0);
    float bandSeed = hash(vec2(band, 53.0));
    float carrier = mod(Time * 38.0 + Seed * 3.0, GuiSize.y + 48.0) - 24.0;
    float scan = 1.0 - smoothstep(0.0, 7.0, abs(point.y - carrier));
    float scanLine = 1.0 - smoothstep(0.0, 1.0, abs(point.y - carrier + 6.0));
    float tear = step(0.91, hash(vec2(band, floor(Time * 6.0))));
    float shift = tear * (hash(vec2(band, floor(Time * 11.0))) - 0.5) * 46.0;
    vec2 stream = point + vec2(floor(shift - Time * (2.0 + bandSeed * 4.0)),
            floor(Time * 2.0) * 8.0);
    float data = dataGlyph(stream, Time);
    float cyanData = dataGlyph(stream + vec2(2.0 + tear * 3.0, 0.0), Time);
    float magentaData = dataGlyph(stream - vec2(2.0 + tear, 0.0), Time);
    float pulse = pow(max(0.0, sin(point.x * 0.043 - Time * 2.6 + bandSeed * 9.0)), 16.0);
    vec2 sector = floor(point / vec2(40.0, 24.0));
    float activity = hash(sector + vec2(floor(Time * 2.0), 83.0));
    float luminance = (0.3 + bandSeed * 0.32 + pulse * 0.48) * (0.55 + activity * 0.45);
    vec3 color = INK + VIOLET * (activity * 0.035 + scan * 0.065);
    vec3 dataColor = mix(VIOLET, CYAN, step(0.78, bandSeed) * 0.65);
    color += mix(dataColor, PALE, pulse * 0.65) * data * luminance;
    color += CYAN * cyanData * (tear * 0.48 + scan * 0.16);
    color += MAGENTA * magentaData * tear * 0.3;

    vec2 packetCell = floor(stream / vec2(20.0, 8.0));
    vec2 packetPixel = mod(stream, vec2(20.0, 8.0));
    float packet = step(0.78, hash(packetCell + floor(Time * 5.0)))
            * step(6.0, packetPixel.y) * step(packetPixel.x, 4.0 + hash(packetCell) * 13.0);
    color += mix(VIOLET, CYAN, bandSeed) * packet * (0.2 + pulse * 0.55);
    float dropout = step(0.94, hash(vec2(floor(point.y / 3.0), floor(Time * 8.0))));
    color = mix(color, INK + CYAN * 0.025, dropout * 0.8);
    vec2 faultPoint = point + vec2(floor(Time * 3.0) * 13.0, 0.0);
    vec2 faultCell = floor(faultPoint / vec2(64.0, 16.0));
    vec2 faultPixel = mod(faultPoint, vec2(64.0, 16.0));
    float faultSeed = hash(faultCell + vec2(floor(Time * 3.0), 97.0));
    float fault = step(0.87, faultSeed) * step(3.0, faultPixel.y) * step(faultPixel.y, 11.0)
            * step(faultPixel.x, 18.0 + faultSeed * 34.0);
    vec3 faultColor = mix(VIOLET, CYAN, step(0.96, faultSeed));
    float faultLine = 1.0 - step(4.0, faultPixel.y);
    float faultBits = step(1.0, mod(floor(faultPixel.x), 3.0));
    color = mix(color, faultColor * (0.13 + faultBits * 0.08 + faultLine * 0.5)
            + PALE * data * 0.35, fault);
    color += CYAN * (scan * data * 0.35 + scanLine * (0.06 + packet * 0.25));
    color *= 0.93 + 0.07 * mod(floor(point.y), 2.0);

    // 轮廓只取合并遮罩的邻域，交叉处不会生成内部描边。
    float inner = min(min(coverage(point + vec2(1.0, 0.0)), coverage(point - vec2(1.0, 0.0))),
            min(coverage(point + vec2(0.0, 1.0)), coverage(point - vec2(0.0, 1.0))));
    float inset = min(min(coverage(point + vec2(4.0, 0.0)), coverage(point - vec2(4.0, 0.0))),
            min(coverage(point + vec2(0.0, 4.0)), coverage(point - vec2(0.0, 4.0))));
    float edgePulse = hash(vec2(floor(point.y / 6.0), floor(Time * 5.0)));
    vec3 rimColor = mix(VIOLET, CYAN, step(0.68, edgePulse));
    color += rimColor * (1.0 - inset) * (0.1 + scan * 0.18);
    color = mix(color, rimColor * (0.45 + edgePulse * 0.5) + PALE * scan * 0.25,
            (1.0 - inner) * (0.65 + edgePulse * 0.35));
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
