package com.erangaherath.clickstream.controller;

import com.erangaherath.clickstream.model.ClickEvent;
import com.erangaherath.clickstream.producer.ClickEventProducer;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class ClickEventController {

    private final ClickEventProducer producer;

    public ClickEventController(ClickEventProducer producer) {
        this.producer = producer;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ClickEvent publishEvent(@RequestBody ClickEvent event) {
        producer.send(event);
        return event;
    }
}
