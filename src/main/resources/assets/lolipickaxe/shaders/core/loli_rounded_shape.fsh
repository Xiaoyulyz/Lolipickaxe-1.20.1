#version 150

uniform vec2 rectSize;
uniform float cornerRadius;
uniform float guiScale;
uniform float borderWidth;
uniform vec4 fillColor;
uniform vec4 borderColor;

in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;

float roundedBoxDistance(vec2 point, vec2 size, float radius) {
    vec2 halfSize = size * 0.5;
    vec2 q = abs(point - halfSize) - halfSize + vec2(radius);
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    float distanceToEdge = roundedBoxDistance(texCoord * rectSize, rectSize, cornerRadius);
    float antialias = max(fwidth(distanceToEdge), 0.65 / max(guiScale, 1.0));
    float outerCoverage = 1.0 - smoothstep(-antialias, antialias, distanceToEdge);
    if (outerCoverage <= 0.001) discard;

    float innerCoverage = borderWidth <= 0.0
        ? outerCoverage
        : 1.0 - smoothstep(-antialias, antialias, distanceToEdge + borderWidth);
    float innerMix = clamp(innerCoverage / max(outerCoverage, 0.0001), 0.0, 1.0);
    vec4 color = mix(borderColor, fillColor, innerMix) * vertexColor;
    fragColor = vec4(color.rgb, color.a * outerCoverage);
}
