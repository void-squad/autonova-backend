package com.autonova.payments_billing_service.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @GetMapping("/_status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("pending-implementation");
    }
}
