#version 150

uniform sampler2D Scene;
uniform vec2 ScreenSize;
uniform float Progress;
uniform float Strength;
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

vec3 sceneAt(vec2 uv) {
    vec2 inset = 0.5 / ScreenSize;
    uv = clamp(uv, inset, 1.0 - inset);
    return texture(Scene, vec2(uv.x, 1.0 - uv.y)).rgb;
}

void main() {
    float t = clamp(Progress, 0.0, 1.0);
    float fade = 1.0 - smoothstep(0.55, 1.0, t);
    float impulse = exp(-8.0 * t) * fade;
    float tail = exp(-3.8 * t) * fade;
    float aspect = ScreenSize.x / ScreenSize.y;
    vec2 aspectScale = vec2(aspect, 1.0);
    vec2 center = (texCoord - 0.5) * aspectScale;
    float distance = length(center);
    vec2 direction = center / max(distance, 0.001);
    vec2 grid = vec2(360.0 * aspect, 360.0);
    vec2 point = floor(texCoord * grid) + 0.5;

    float radius = pow(clamp((t - 0.025) / 0.52, 0.0, 1.0), 0.72) * length(aspectScale) * 0.62;
    float waveDistance = (distance - radius) / 0.045;
    float wave = exp(-waveDistance * waveDistance) * (1.0 - smoothstep(0.22, 0.62, t));
    float compression = (1.0 - smoothstep(0.0, 0.09, t)) * 0.055;
    float row = floor(point.y / 9.0);
    float epoch = floor(t * 24.0);
    float rowSeed = hash(vec2(row, epoch));
    float torn = step(0.72, rowSeed);
    float tear = (rowSeed - 0.5) * torn * tail * 0.085;
    vec2 uv = 0.5 + (texCoord - 0.5) * (1.0 + compression * Strength);
    uv += direction / aspectScale * wave * sin(waveDistance * 1.8) * 0.025 * Strength;
    uv.x += tear * Strength;
    uv.y += (hash(vec2(row, epoch + 71.0)) - 0.5) * torn * tail * 0.006 * Strength;

    vec2 split = (direction / aspectScale * (impulse * 0.006 + wave * 0.013)
            + vec2(tear * 0.35, 0.0)) * Strength;
    vec3 color = vec3(sceneAt(uv + split).r, sceneAt(uv).g, sceneAt(uv - split).b);
    vec2 stretch = center / aspectScale * impulse * 0.035 * Strength;
    vec3 echo = sceneAt(uv - stretch) * 0.6 + sceneAt(uv - stretch * 2.0) * 0.4;
    color = mix(color, echo, impulse * Strength * 0.28);

    float edge = smoothstep(0.18, 0.72, distance);
    color = mix(color, INK, edge * tail * Strength * 0.62);
    color += VIOLET * edge * impulse * Strength * 0.09;
    float flash = exp(-t * 48.0) * exp(-distance * distance * 16.0);
    color = mix(color, PALE, flash * Strength * 0.72);

    float arc = hash(vec2(floor(atan(center.y, center.x) * 36.0), 19.0));
    float rim = exp(-waveDistance * waveDistance * 5.0) * (1.0 - smoothstep(0.15, 0.58, t));
    color += mix(VIOLET, CYAN, arc) * rim * (0.1 + step(0.6, arc) * 0.24) * Strength;
    color += MAGENTA * wave * torn * tail * Strength * 0.12;

    vec2 cell = floor(point / vec2(24.0, 6.0));
    vec2 pixel = mod(point, vec2(24.0, 6.0));
    float packetSeed = hash(cell + vec2(37.0, 83.0));
    vec2 packetCenter = ((cell + 0.5) * vec2(24.0, 6.0) / grid - 0.5) * aspectScale;
    float arrival = length(packetCenter) * 0.28 + packetSeed * 0.09;
    float age = t - arrival;
    float packetLife = smoothstep(0.0, 0.025, age) * (1.0 - smoothstep(0.04, 0.24, age));
    float packet = step(0.82, packetSeed) * packetLife * fade;
    packet *= step(pixel.x, 7.0 + packetSeed * 15.0) * step(pixel.y, 4.0);
    float bits = step(0.55, hash(floor(point / vec2(2.0, 1.0)))) * step(1.0, pixel.y);
    vec3 packetColor = mix(VIOLET, CYAN, step(0.94, packetSeed));
    color = mix(color, INK + packetColor * (0.18 + bits * 0.38), packet * Strength * 0.7);
    color += packetColor * packet * (1.0 - step(1.0, pixel.y)) * Strength * 0.5;
    float scanline = mod(floor(point.y), 3.0) < 1.0 ? 1.0 : 0.0;
    color *= 1.0 - scanline * torn * tail * Strength * 0.16;

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
