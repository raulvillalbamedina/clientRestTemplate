package com.rvillalba.clientresttemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rvillalba.clientresttemplate.jwt.LoginRequest;
import com.rvillalba.clientresttemplate.jwt.LoginResponse;

import lombok.Data;

@Data
public class ClientRestTemplate {

    private RestTemplate restTemplate = restTemplate();
    private String baseurl = "http://localhost";
    private int port = 8081;

    public String generateRepositoryRestResourcePath(Class<?> entityClass) {
        String path;
        if (entityClass.getSimpleName().toLowerCase().endsWith("s")) {
            path = entityClass.getSimpleName().toLowerCase();
        } else if (entityClass.getSimpleName().toLowerCase().endsWith("y")) {
            path = entityClass.getSimpleName().toLowerCase().substring(0, entityClass.getSimpleName().length() - 1) + "ies";
        } else {
            path = entityClass.getSimpleName().toLowerCase() + "s";
        }
        return path;
    }

    private RestTemplate restTemplate() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate();
            List<HttpMessageConverter<?>> existingConverters = restTemplate.getMessageConverters();
            List<HttpMessageConverter<?>> newConverters = new ArrayList<>();
            newConverters.add(getHalMessageConverter());
            newConverters.addAll(existingConverters);
            restTemplate.setMessageConverters(newConverters);
        }
        return restTemplate;
    }

    private static HttpMessageConverter getHalMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jackson2HalModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MappingJackson2HttpMessageConverter halConverter = new TypeConstrainedMappingJackson2HttpMessageConverter(ResourceSupport.class);
        halConverter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));
        halConverter.setObjectMapper(objectMapper);
        return halConverter;
    }

    private HttpEntity<Object> createHttpEntity(LoginResponse loginResponse, Object entity) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Authorization", "Bearer " + loginResponse.getToken());
        HttpEntity result = null;
        if (entity != null) {
            result = new HttpEntity<Object>(entity, headers);
        } else {
            result = new HttpEntity<Object>(headers);
        }
        return result;
    }

    public ResponseEntity<PagedResources> readAll(Class<?> entityClass, LoginResponse loginResponse,
            ParameterizedTypeReference parameterizedTypeReference, Map<String, String> uriVariables) {
        if (uriVariables == null) {
            uriVariables = new HashMap<>();
        }
        return restTemplate().exchange(String.format(baseurl + ":%s/" + generateRepositoryRestResourcePath(entityClass), port), HttpMethod.GET,
                createHttpEntity(loginResponse, null), parameterizedTypeReference, uriVariables);
    }

    public ResponseEntity<Resource> readOne(LoginResponse loginResponse, Class<?> entityClass, Integer id,
            ParameterizedTypeReference parameterizedTypeReference) {
        return restTemplate().exchange(String.format(baseurl + ":%s/" + generateRepositoryRestResourcePath(entityClass) + "/%s", port, id),
                HttpMethod.GET, createHttpEntity(loginResponse, null), parameterizedTypeReference);
    }

    public LoginResponse login(LoginRequest user) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
        headers.add("Content-Type", "application/json");
        headers.add("X-Requested-With", "XMLHttpRequest");
        HttpEntity<LoginRequest> request = new HttpEntity<LoginRequest>(user, headers);
        LoginResponse response = restTemplate().postForObject(baseurl + ":" + port + "/api/auth/login", request, LoginResponse.class);
        return response;
    }

    public ResponseEntity<Resource> create(Class<?> entityClass, LoginResponse loginResponse, Object entity,
            ParameterizedTypeReference parameterizedTypeReference) {
        return restTemplate().exchange(String.format(baseurl + ":%s/" + generateRepositoryRestResourcePath(entityClass), port), HttpMethod.POST,
                createHttpEntity(loginResponse, entity), parameterizedTypeReference);
    }

    public ResponseEntity<Resource> update(LoginResponse loginResponse, Object entity, Class<?> entityClass, Integer id,
            ParameterizedTypeReference parameterizedTypeReference) {
        return restTemplate().exchange(String.format(baseurl + ":%s/" + generateRepositoryRestResourcePath(entityClass) + "/%s", port, id),
                HttpMethod.PUT, createHttpEntity(loginResponse, entity), parameterizedTypeReference);
    }

    public ResponseEntity<String> delete(LoginResponse loginResponse, Class<?> entityClass, Integer id) {
        return restTemplate().exchange(String.format(baseurl + ":%s/" + generateRepositoryRestResourcePath(entityClass) + "/%s", port, id),
                HttpMethod.DELETE, createHttpEntity(loginResponse, null), String.class);
    }
}
