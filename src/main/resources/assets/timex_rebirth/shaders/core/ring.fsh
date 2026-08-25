#version 150

in vec2 texCoord0;

uniform vec4 ColorModulator;
uniform vec4 innerColor;
uniform vec4 outerColor;

uniform float innerRadius; // inner radius (UV space, 0-0.5)
uniform float outerRadius; // outer radius (UV space, 0.5)
uniform float startAngle;  // arc start angle (degrees, clockwise from top)
uniform float endAngle;    // arc end angle (degrees, clockwise from top)

uniform float Smooth;      // anti-aliasing width (UV space)

out vec4 fragColor;

// atan2 that returns [0, 360) with 0 at the top (12 o'clock), going clockwise
float atan2(float y, float x) {
    const float PI2 = 6.2831853071795864769252867665590;
    return mod(atan(y, x) + (PI2 * 0.5), PI2);
}

void main() {
    vec2 center = vec2(0.5, 0.5);
    float distan = distance(center, texCoord0);

    // Rotate so that startAngle maps to 0 degrees
    float radian = radians(startAngle + 90.0);
    mat2 rotationMatrix = mat2(cos(radian), -sin(radian), sin(radian), cos(radian));
    vec2 translatedUV = texCoord0 - center;
    vec2 rotatedUV = rotationMatrix * translatedUV;
    vec2 finalUV = rotatedUV + center;
    float angle = degrees(atan2(finalUV.y - center.y, finalUV.x - center.x));

    float sweep = endAngle - startAngle;
    float inAngleRange = smoothstep(0.0, Smooth, angle) * (1.0 - smoothstep(sweep - Smooth, sweep, angle));
    float inRadiusRange = smoothstep(innerRadius, innerRadius + Smooth, distan) * (1.0 - smoothstep(outerRadius - Smooth, outerRadius, distan));

    float inSector = inAngleRange * inRadiusRange;
    if (inSector <= 0.0) {
        discard;
    }

    float radiusRatio = (outerRadius > innerRadius)
        ? (distan - innerRadius) / (outerRadius - innerRadius)
        : 0.0;
    vec4 fColor = mix(innerColor, outerColor, clamp(radiusRatio, 0.0, 1.0));
    fragColor = fColor * ColorModulator;
    fragColor.a *= inSector;
}
