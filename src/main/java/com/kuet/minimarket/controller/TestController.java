package com.kuet.minimarket.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/admin/test")
    public ResponseEntity<String> adminTest() {
        return ResponseEntity.ok("Admin access OK");
    }

    @GetMapping("/seller/test")
    public ResponseEntity<String> sellerTest() {
        return ResponseEntity.ok("Seller access OK");
    }

    @GetMapping("/buyer/test")
    public ResponseEntity<String> buyerTest() {
        return ResponseEntity.ok("Buyer access OK");
    }
}
