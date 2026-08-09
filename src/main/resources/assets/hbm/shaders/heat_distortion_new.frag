#version 120

uniform sampler2D tex_mask;
uniform float amount;

varying vec2 texCoord;

void main(){
	vec4 mask = texture2D(tex_mask, texCoord);
	float sampleStrength = max(mask.a, max(mask.r, max(mask.g, mask.b)));
	gl_FragColor = vec4(amount*smoothstep(0, 1, sampleStrength), 0, 0, 1);
}
