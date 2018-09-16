#version 330

in vec3 mVertexNormal;
in vec3 mVertexPosition;
in float cameraDistance;

out vec4 fragColor;

struct PointLight
{
    vec3 color;
    // light position in modelview coordinates.
    vec3 mPosition;
    float intensity;
};

struct Material
{
    vec4 diffuse;
    vec4 specular;
    float reflectance;
};

const int MAX_POINT_LIGHTS = 20;

uniform float specularPower;
uniform Material material;
uniform PointLight pointLights[MAX_POINT_LIGHTS];
uniform vec3 ambientLight;
// in model space
uniform vec3 cameraPosition;

vec4 materialColor;
vec4 diffuseC;
vec4 speculrC;

// P, N, eye and light.mPosition in model-space
vec3 calculateLighting(vec3 P, vec3 N, vec3 eye, PointLight light){
    vec3 result = vec3(0.0, 0.0, 0.0);

	vec3 lightDirection = normalize(light.mPosition.xyz - P); //vector towards light source
    // diffuse component
    float intensity = max(0.0, dot(N, lightDirection));
    result += intensity * light.color * material.diffuse.xyz;

	vec3 reflection = (reflect(lightDirection, N));
	vec3 virtualLightPosition = normalize(-reflection);

	// specular component
    float shine = pow( max(0.0, dot(virtualLightPosition, normalize(eye))), material.reflectance);
    //float shine = pow( max(0.0, dot(N, HalfAngle) ), mat.shininess );
    result += pow(shine, specularPower) * light.color;

	return result;
}

void main()
{
    vec3 diffuseSpecularComponent = vec3(0.0, 0.0, 0.0);
    for (int i=0; i < MAX_POINT_LIGHTS; i++) {
        if (pointLights[i].intensity > 0 ) {
            diffuseSpecularComponent += calculateLighting(mVertexPosition, mVertexNormal, cameraPosition, pointLights[i]);
        }
    }

    fragColor = material.diffuse * vec4(ambientLight, 1.0) + vec4(diffuseSpecularComponent, material.diffuse.w);
}