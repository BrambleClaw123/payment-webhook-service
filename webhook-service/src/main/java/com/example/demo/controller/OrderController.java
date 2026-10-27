package com.example.demo.controller;

import com.example.demo.entity.Order;
import com.example.demo.repository.OrderRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/orders")
public class OrderController {

    public record CreateOrderRequest(@NotNull @Positive Long amount) {}

    private final OrderRepository orders;

    public OrderController(OrderRepository orders) {
        this.orders = orders;
    }

    @PostMapping
    public Order create(@Valid @RequestBody CreateOrderRequest req) {
        return orders.save(new Order(req.amount()));
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable Long id) {
        return orders.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }
}
