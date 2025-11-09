package com.example.childhomeworktracker.service;
import com.example.childhomeworktracker.model.Parent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class ParentDataService {
    private final Map<String, Parent> parentsByPhone = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ResourceLoader resourceLoader;
    @Autowired
    public ParentDataService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    @PostConstruct
    public void init() throws IOException {
        var resource = resourceLoader.getResource("classpath:parents.json");
        if (!resource.exists()) return;
        var parents = mapper.readValue(resource.getInputStream(), new TypeReference<List<Parent>>() {});
        for (var p : parents) {
            parentsByPhone.put(p.getPhoneNumber(), p);
        }
    }
    public List<String> getChildRegistrationsForParent(String phone) {
        var parent = parentsByPhone.get(phone);
        return parent != null && parent.getChildren() != null
                ? parent.getChildren().stream().map(c -> c.getRegistrationNumber()).toList()
                : new ArrayList<>();
    }
}
